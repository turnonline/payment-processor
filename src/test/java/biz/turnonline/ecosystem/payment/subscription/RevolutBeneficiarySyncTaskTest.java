package biz.turnonline.ecosystem.payment.subscription;

import biz.turnonline.ecosystem.billing.model.IncomingInvoice;
import biz.turnonline.ecosystem.payment.service.CodeBook;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.model.BeneficiaryBankAccount;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.revolut.business.counterparty.model.Counterparty;
import biz.turnonline.ecosystem.revolut.business.counterparty.model.CreateCounterpartyRequest;
import biz.turnonline.ecosystem.steward.model.Account;
import com.googlecode.objectify.Key;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.ctoolkit.restapi.client.AuthRequest;
import org.ctoolkit.restapi.client.RestFacade;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.UUID;

import static biz.turnonline.ecosystem.payment.service.BackendServiceTestCase.genericJsonFromFile;
import static biz.turnonline.ecosystem.payment.service.PaymentConfig.REVOLUT_BANK_CODE;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * {@link RevolutBeneficiarySyncTask} unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@SuppressWarnings( "ConstantConditions" )
public class RevolutBeneficiarySyncTaskTest
{
    private static final String COUNTERPARTY_ID = "239cdcaa-d2a6-4c08-aa32-2100e1976204";

    // value from the incoming-invoice.pubsub.json
    private static final String IBAN = "GB05REVO37687428278420";

    // value from the incoming-invoice.pubsub.json
    private static final String BIC = "REVOGB21";

    // value from the incoming-invoice.pubsub.json
    private static final String CURRENCY = "EUR";

    @Tested
    private RevolutBeneficiarySyncTask tested;

    @Injectable
    private Key<LocalAccount> accountKey;

    @Injectable
    private String json = "{}";

    @Injectable
    private Key<CompanyBankAccount> debtorBankAccountKey;

    @Injectable
    private RestFacade facade;

    @Injectable
    private PaymentConfig config;

    @Mocked
    private CodeBook codeBook;

    @Mocked
    private AuthRequest<?> authBy;

    private IncomingInvoice invoice;

    private LocalAccount account = new LocalAccount( new Account()
            .setId( 1735L )
            .setEmail( "my.account@turnonline.biz" )
            .setIdentityId( "64HGtr6ks" )
            .setAudience( "a1b" ) );

    @BeforeMethod
    public void before()
    {
        invoice = genericJsonFromFile( "incoming-invoice.pubsub.json", IncomingInvoice.class );

        new Expectations()
        {
            {
                authBy.finish();
                result = new Counterparty().id( UUID.fromString( COUNTERPARTY_ID ) );
                minTimes = 0;
            }
        };
    }

    @Test
    public void execute_CounterpartyCreated()
    {
        BeneficiaryBankAccount bankAccount = new BeneficiaryBankAccount( codeBook );
        bankAccount.setIban( invoice.getPayment().getBankAccount().getIban() );
        bankAccount.setBic( invoice.getPayment().getBankAccount().getBic() );
        bankAccount.setCurrency( invoice.getPayment().getBankAccount().getCurrency() );

        new Expectations( bankAccount )
        {
            {
                config.insertBeneficiary( account, IBAN, BIC, CURRENCY );
                result = bankAccount;

                // ExternalId needs to be saved
                bankAccount.save();
            }
        };

        tested.execute( account, invoice );

        new Verifications()
        {
            {
                CreateCounterpartyRequest request;
                facade.insert( request = withCapture() );

                assertWithMessage( "Counterparty create request" )
                        .that( request )
                        .isNotNull();

                assertWithMessage( "Counterparty request: business name" )
                        .that( request.getCompanyName() )
                        .isEqualTo( invoice.getCreditor().getBusinessName() );

                assertWithMessage( "Counterparty request: email" )
                        .that( request.getEmail() )
                        .isEqualTo( invoice.getCreditor().getContact().getEmail() );

                assertWithMessage( "Counterparty request: bank country" )
                        .that( request.getBankCountry() )
                        .isEqualTo( "GB" );

                assertWithMessage( "Counterparty request: currency" )
                        .that( request.getCurrency() )
                        .isEqualTo( CURRENCY );

                assertWithMessage( "Counterparty request: BIC" )
                        .that( request.getBic() )
                        .isEqualTo( BIC );

                assertWithMessage( "Counterparty request: IBAN" )
                        .that( nl.garvelink.iban.IBAN.valueOf( request.getIban() ).toPlainString() )
                        .isEqualTo( IBAN );
            }
        };

        assertWithMessage( "Beneficiary bank account external ID" )
                .that( bankAccount.getExternalId( REVOLUT_BANK_CODE ) )
                .isEqualTo( COUNTERPARTY_ID );

    }

