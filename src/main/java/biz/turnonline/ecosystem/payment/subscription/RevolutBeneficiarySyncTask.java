package biz.turnonline.ecosystem.payment.subscription;

import biz.turnonline.ecosystem.billing.model.BankAccount;
import biz.turnonline.ecosystem.billing.model.Creditor;
import biz.turnonline.ecosystem.billing.model.IncomingInvoice;
import biz.turnonline.ecosystem.billing.model.InvoicePayment;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.model.BeneficiaryBankAccount;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.revolut.business.counterparty.model.Counterparty;
import biz.turnonline.ecosystem.revolut.business.counterparty.model.CreateCounterpartyRequest;
import biz.turnonline.ecosystem.revolut.business.counterparty.model.ProfileType;
import com.google.common.base.Strings;
import com.googlecode.objectify.Key;
import org.ctoolkit.restapi.client.RestFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import static biz.turnonline.ecosystem.payment.service.PaymentConfig.REVOLUT_BANK_CODE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Idempotent asynchronous task to sync beneficiary bank account with Revolut via Business API.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
class RevolutBeneficiarySyncTask
        extends JsonTask<IncomingInvoice>
{
    private static final long serialVersionUID = 4231567939684938821L;

    private static final Logger LOGGER = LoggerFactory.getLogger( RevolutBeneficiarySyncTask.class );

    private final Key<CompanyBankAccount> debtorBankAccountKey;

    @Inject
    transient private RestFacade facade;

    @Inject
    transient private PaymentConfig config;

    /**
     * Constructor.
     *
     * @param accountKey    the key of a local account as an owner of the payload
     * @param json          the incoming invoice as JSON payload
     * @param debtorBankKey the debtor bank account key, the bank account to be debited
     */
    RevolutBeneficiarySyncTask( @Nonnull Key<LocalAccount> accountKey,
                                @Nonnull String json,
                                @Nonnull Key<CompanyBankAccount> debtorBankKey )
    {
        super( accountKey, json, false );
        this.debtorBankAccountKey = checkNotNull( debtorBankKey, "Debtor bank account key can't be null" );
    }

    @Override
    protected void execute( @Nonnull LocalAccount owner, @Nonnull IncomingInvoice invoice )
    {
        Creditor creditor = invoice.getCreditor();
        if ( creditor == null || Strings.isNullOrEmpty( creditor.getBusinessName() ) )
        {
            LOGGER.warn( "Creditor's business name at incoming invoice '" + key( invoice ) + "' is missing" );
            return;
        }

        InvoicePayment payment = invoice.getPayment();
        if ( payment == null )
        {
            LOGGER.warn( "Incoming invoice identified by '" + key( invoice ) + "' is missing payment" );
            return;
        }

        BankAccount beneficiary = payment.getBankAccount();
        if ( beneficiary == null )
        {
            LOGGER.warn( "Incoming invoice identified by '" + key( invoice ) + "' is missing bank account" );
            return;
        }

        String iban = beneficiary.getIban();
        if ( Strings.isNullOrEmpty( iban ) )
        {
            LOGGER.warn( "IBAN is missing at bank account of incoming invoice '" + key( invoice ) + "'" );
            return;
        }

        String currency = beneficiary.getCurrency();
        if ( Strings.isNullOrEmpty( currency ) )
        {
            CompanyBankAccount debtorBankAccount = getDebtorBankAccount();
            if ( debtorBankAccount != null )
            {
                currency = debtorBankAccount.getCurrency();
                LOGGER.info( "Currency is missing at bank account of incoming invoice '"
                        + key( invoice )
                        + "' the debtor's default bank account currency has been set: "
                        + currency );
            }
        }

        if ( Strings.isNullOrEmpty( currency ) )
        {
            LOGGER.warn( "Currency is missing at bank account of incoming invoice '" + key( invoice ) + "'" );
            return;
        }

        BeneficiaryBankAccount bankAccount;
        try
        {
            bankAccount = config.insertBeneficiary( owner, iban, beneficiary.getBic(), currency );
        }
        catch ( IllegalArgumentException e )
        {
            LOGGER.warn( "Bak account validation at incoming invoice '"
                    + key( invoice )
                    + "' has failed, processing has been ignored", e );
            return;
        }

        String syncBank = REVOLUT_BANK_CODE;
        String externalId = bankAccount.getExternalId( syncBank );
        if ( !Strings.isNullOrEmpty( externalId ) )
        {
            LOGGER.warn( "Bank account "
                    + bankAccount.getId()
                    + " with IBAN: "
                    + iban
                    + " already synced to bank (code): "
                    + syncBank );
            return;
        }

        String beneficiaryContactEmail = null;
        if ( creditor.getContact() != null && !Strings.isNullOrEmpty( creditor.getContact().getEmail() ) )
        {
            // beneficiary contact email is optional
            beneficiaryContactEmail = creditor.getContact().getEmail();
        }

        CreateCounterpartyRequest request = new CreateCounterpartyRequest();
        // creditor must be always a business
        request.profileType( ProfileType.BUSINESS )
                .companyName( creditor.getBusinessName() )
                .email( beneficiaryContactEmail )
                .bankCountry( bankAccount.getCountry() )
                .currency( bankAccount.getCurrency() )
                .bic( bankAccount.getBic() )
                .iban( bankAccount.getIbanString() );

        Counterparty counterparty = facade.insert( request )
                .answerBy( Counterparty.class )
                .finish();

        bankAccount.setExternalId( syncBank, counterparty.getId().toString() );
        bankAccount.save();
    }

    private String key( @Nonnull IncomingInvoice invoice )
    {
        return invoice.getOrderId() + "/" + invoice.getId();
    }

    CompanyBankAccount getDebtorBankAccount()
    {
        return ofy().load().key( debtorBankAccountKey ).now();
    }

    @Override
    protected Class<IncomingInvoice> type()
    {
        return IncomingInvoice.class;
    }
}
