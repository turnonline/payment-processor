package biz.turnonline.ecosystem.payment.subscription;

import biz.turnonline.ecosystem.billing.model.IncomingInvoice;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.model.BeneficiaryBankAccount;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.revolut.business.draft.model.CreatePaymentDraftRequest;
import biz.turnonline.ecosystem.revolut.business.draft.model.CreatePaymentDraftResponse;
import biz.turnonline.ecosystem.revolut.business.draft.model.PaymentReceiver;
import biz.turnonline.ecosystem.revolut.business.draft.model.PaymentRequest;
import biz.turnonline.ecosystem.steward.model.Account;
import com.google.api.client.util.DateTime;
import com.googlecode.objectify.Key;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.ctoolkit.restapi.client.PayloadRequest;
import org.ctoolkit.restapi.client.RestFacade;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static biz.turnonline.ecosystem.payment.service.BackendServiceTestCase.genericJsonFromFile;
import static biz.turnonline.ecosystem.payment.service.PaymentConfig.REVOLUT_BANK_CODE;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * {@link RevolutIncomingInvoiceProcessorTask} unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@SuppressWarnings( "ConstantConditions" )
public class RevolutIncomingInvoiceProcessorTaskTest
{
    private static final String BENEFICIARY_EXT_ID = "236995f4-48a0-11ea-b77f-2e728ce88125";

    private static final String PAYMENT_DRAFT_ID = "561cecf8-48a0-11ea-b77f-2e728ce88125";

    private static final String DEBTOR_IBAN = "GB56 REVO 0099 6958 1740 26";

    private static final String DEBTOR_EXT_ID = "36ec71ac-48a8-11ea-b77f-2e728ce88125";

    private static final String DEBTOR_CURRENCY = "EUR";

    /**
     * Feb 08 2020 05:00:00 GMT+0100
     */
    private static final long FEB_08_2020 = 1581134400000L;

    @Tested
    private RevolutIncomingInvoiceProcessorTask tested;

    @Injectable
    private Key<LocalAccount> accountKey;

    @Injectable
    private String json = "{}";

    @Injectable
    private boolean delete;

    @Injectable
    private Key<CompanyBankAccount> debtorBankKey;

    @Injectable
    private RestFacade facade;

    @Mocked
    private BeneficiaryBankAccount beneficiary;

    @Injectable
    private PaymentConfig config;

    @Mocked
    private PayloadRequest<?> payloadRequest;

    private IncomingInvoice invoice;

    private CompanyBankAccount debtorBank;

    private LocalAccount account = new LocalAccount( new Account()
            .setId( 56534352L )
            .setEmail( "my.account@turnonline.biz" )
            .setIdentityId( "HG78trTgh7z" )
            .setAudience( "a2b" ) );

    @BeforeMethod
    public void before()
    {
        invoice = genericJsonFromFile( "incoming-invoice.pubsub.json", IncomingInvoice.class );

        // debtor's bank account configured to be ready debited, #isDebtorReady() returns true
        debtorBank = new CompanyBankAccount( null );
        debtorBank.setIban( DEBTOR_IBAN );
        debtorBank.setCurrency( DEBTOR_CURRENCY );
        debtorBank.setExternalId( DEBTOR_EXT_ID );

        new Expectations()
        {
            {
                payloadRequest.finish();
                result = new CreatePaymentDraftResponse().id( UUID.fromString( PAYMENT_DRAFT_ID ) );
                minTimes = 0;
            }
        };
    }

    @Test
    public void scheduleByDueDate_DueDateNull()
    {
        assertWithMessage( "Scheduled due date" )
                .that( tested.scheduleByDueDate( account, null ) )
                .isNull();
    }

    @Test
    public void scheduleByDueDate_DueDateMinusDefaultDays()
    {
        expectationNow_FEB_01_2020();

        LocalDate dueDate = tested.scheduleByDueDate( account, new DateTime( FEB_08_2020 ) );
        assertWithMessage( "Due date FEB 08 2020 minus 2 (default) days" )
                .that( dueDate )
                .isEqualTo( LocalDate.of( 2020, 2, 6 ) );
    }

    @Test
    public void scheduleByDueDate_DueDateMinusDefaultDaysGoesPast()
    {
        expectationNow_FEB_01_2020();

        // Input: Feb 02 2020 07:00:00 GMT+0100
        LocalDate dueDate = tested.scheduleByDueDate( account, new DateTime( 1580623200000L ) );
        assertWithMessage( "Due date FEB 02 2020 minus 2 (default) days" )
                .that( dueDate )
                .isNull();
    }

