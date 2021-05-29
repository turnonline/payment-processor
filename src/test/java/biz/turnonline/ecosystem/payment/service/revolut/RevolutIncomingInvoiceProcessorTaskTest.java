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

package biz.turnonline.ecosystem.payment.service.revolut;

import biz.turnonline.ecosystem.billing.model.IncomingInvoice;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.model.BeneficiaryBankAccount;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.FormOfPayment;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.payment.service.model.TransactionReceipt;
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
 * {@link RevolutPaymentDraftProcessorTask} unit testing.
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
    private RevolutPaymentDraftProcessorTask tested;

    @Injectable
    private Key<LocalAccount> accountKey;

    @Injectable
    private CommonTransaction transaction;

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

    @Mocked
    private Key<CommonTransaction> transactionKey;

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

        transaction = new TransactionReceipt( "any" );

        new Expectations( transaction )
        {
            {
                payloadRequest.finish();
                result = new CreatePaymentDraftResponse().id( UUID.fromString( PAYMENT_DRAFT_ID ) );
                minTimes = 0;

                transaction.entityKey();
                result = transactionKey;
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

        new Expectations( tested, transaction )
        {
            {
                tested.getDebtorBankAccount();
                result = debtorBank;

                config.getBeneficiary( invoice.getPayment().getBankAccount().getIban() );
                result = beneficiary;

                beneficiary.getExternalId( REVOLUT_BANK_CODE );
                result = BENEFICIARY_EXT_ID;

                tested.getTransactionDraft();
                result = transaction;

                transaction.save();
                transaction.credit( false );
                transaction.failure( false );
                transaction.amount( 34.8 );
                transaction.currency( DEBTOR_CURRENCY );
                transaction.type( FormOfPayment.TRANSFER );
                transaction.bankCode( REVOLUT_BANK_CODE );
                transaction.externalId( PAYMENT_DRAFT_ID );
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

                config.getBeneficiary( anyString );
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