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

package biz.turnonline.ecosystem.payment.subscription;

import biz.turnonline.ecosystem.billing.model.TransactionCounterparty;
import biz.turnonline.ecosystem.payment.api.model.Bill;
import biz.turnonline.ecosystem.payment.api.model.CounterpartyBankAccount;
import biz.turnonline.ecosystem.payment.api.model.ExchangeAmount;
import biz.turnonline.ecosystem.payment.api.model.ExchangeRate;
import biz.turnonline.ecosystem.payment.api.model.Merchant;
import biz.turnonline.ecosystem.payment.api.model.Transaction;
import biz.turnonline.ecosystem.payment.api.model.TransactionBank;
import biz.turnonline.ecosystem.payment.service.LocalAccountProvider;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import com.google.api.client.util.DateTime;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.googlecode.objectify.Key;
import ma.glasnost.orika.MapperFacade;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.services.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Dedicated task to push {@link biz.turnonline.ecosystem.billing.model.Transaction}
 * events to TurnOnline.biz Ecosystem Product Billing service.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
class TransactionPublisherTask
        extends Task<CommonTransaction>
{
    private static final long serialVersionUID = 3738143068915946947L;

    private static final Logger LOGGER = LoggerFactory.getLogger( TransactionPublisherTask.class );

    private final String extId;

    @Inject
    private transient RestFacade facade;

    @Inject
    private transient MapperFacade mapper;

    @Inject
    private transient LocalAccountProvider lap;

    @Inject
    private transient PaymentConfig config;

    TransactionPublisherTask( @Nonnull String extId )
    {
        super( "Push" );
        this.extId = checkNotNull( extId, "Transaction Ext ID can't be null" );
    }

    @Override
    public final void execute()
    {
        Stopwatch stopwatch = Stopwatch.createStarted();
        // At this point transaction must exist. If not found, this task will be rescheduled as exception is thrown.
        CommonTransaction transaction = workWith();
        if ( !transaction.propagate() )
        {
            Key<CommonTransaction> key = transaction.entityKey();
            LOGGER.info( "Transaction will not be propagated to product-billing service '" + key + "'" );
            LOGGER.info( "Execution took " + stopwatch.stop() );
            return;
        }

        // TECO-238 ignore publishing of the transaction if it's incomplete yet (race condition issue)
        if ( !transaction.isAmount() )
        {
            Key<CommonTransaction> key = transaction.entityKey();
            LOGGER.warn( "Transaction is considered incomplete as amount and currency is not set yet " + key );
            return;
        }

        LocalAccount lAccount = lap.get();
        if ( lAccount == null )
        {
            LOGGER.error( "Local account has not been configured yet." );
            LOGGER.info( "Execution took " + stopwatch.stop() );
            return;
        }

        Transaction api = mapper.map( transaction, Transaction.class );
        biz.turnonline.ecosystem.billing.model.Transaction pbt;
        pbt = new biz.turnonline.ecosystem.billing.model.Transaction();

        pbt.setAmount( api.getAmount() );
        pbt.setBalance( api.getBalance() );
        pbt.setCredit( api.isCredit() );
        pbt.setCurrency( api.getCurrency() );
        pbt.setReference( api.getReference() );
        pbt.setKey( api.getKey() );
        pbt.setStatus( api.getStatus() );
        pbt.setTransactionId( api.getTransactionId() );
        pbt.setType( api.getType() );
        pbt.setBillAmount( api.getBillAmount() );
        pbt.setBillCurrency( api.getBillCurrency() );

        Date completedAt = api.getCompletedAt();
        pbt.setCompletedAt( completedAt == null ? null : new DateTime( completedAt ) );

        TransactionBank bankAccount = api.getBankAccount();
        if ( bankAccount != null )
        {
            biz.turnonline.ecosystem.billing.model.TransactionBank bank;
            bank = new biz.turnonline.ecosystem.billing.model.TransactionBank();
            bank.setCode( bankAccount.getCode() );
            bank.setIban( bankAccount.getIban() );

            pbt.setBankAccount( bank );
        }

        Bill apiBill = api.getBill();
        if ( apiBill != null )
        {
            biz.turnonline.ecosystem.billing.model.Bill bill;
            bill = new biz.turnonline.ecosystem.billing.model.Bill();
            bill.setReceipt( apiBill.getReceipt() );
            bill.setOrder( apiBill.getOrder() );
            bill.setInvoice( apiBill.getInvoice() );

            pbt.setBill( bill );
        }

        Merchant merchant = api.getMerchant();

        // checking whether there is at least one non null property
        if ( merchant != null
                && ( !isNullOrEmpty( merchant.getCategory() )
                || !isNullOrEmpty( merchant.getCity() )
                || !isNullOrEmpty( merchant.getName() ) ) )
        {
            pbt.setMerchant( new biz.turnonline.ecosystem.billing.model.Merchant()
                    .setCategory( merchant.getCategory() )
                    .setCity( merchant.getCity() )
                    .setName( merchant.getName() ) );
        }

        CounterpartyBankAccount counterparty = api.getCounterparty();
        if ( counterparty != null && !Strings.isNullOrEmpty( counterparty.getIban() ) )
        {
            pbt.setCounterparty( new TransactionCounterparty() );
            pbt.getCounterparty().setIban( counterparty.getIban() );
            pbt.getCounterparty().setBic( counterparty.getBic() );
            pbt.getCounterparty().setName( counterparty.getName() );
        }

        ExchangeRate rate = api.getExchangeRate();
        if ( rate != null )
        {
            biz.turnonline.ecosystem.billing.model.ExchangeRate pbRate;
            pbRate = new biz.turnonline.ecosystem.billing.model.ExchangeRate();

            Date rateDate = rate.getRateDate();
            pbRate.setFrom( toPbAmount( rate.getFrom() ) )
                    .setTo( toPbAmount( rate.getTo() ) )
                    .setFee( toPbAmount( rate.getFee() ) )
                    .setRate( rate.getRate() )
                    .setRateDate( rateDate == null ? null : new DateTime( rateDate ) );

            pbt.setExchangeRate( pbRate );
        }

        // Transaction type taken from product-billing service to be pushed
        facade.insert( pbt )
                .onBehalfOf( lAccount )
                .finish();

        stopwatch.stop();
        LOGGER.info( Transaction.class.getSimpleName()
                + " has been pushed to product-billing service. "
                + getTaskName()
                + " final duration "
                + stopwatch );
    }

    @Override
    public CommonTransaction workWith()
    {
        // Throws TransactionNotFound if not found yet (eventual consistency of the query)
        return config.searchTransaction( extId );
    }

    private biz.turnonline.ecosystem.billing.model.ExchangeAmount toPbAmount( @Nullable ExchangeAmount amount )
    {
        if ( amount == null )
        {
            return null;
        }

        return new biz.turnonline.ecosystem.billing.model.ExchangeAmount()
                .setAmount( amount.getAmount() )
                .setCurrency( amount.getCurrency() );
    }
}