    @Test
    public void scheduleByDueDate_PastDueDate()
    {
        expectationNow_FEB_01_2020();

        // Input: Jan 31 2020 20:00:00 GMT+0100
        LocalDate dueDate = tested.scheduleByDueDate( account, new DateTime( 1580497200000L ) );
        assertWithMessage( "Due date FEB 02 2020 minus 2 (default) days" )
                .that( dueDate )
                .isNull();
    }

    @Test
    public void execute_ProcessingScheduled()
    {
        expectationNow_FEB_01_2020();

        new Expectations( tested )
        {
            {
                tested.getDebtorBankAccount();
                result = debtorBank;

                config.getBeneficiary( account, invoice.getPayment().getBankAccount().getIban() );
                result = beneficiary;

                beneficiary.getExternalId( REVOLUT_BANK_CODE );
                result = BENEFICIARY_EXT_ID;
            }
        };

        tested.execute( account, invoice );

        new Verifications()
        {
            {
                CreatePaymentDraftRequest request;
                facade.insert( request = withCapture() );

                assertWithMessage( "Payment draft request" )
                        .that( request )
                        .isNotNull();

                assertWithMessage( "Payment scheduled based on invoice due date minus 2 days" )
                        .that( request.getScheduleFor() )
                        .isEqualTo( LocalDate.of( 2020, 2, 18 ) );

                assertWithMessage( "Payment title (invoice key)" )
                        .that( request.getTitle() )
                        .isEqualTo( "100022020" );

                assertWithMessage( "List of scheduled payments" )
                        .that( request.getPayments() )
                        .isNotEmpty();

                PaymentRequest payDraft = request.getPayments().get( 0 );
                assertWithMessage( "Debtor Ext account ID at payment request" )
                        .that( payDraft.getAccountId() )
                        .isEqualTo( DEBTOR_EXT_ID );

                assertWithMessage( "Payment amount at payment request" )
                        .that( payDraft.getAmount() )
                        .isEqualTo( 34.8 );

                assertWithMessage( "Debtor currency at payment request" )
                        .that( payDraft.getCurrency() )
                        .isEqualTo( DEBTOR_CURRENCY );

                assertWithMessage( "Payment reference" )
                        .that( payDraft.getReference() )
                        .isNotNull();

                PaymentReceiver receiver = payDraft.getReceiver();
                assertWithMessage( "Payment receiver" )
                        .that( receiver )
                        .isNotNull();

                assertWithMessage( "Payment receiver Ext ID" )
                        .that( receiver.getCounterpartyId() )
                        .isEqualTo( UUID.fromString( BENEFICIARY_EXT_ID ) );
            }
        };
    }

    @Test
    public void execute_PaymentMissing()
    {
        // make sure payment is not configured
        invoice.setPayment( null );

        // test call
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
    public void execute_DebtorBankAccountNotFound()
    {
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
    public void execute_DebtorBankAccountIsNotReady()
    {
        // make sure debtor's bank does not have an external ID yet -> not ready yet to be debited
        debtorBank.setExternalId( null );

        new Expectations( tested )
        {
            {
                tested.getDebtorBankAccount();
                result = debtorBank;
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
    public void execute_MissingCreditorIban()
    {
        // make sure creditor's IBAN is an empty at invoice payment
        invoice.getPayment().getBankAccount().setIban( "" );

        new Expectations( tested )
        {
            {
                tested.getDebtorBankAccount();
                result = debtorBank;
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
    public void execute_BeneficiaryBankAccountNotFound()
    {
        new Expectations( tested )
        {
            {
                tested.getDebtorBankAccount();
                result = debtorBank;

                config.getBeneficiary( account, anyString );
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
    public void execute_BeneficiaryExternalIdNotSet()
    {
        new Expectations( tested )
        {
            {
                tested.getDebtorBankAccount();
                result = debtorBank;

                beneficiary.getExternalId( REVOLUT_BANK_CODE );
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

    private void expectationNow_FEB_01_2020()
    {
        new MockUp<LocalDate>()
        {
            @Mock
            public LocalDate now( ZoneId zone )
            {
                return LocalDate.of( 2020, 2, 1 );
            }
        };
    }
}