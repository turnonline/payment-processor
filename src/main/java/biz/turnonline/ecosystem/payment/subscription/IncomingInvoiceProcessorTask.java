package biz.turnonline.ecosystem.payment.subscription;

import biz.turnonline.ecosystem.billing.model.IncomingInvoice;
import biz.turnonline.ecosystem.billing.model.InvoicePayment;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.model.BankAccount;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.revolut.business.draft.model.CreatePaymentDraftRequest;
import biz.turnonline.ecosystem.revolut.business.draft.model.CreatePaymentDraftResponse;
import biz.turnonline.ecosystem.revolut.business.draft.model.PaymentReceiver;
import biz.turnonline.ecosystem.revolut.business.draft.model.PaymentRequest;
import com.google.api.client.util.DateTime;
import com.google.common.base.Strings;
import com.googlecode.objectify.Key;
import ma.glasnost.orika.MappingContext;
import org.ctoolkit.restapi.client.RestFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.UUID;

/**
 * The asynchronous task to process incoming invoice and setups payment via configured bank (if bank API available).
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
class IncomingInvoiceProcessorTask
        extends JsonTask<IncomingInvoice>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( IncomingInvoiceProcessorTask.class );

    private static final long serialVersionUID = -7360422665441968044L;

    @Inject
    transient private RestFacade facade;

    @Inject
    transient private PaymentConfig config;

    IncomingInvoiceProcessorTask( @Nonnull Key<LocalAccount> accountKey, @Nonnull String json, boolean delete )
    {
        super( accountKey, json, delete );
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
        BankAccount debtorBank = config.getDebtorBankAccount( debtor, payment );
        if ( debtorBank == null )
        {
            LOGGER.warn( "Debtor " + debtor + " has no bank account defined at all." );
            return;
        }

        // debtor bank will be the bank to make a payment
        String debtorBankCode = debtorBank.getBankCode();
        if ( !debtorBank.isDebtorReadyFor() )
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
                    + "' missing creditor's bank account." );
            return;
        }

        BankAccount beneficiary = config.getBeneficiary( debtor, creditorIban );
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

        String beneficiaryExtId = beneficiary.getExternalId( debtorBankCode );
        if ( beneficiaryExtId == null )
        {
            LOGGER.warn( "External beneficiary ID for bank " + debtorBankCode + " is missing " + beneficiary + "." );
            return;
        }

        if ( isDelete() )
        {

        }
        else
        {
            if ( debtorBank.isRevolut() )
            {
                schedulePaymentDraftByRevolut( debtor, debtorBank, beneficiaryExtId, payment );
            }
            else
            {
                LOGGER.warn( "Currently only Revolut is being supported to make a payment via API." );
            }
        }
    }

    private void schedulePaymentDraftByRevolut( @Nonnull LocalAccount debtor,
                                                @Nonnull BankAccount debtorBank,
                                                @Nonnull String beneficiaryId,
                                                @Nonnull InvoicePayment payment )
    {
        String debtorExtId = debtorBank.getExternalId();
        String debtorCurrency = debtorBank.getCurrency();

        Double totalAmount = payment.getTotalAmount();
        LocalDate dueDate = scheduleByDueDate( debtor, payment.getDueDate() );
        String key = payment.getKey();
        String method = payment.getMethod();
        Long vs = payment.getVariableSymbol();

        String title = key + ( vs == null ? "" : ", VS: " + vs );

        PaymentRequest payDraft = new PaymentRequest();
        payDraft.amount( totalAmount )
                .accountId( debtorExtId )
                .currency( debtorCurrency )
                .reference( "Payment for: " + title );

        PaymentReceiver receiver = new PaymentReceiver();
        receiver.counterpartyId( UUID.fromString( beneficiaryId ) );
        payDraft.setReceiver( receiver );

        CreatePaymentDraftRequest request = new CreatePaymentDraftRequest()
                .title( payment.getKey() )
                .scheduleFor( dueDate )
                .addPaymentsItem( payDraft );

        CreatePaymentDraftResponse response = facade.insert( request )
                .answerBy( CreatePaymentDraftResponse.class )
                .authBy( "TOKEN" )
                .bearer()
                .finish();

    }

    private LocalDate scheduleByDueDate( @Nonnull LocalAccount debtor, @Nullable DateTime date )
    {
        if ( date == null )
        {
            return null;
        }

        ZoneId zoneId = debtor.getZoneId();
        LocalDate now = LocalDate.now( zoneId );

        return Instant.ofEpochMilli( date.getValue() )
                .atZone( zoneId )
                .toLocalDate();
    }

    private String uniqueKey( @Nonnull IncomingInvoice invoice )
    {
        return invoice.getOrderId() + "/" + invoice.getId();
    }

    private MappingContext context( @Nonnull LocalAccount debtor )
    {
        MappingContext context = new MappingContext( new HashMap<>() );
        context.setProperty( LocalAccount.class, debtor );
        return context;
    }

    @Override
    protected Class<IncomingInvoice> type()
    {
        return IncomingInvoice.class;
    }
}
