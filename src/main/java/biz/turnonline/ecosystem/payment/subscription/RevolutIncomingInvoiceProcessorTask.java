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

import biz.turnonline.ecosystem.billing.model.IncomingInvoice;
import biz.turnonline.ecosystem.billing.model.InvoicePayment;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.model.BeneficiaryBankAccount;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.FormOfPayment;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.payment.service.model.Transaction;
import biz.turnonline.ecosystem.revolut.business.draft.model.CreatePaymentDraftRequest;
import biz.turnonline.ecosystem.revolut.business.draft.model.CreatePaymentDraftResponse;
import biz.turnonline.ecosystem.revolut.business.draft.model.PaymentReceiver;
import biz.turnonline.ecosystem.revolut.business.draft.model.PaymentRequest;
import com.google.api.client.util.DateTime;
import com.google.common.base.Strings;
import com.googlecode.objectify.Key;
import org.ctoolkit.restapi.client.ClientErrorException;
import org.ctoolkit.restapi.client.RestFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static biz.turnonline.ecosystem.payment.service.PaymentConfig.REVOLUT_BANK_CODE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * The asynchronous task to process incoming invoice and setup payment (bank transfer) via Revolut Business API.
 * <p>
 * <strong>Preconditions:</strong>
 * <ul>
 *     <li>Debtor's bank account is ready to be debited {@link CompanyBankAccount#isDebtorReady()}</li>
 *     <li>{@link InvoicePayment} has all mandatory properties to make a payment</li>
 *     <li>Beneficiary bank account is already synced with Revolut, see {@link RevolutBeneficiarySyncTask}</li>
 *     <li>There is transaction record already prepared to be populated from invoice payments
 *     if payment sync is successful</li>
 * </ul>
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
class RevolutIncomingInvoiceProcessorTask
        extends JsonTask<IncomingInvoice>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( RevolutIncomingInvoiceProcessorTask.class );

    private static final long serialVersionUID = 7602323455177165927L;

    private final Key<CompanyBankAccount> debtorBankAccountKey;

    private final Key<Transaction> transactionKey;

    @Inject
    transient private RestFacade facade;

    @Inject
    transient private PaymentConfig config;

    /**
     * Constructor.
     *
     * @param accountKey    the key of a local account as an owner of the payload
     * @param json          the incoming invoice as JSON payload
     * @param delete        {@code true} to be incoming invoice processed as deleted
     * @param debtorBankKey the debtor bank account key, the bank account to be debited
     * @param t             the transaction draft to be populated if payment sync is successful
     */
    RevolutIncomingInvoiceProcessorTask( @Nonnull Key<LocalAccount> accountKey,
                                         @Nonnull String json,
                                         boolean delete,
                                         @Nonnull Key<CompanyBankAccount> debtorBankKey,
                                         @Nonnull Transaction t )
    {
        super( accountKey, json, delete );
        this.debtorBankAccountKey = checkNotNull( debtorBankKey, "Debtor bank account key can't be null" );
        this.transactionKey = checkNotNull( t.entityKey(), "Transaction draft key can't be null" );
    }

    @Override
    protected void execute( @Nonnull LocalAccount debtor, @Nonnull IncomingInvoice invoice )
    {
        InvoicePayment payment = invoice.getPayment();
        if ( payment == null )
        {
            LOGGER.warn( "Incoming invoice identified by '" + uniqueKey( invoice ) + "' missing payment." );
            return;
        }

        // debtor bank account details
        CompanyBankAccount debtorBank = getDebtorBankAccount();
        if ( debtorBank == null )
        {
            LOGGER.warn( "Debtor " + debtor + " has no bank account defined at all." );
            return;
        }

        // debtor bank will be the bank to make a payment
        if ( !debtorBank.isDebtorReady() )
        {
            LOGGER.warn( "Debtor's bank account " + debtorBank + " is not ready yet to be debited." );
            return;
        }

        // creditor bank account details (beneficiary)
        String creditorIban;
        if ( payment.getBankAccount() != null && !Strings.isNullOrEmpty( payment.getBankAccount().getIban() ) )
        {
            creditorIban = payment.getBankAccount().getIban();
        }
        else
        {
            LOGGER.warn( "Incoming invoice identified by '"
                    + uniqueKey( invoice )
                    + "' missing creditor's IBAN (payment.bankAccount.iban)." );
            return;
        }

        BeneficiaryBankAccount beneficiary = config.getBeneficiary( debtor, creditorIban );
        if ( beneficiary == null )
        {
            LOGGER.warn( "Incoming invoice identified by '"
                    + uniqueKey( invoice )
                    + ", Beneficiary for "
                    + creditorIban
                    + " and debtor "
                    + debtor + " not found." );
            return;
        }

        String debtorBankCode = debtorBank.getBankCode();
        String beneficiaryExtId = beneficiary.getExternalId( debtorBankCode );

        if ( beneficiaryExtId == null )
        {
            LOGGER.warn( "External beneficiary ID for bank " + debtorBankCode + " is missing " + beneficiary + "." );
            return;
        }

        Transaction transaction = getTransactionDraft();
        if ( transaction == null )
        {
            LOGGER.warn( "Transaction draft not found for " + transactionKey );
            return;
        }

        if ( isDelete() )
        {

        }
        else
        {
            schedulePaymentDraft( debtor, debtorBank, beneficiaryExtId, payment, transaction );
        }
    }

    private void schedulePaymentDraft( @Nonnull LocalAccount debtor,
                                       @Nonnull CompanyBankAccount debtorBank,
                                       @Nonnull String beneficiaryId,
                                       @Nonnull InvoicePayment payment,
                                       @Nonnull Transaction transaction )
    {
        String debtorExtId = debtorBank.getExternalId();
        String debtorCurrency = debtorBank.getCurrency();

        Double totalAmount = payment.getTotalAmount();
        LocalDate dueDate = scheduleByDueDate( debtor, payment.getDueDate() );
        String key = payment.getKey();
        Long vs = payment.getVariableSymbol();

        String title = key + ( vs == null ? "" : ", VS: " + vs );
        String reference = "Payment for: " + title;

        PaymentRequest payDraft = new PaymentRequest();
        payDraft.amount( totalAmount )
                .accountId( debtorExtId )
                .currency( debtorCurrency )
                .reference( reference );

        PaymentReceiver receiver = new PaymentReceiver();
        receiver.counterpartyId( UUID.fromString( beneficiaryId ) );
        payDraft.setReceiver( receiver );

        CreatePaymentDraftRequest request = new CreatePaymentDraftRequest()
                .title( payment.getKey() )
                .scheduleFor( dueDate )
                .addPaymentsItem( payDraft );

        try
        {
            CreatePaymentDraftResponse response = facade.insert( request )
                    .answerBy( CreatePaymentDraftResponse.class )
                    .finish();

            if ( response.getId() != null )
            {
                transaction.credit( true )
                        .amount( totalAmount )
                        .currency( debtorCurrency )
                        .key( key )
                        .type( FormOfPayment.TRANSFER )
                        .bankCode( REVOLUT_BANK_CODE )
                        .reference( reference )
                        .externalId( response.getId().toString() );

                transaction.save();
            }
            else
            {
                LOGGER.info( transaction.toString() );
                LOGGER.error( "Payment draft response does not have ID ?? " + response );
            }
        }
        catch ( ClientErrorException e )
        {
            LOGGER.error( "Payment request has failed for invoice: " + reference, e );
        }
    }

    /**
     * Returns due date reduced by two (default value) days.
     *
     * @param debtor as a source of ZoneId
     * @param date   the invoice due date as an input to plan payment
     * @return the date when to schedule a payment, or {@code null} for immediate payment
     */
    LocalDate scheduleByDueDate( @Nonnull LocalAccount debtor, @Nullable DateTime date )
    {
        if ( date == null )
        {
            return null;
        }

        ZoneId zoneId = debtor.getZoneId();
        LocalDate input = Instant.ofEpochMilli( date.getValue() )
                .atZone( zoneId )
                .toLocalDate();

        LocalDate dueDate;
        LocalDate now = LocalDate.now( zoneId );

        if ( now.isBefore( input ) )
        {
            LocalDate paymentDate = input.minusDays( 2 );
            if ( now.isBefore( paymentDate ) )
            {
                dueDate = paymentDate;
            }
            else
            {
                dueDate = null;
            }
        }
        else
        {
            dueDate = null;
        }

        return dueDate;
    }

    private String uniqueKey( @Nonnull IncomingInvoice invoice )
    {
        return invoice.getOrderId() + "/" + invoice.getId();
    }

    CompanyBankAccount getDebtorBankAccount()
    {
        return ofy().load().key( debtorBankAccountKey ).now();
    }

    Transaction getTransactionDraft()
    {
        return ofy().load().key( transactionKey ).now();
    }

    @Override
    protected Class<IncomingInvoice> type()
    {
        return IncomingInvoice.class;
    }
}
