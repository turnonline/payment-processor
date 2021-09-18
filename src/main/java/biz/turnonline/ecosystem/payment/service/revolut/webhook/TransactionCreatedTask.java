/*
 * Copyright (c) 2020 TurnOnline.biz s.r.o. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package biz.turnonline.ecosystem.payment.service.revolut.webhook;

import biz.turnonline.ecosystem.payment.service.CategoryService;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.CounterpartyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.ExchangeAmount;
import biz.turnonline.ecosystem.payment.service.model.ExchangeRate;
import biz.turnonline.ecosystem.payment.service.model.FormOfPayment;
import biz.turnonline.ecosystem.payment.service.model.TransactionCategory;
import biz.turnonline.ecosystem.payment.service.model.TransactionReceipt;
import biz.turnonline.ecosystem.payment.subscription.JsonTask;
import biz.turnonline.ecosystem.revolut.business.counterparty.model.Counterparty;
import biz.turnonline.ecosystem.revolut.business.counterparty.model.CounterpartyAccount;
import biz.turnonline.ecosystem.revolut.business.transaction.model.Transaction;
import biz.turnonline.ecosystem.revolut.business.transaction.model.TransactionLeg;
import biz.turnonline.ecosystem.revolut.business.transaction.model.TransactionMerchant;
import biz.turnonline.ecosystem.revolut.business.transaction.model.TransactionState;
import biz.turnonline.ecosystem.revolut.business.transaction.model.TransactionType;
import com.google.common.base.Strings;
import org.ctoolkit.restapi.client.NotFoundException;
import org.ctoolkit.restapi.client.RestFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static biz.turnonline.ecosystem.payment.service.PaymentConfig.REVOLUT_BANK_EU_CODE;

/**
 * Async task to process Revolut {@link Transaction}.
 * <p>
 * Incoming transaction Id is being only used to get {@link Transaction} from the bank.
 * <p>
 * <strong>Note</strong>
 * </p>
 * In case declared transaction is not found in Revolut bank, next task will be cleared and nothing will be executed.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class TransactionCreatedTask
        extends JsonTask<Transaction>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( TransactionCreatedTask.class );

    private static final long serialVersionUID = 8438319760944126297L;

    private static final ReferenceResolver referenceResolver = new ReferenceResolver();

    transient private PaymentConfig config;

    transient private RestFacade facade;

    transient private CategoryService categoryService;

    /**
     * Constructor.
     *
     * @param json the event JSON payload of type {@link Transaction}
     */
    public TransactionCreatedTask( @Nonnull String json )
    {
        super( json, "Revolut-Transaction" );
    }

    @Override
    protected void execute( @Nonnull Transaction incoming )
    {
        String id = Strings.isNullOrEmpty( incoming.getId() ) ? "" : incoming.getId();
        Transaction transactionFromBank;

        try
        {
            transactionFromBank = facade.get( Transaction.class ).identifiedBy( id ).finish();
            LOGGER.info( "Incoming transaction (via webhook) found in bank system too" );
        }
        catch ( NotFoundException e )
        {
            clear();
            LOGGER.error( "Unknown incoming transaction identified by transaction Id: " + id, e );
            LOGGER.warn( "Next task cleared, nothing will be executed." );
            return;
        }

        List<TransactionLeg> legs = transactionFromBank.getLegs();
        if ( legs == null || legs.isEmpty() )
        {
            clear();
            LOGGER.warn( "Invalid incoming transaction, it has leg; ID " + id );
            LOGGER.warn( "Next task cleared, nothing will be executed." );
            return;
        }

        TransactionLeg leg = legs.get( 0 );

        String reference = referenceResolver.resolve( transactionFromBank.getReference() );
        CommonTransaction transaction = config.searchInitTransaction( id, reference );
        transaction.bankCode( REVOLUT_BANK_EU_CODE )
                .currency( leg.getCurrency() )
                .balance( leg.getBalance() )
                .reference( transactionFromBank.getReference() );

        if ( leg.getCounterparty() != null && leg.getCounterparty().getId() != null )
        {
            String counterpartyId = leg.getCounterparty().getId().toString();

            Counterparty counterparty = facade.get( Counterparty.class ).identifiedBy( counterpartyId ).finish();
            CounterpartyAccount counterpartyAccount = counterparty.getAccounts().get( 0 );

            CounterpartyBankAccount counterpartyBankAccount = new CounterpartyBankAccount();
            counterpartyBankAccount.setIban( counterpartyAccount.getIban() );
            counterpartyBankAccount.setBic( counterpartyAccount.getBic() );
            counterpartyBankAccount.setName( counterpartyAccount.getName() );

            transaction.setCounterparty( counterpartyBankAccount );
        }

        UUID accountId = leg.getAccountId();
        if ( accountId != null )
        {
            CompanyBankAccount bankAccount = config.getBankAccount( accountId.toString() );
            if ( bankAccount != null )
            {
                transaction.bankAccountKey( bankAccount.entityKey() );
                transaction.bankCode( bankAccount.getBankCode() );
            }
            else
            {
                LOGGER.warn( "Company bank account not found for external ID " + accountId );
            }
        }

        TransactionState state = transactionFromBank.getState();
        transaction.failure( TransactionState.CREATED != state
                && TransactionState.PENDING != state
                && TransactionState.COMPLETED != state );

        if ( state != null )
        {
            transaction.status( CommonTransaction.State.fromValue( transactionFromBank.getState().getValue() ) );
        }

        Double amount = leg.getAmount();
        if ( amount != null && amount > 0 )
        {
            transaction.amount( Math.abs( amount ) ).credit( true );
        }

        if ( amount != null && amount < 0 )
        {
            transaction.amount( Math.abs( amount ) ).credit( false );
        }

        if ( amount == null || amount == 0 )
        {
            transaction.amount( 0.0 ).credit( true );
        }

        Double billAmount = leg.getBillAmount();
        if ( billAmount != null )
        {
            transaction.billAmount( Math.abs( billAmount ) ).billCurrency( leg.getBillCurrency() );
        }

        if ( TransactionState.COMPLETED.equals( state ) )
        {
            transaction.completedAt( transactionFromBank.getCompletedAt() );
        }

        if ( TransactionType.CARD_PAYMENT.equals( transactionFromBank.getType() ) )
        {
            transaction.type( FormOfPayment.CARD_PAYMENT );
            populateMerchant( transaction, transactionFromBank );
        }
        else if ( TransactionType.TRANSFER.equals( transactionFromBank.getType() )
                || TransactionType.TOPUP.equals( transactionFromBank.getType() )
                || TransactionType.FEE.equals( transactionFromBank.getType() ) )
        {
            transaction.type( FormOfPayment.TRANSFER );
            populateMerchant( transaction, leg );
        }
        else if ( TransactionType.CARD_REFUND.equals( transactionFromBank.getType() )
                || TransactionType.REFUND.equals( transactionFromBank.getType() ) )
        {
            transaction.type( FormOfPayment.REFUND );
            populateMerchant( transaction, transactionFromBank );
        }

        List<TransactionCategory> categories = categoryService.resolveCategories( transaction );
        transaction.setCategories( categories );
        transaction.addOrigin( json() );

        if ( transaction.getBillAmount() != null
                && transaction.getAmount() != null
                && !Strings.isNullOrEmpty( transaction.getBillCurrency() )
                && !Strings.isNullOrEmpty( transaction.getCurrency() )
                && !Objects.equals( transaction.getBillCurrency(), transaction.getCurrency() ) )
        {
            double fromOriginal = transaction.getBillAmount();
            double toNew = transaction.getAmount();

            ExchangeRate rate = transaction.exchangeRate( new ExchangeRate() ).getExchangeRate();
            rate.from( new ExchangeAmount().amount( fromOriginal ).currency( transaction.getBillCurrency() ) );
            rate.to( new ExchangeAmount().amount( toNew ).currency( transaction.getCurrency() ) );
            rate.rateDate( new Date() );
            rate.fee( new ExchangeAmount().amount( 0.0 ).currency( transaction.getCurrency() ) );

            // the concrete exchange rate is being calculated
            double calcRate = BigDecimal.valueOf( fromOriginal )
                    // scale set to 9 as it's the same as Revolut /rate endpoint does
                    .setScale( 9, BigDecimal.ROUND_UP )
                    .divide( BigDecimal.valueOf( toNew ), BigDecimal.ROUND_UP )
                    .doubleValue();

            rate.rate( calcRate );
        }

        transaction.save();
        LOGGER.info( "Revolut Transaction [" + transaction.getId() + "] has been processed." );
    }

    private void populateMerchant( @Nonnull CommonTransaction transaction, @Nonnull Transaction fromBank )
    {
        TransactionMerchant merchant = fromBank.getMerchant();
        if ( merchant != null && transaction instanceof TransactionReceipt )
        {
            TransactionReceipt receipt = ( TransactionReceipt ) transaction;
            receipt.setCategory( merchant.getCategoryCode() );
            receipt.setCity( merchant.getCity() );
            receipt.setMerchantName( merchant.getName() );
        }
    }

    private void populateMerchant( @Nonnull CommonTransaction transaction, @Nonnull TransactionLeg leg )
    {
        String description = leg.getDescription();
        if ( description != null && transaction instanceof TransactionReceipt )
        {
            TransactionReceipt receipt = ( TransactionReceipt ) transaction;
            receipt.setMerchantName( description );
        }
    }

    @Override
    protected Class<Transaction> type()
    {
        return Transaction.class;
    }

    @Inject
    void setConfig( PaymentConfig config )
    {
        this.config = config;
    }

    @Inject
    void setFacade( RestFacade facade )
    {
        this.facade = facade;
    }

    @Inject
    void setCategoryService( CategoryService categoryService )
    {
        this.categoryService = categoryService;
    }
}