    @Test
    public void execute_CounterpartyCreated_MissingOptionalEmail()
    {
        BeneficiaryBankAccount bankAccount = new BeneficiaryBankAccount( codeBook );
        bankAccount.setIban( invoice.getPayment().getBankAccount().getIban() );
        bankAccount.setBic( invoice.getPayment().getBankAccount().getBic() );
        bankAccount.setCurrency( invoice.getPayment().getBankAccount().getCurrency() );

        // make sure email is null
        invoice.getCreditor().setContact( null );

        new Expectations( bankAccount )
        {
            {
                config.insertBeneficiary( account, IBAN, BIC, CURRENCY );
                result = bankAccount;

                // ExternalId needs to be saved
                bankAccount.save();
            }
        };

        tested.execute( account, invoice );

        new Verifications()
        {
            {
                CreateCounterpartyRequest request;
                facade.insert( request = withCapture() );

                assertWithMessage( "Counterparty create request" )
                        .that( request )
                        .isNotNull();

                assertWithMessage( "Counterparty request: email" )
                        .that( request.getEmail() )
                        .isNull();

                assertWithMessage( "Counterparty request: IBAN" )
                        .that( request.getIban() )
                        .isNotNull();
            }
        };
    }

    @Test
    public void execute_CounterpartyCreated_DebtorDefaultCurrency()
    {
        final String debtorCurrency = "CZK";

        BeneficiaryBankAccount bankAccount = new BeneficiaryBankAccount( codeBook );
        bankAccount.setIban( invoice.getPayment().getBankAccount().getIban() );
        bankAccount.setBic( invoice.getPayment().getBankAccount().getBic() );
        bankAccount.setCurrency( debtorCurrency );

        // make sure currency is null
        invoice.getPayment().getBankAccount().setCurrency( null );

        // currency of the debtor default bank account
        CompanyBankAccount cba = new CompanyBankAccount( codeBook );
        cba.setCurrency( debtorCurrency );

        new Expectations( bankAccount, cba, tested )
        {
            {
                config.insertBeneficiary( account, IBAN, BIC, debtorCurrency );
                result = bankAccount;

                // ExternalId needs to be saved
                bankAccount.save();

                tested.getDebtorBankAccount();
                result = cba;
            }
        };

        tested.execute( account, invoice );

        new Verifications()
        {
            {
                CreateCounterpartyRequest request;
                facade.insert( request = withCapture() );

                assertWithMessage( "Counterparty create request" )
                        .that( request )
                        .isNotNull();

                assertWithMessage( "Counterparty request: currency" )
                        .that( request.getCurrency() )
                        .isNotNull();

                assertWithMessage( "Counterparty request: currency" )
                        .that( request.getCurrency() )
                        .isEqualTo( debtorCurrency );
            }
        };
    }

    @Test
    public void execute_CreditorMissing()
    {
        invoice.setCreditor( null );
        tested.execute( account, invoice );

        new Verifications()
        {
            {
                facade.insert( any );
                times = 0;
            }
        };
    }

    @Test
    public void execute_PaymentMissing()
    {
        invoice.setPayment( null );
        tested.execute( account, invoice );

        new Verifications()
        {
            {
                facade.insert( any );
                times = 0;
            }
        };
    }

    @Test
    public void execute_BeneficiaryBankAccountMissing()
    {
        invoice.getPayment().setBankAccount( null );
        tested.execute( account, invoice );

        new Verifications()
        {
            {
                facade.insert( any );
                times = 0;
            }
        };
    }

    @Test
    public void execute_IbanMissing()
    {
        invoice.getPayment().getBankAccount().setIban( null );
        tested.execute( account, invoice );

        new Verifications()
        {
            {
                facade.insert( any );
                times = 0;
            }
        };
    }

    @Test
    public void execute_CurrencyNull()
    {
        BeneficiaryBankAccount bankAccount = new BeneficiaryBankAccount( codeBook );
        bankAccount.setIban( invoice.getPayment().getBankAccount().getIban() );
        bankAccount.setBic( invoice.getPayment().getBankAccount().getBic() );

        // make sure currency is null
        invoice.getPayment().getBankAccount().setCurrency( null );
        new Expectations( tested )
        {
            {
                tested.getDebtorBankAccount();
                result = null;
            }
        };

        tested.execute( account, invoice );

        new Verifications()
        {
            {
                facade.insert( any );
                times = 0;
            }
        };
    }

    @Test
    public void execute_IbanValidationFailure()
    {
        new Expectations()
        {
            {
                config.insertBeneficiary( account, IBAN, BIC, CURRENCY );
                result = new IllegalArgumentException( "Invalid IBAN" );
            }
        };

        tested.execute( account, invoice );

        new Verifications()
        {
            {
                facade.insert( any );
                times = 0;
            }
        };
    }

    @Test
    public void execute_ExternalIdExist()
    {
        BeneficiaryBankAccount bankAccount = new BeneficiaryBankAccount( codeBook );
        bankAccount.setIban( invoice.getPayment().getBankAccount().getIban() );
        bankAccount.setBic( invoice.getPayment().getBankAccount().getBic() );
        bankAccount.setCurrency( invoice.getPayment().getBankAccount().getCurrency() );
        bankAccount.setExternalId( REVOLUT_BANK_CODE, COUNTERPARTY_ID );

        new Expectations( bankAccount )
        {
            {
                config.insertBeneficiary( account, IBAN, BIC, CURRENCY );
                result = bankAccount;
            }
        };

        tested.execute( account, invoice );

        new Verifications()
        {
            {
                facade.insert( any );
                times = 0;
            }
        };
    }
}