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

package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.billing.model.IncomingInvoice;
import biz.turnonline.ecosystem.payment.api.ApiValidationException;
import biz.turnonline.ecosystem.payment.api.model.Certificate;
import biz.turnonline.ecosystem.payment.oauth.RevolutCredentialAdministration;
import biz.turnonline.ecosystem.payment.service.model.BeneficiaryBankAccount;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.FormOfPayment;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.payment.service.model.PaymentGate;
import biz.turnonline.ecosystem.payment.service.model.TransactionInvoice;
import biz.turnonline.ecosystem.payment.service.revolut.RevolutDebtorBankAccountsInit;
import biz.turnonline.ecosystem.steward.model.Account;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.inject.Injector;
import mockit.Mock;
import mockit.MockUp;
import nl.garvelink.iban.IBAN;
import org.ctoolkit.agent.service.impl.ImportTask;
import org.ctoolkit.restapi.client.adaptee.GetExecutorAdaptee;
import org.ctoolkit.restapi.client.pubsub.PubsubCommand;
import org.ctoolkit.services.task.Task;
import org.ctoolkit.services.task.TaskExecutor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static biz.turnonline.ecosystem.payment.service.PaymentConfig.REVOLUT_BANK_CODE;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_EMAIL;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_IDENTITY_ID;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_UNIQUE_ID;

/**
 * {@link PaymentConfigBean} unit testing incl. tests against emulated (local) App Engine datastore.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class PaymentConfigBeanDbTest
        extends BackendServiceTestCase
{
    private static final Long ACCOUNT_ID = 579L;

    private static final String REVOLUT_IBAN = "GB67REVO38133712681951";

    private static final String REVOLUT_BIC = "REVOGB21";

    private static final String REVOLUT_IBAN_SET = "GB05REVO37687428278420";

    private static final String CLIENT_ID = "client_Y6zhUcyAa..";

    private static final String BANK_ACCOUNT_EXT_ID = "9967e306-af32-4663-923b-09b5dff13c3c";

    @Inject
    private PaymentConfig bean;

    @Inject
    private RevolutCredentialAdministration revolut;

    @Inject
    private LocalAccountProvider lap;

    @Inject
    private Injector injector;

    private Account account;

    private LocalAccount lAccount;

    private IncomingInvoice invoice;

    @BeforeMethod
    public void before()
    {
        invoice = genericJsonFromFile( "incoming-invoice.pubsub.json", IncomingInvoice.class );

        account = genericJsonFromFile( "account.json", Account.class );
        lAccount = new LocalAccount( account );

        Account another = genericJsonFromFile( "account.json", Account.class );
        another.setId( 998877L );
        another.setIdentityId( "111DN78L2233" );
        another.setEmail( "another.account@turnonline.biz" );

        // import test bank accounts
        ImportTask task = new ImportTask( "/testdataset/changeset_00001.xml" );
        task.run();

        // import bank codes
        task = new ImportTask( "/dataset/changeset_00001.xml" );
        task.run();
    }

    @Test
    public <T extends GetExecutorAdaptee<?>> void localAccount_Check()
    {
        // precondition check
        int count = ofy().load().type( LocalAccount.class ).count();
        assertWithMessage( "Local account not be configured yet" )
                .that( count == 0 )
                .isTrue();

        AtomicReference<Boolean> remoteCall = new AtomicReference<>();
        remoteCall.set( Boolean.FALSE );

        new MockUp<T>()
        {
            @Mock
            Object executeGet( @Nonnull Object request,
                               @Nullable Map<String, Object> parameters,
                               @Nullable Locale locale )
            {
                remoteCall.set( Boolean.TRUE );
                return account;
            }
        };

        Map<String, String> attributes = new HashMap<>();
        attributes.put( ACCOUNT_IDENTITY_ID, lAccount.getIdentityId() );

        LocalAccount checked = lap.check( new PubsubCommand( attributes, null, null, null ) );
        assertWithMessage( "Local account just created" )
                .that( checked )
                .isNotNull();

        count = ofy().load().type( LocalAccount.class ).count();
        assertWithMessage( "Local account already stored" )
                .that( count == 1 )
                .isTrue();

        assertWithMessage( "Remote Account retrieval" )
                .that( remoteCall.get() )
                .isTrue();

        // trying to get non default account
        attributes.put( ACCOUNT_UNIQUE_ID, String.valueOf( lAccount.getId() + 1 ) );
        attributes.put( ACCOUNT_EMAIL, "blink.account@turnonline.biz" );
        checked = lap.check( new PubsubCommand( attributes, null, null, null ) );
        assertWithMessage( "Local account with non default ID" )
                .that( checked )
                .isNull();
    }

    @Test
    public <T extends TaskExecutor, E extends GetExecutorAdaptee<?>> void enableApiAccess_RevolutTaskScheduled()
    {
        new MockUp<E>()
        {
            @Mock
            Object executeGet( @Nonnull Object request,
                               @Nullable Map<String, Object> parameters,
                               @Nullable Locale locale )
            {
                return account;
            }
        };

        Map<String, String> attributes = new HashMap<>();
        attributes.put( ACCOUNT_IDENTITY_ID, lAccount.getIdentityId() );

        LocalAccount checked = lap.check( new PubsubCommand( attributes, null, null, null ) );

        // precondition check
        assertWithMessage( "Local Account should not be configured yet" )
                .that( lap.get() )
                .isNull();

        AtomicReference<Task<?>> scheduledTask = new AtomicReference<>();

        // scheduled tasks mocked
        new MockUp<T>()
        {
            @Mock
            public TaskHandle schedule( Task<?> task )
            {
                scheduledTask.set( task );
                return null;
            }
        };

        String privateKeyName = "Customized_Revolut_private_key_name";

        Certificate certificate = new Certificate();
        certificate.setClientId( CLIENT_ID );
        certificate.setKeyName( privateKeyName );

        Certificate result = bean.enableApiAccess( checked, REVOLUT_BANK_CODE.toLowerCase(), certificate );
        ofy().clear();

        assertWithMessage( "Local Account should be already configured, thus" )
                .that( lap.get() )
                .isNotNull();

        assertWithMessage( "Updated certificate" )
                .that( result )
                .isNotNull();

        assertWithMessage( "Certificate client ID" )
                .that( result.getClientId() )
                .isEqualTo( CLIENT_ID );

        assertWithMessage( "Certificate private_key_name" )
                .that( result.getKeyName() )
                .isEqualTo( privateKeyName );

        assertWithMessage( "Access authorised on" )
                .that( result.getAuthorisedOn() )
                .isNull();

        assertWithMessage( "Access authorised to Revolut API" )
                .that( result.isAccessAuthorised() )
                .isFalse();

        assertWithMessage( "Task to initialize bank accounts" )
                .that( scheduledTask.get() )
                .isNotNull();

        assertWithMessage( "Task type" )
                .that( scheduledTask.get() )
                .isInstanceOf( RevolutDebtorBankAccountsInit.class );

        assertWithMessage( "Task's account entity key" )
                .that( scheduledTask.get().getEntityKey() )
                .isEqualTo( checked.entityKey() );
    }

    @Test
    public <T extends TaskExecutor> void enableApiAccess_RevolutAccessAuthorised()
    {
        lAccount.save();
        AtomicReference<Task<?>> scheduledTask = new AtomicReference<>();

        // scheduled tasks mocked
        new MockUp<T>()
        {
            @Mock
            public TaskHandle schedule( Task<?> task )
            {
                scheduledTask.set( task );
                return null;
            }
        };

        Certificate certificate = new Certificate();
        certificate.setClientId( CLIENT_ID );

        // mark manually access Granted to the bank account API
        revolut.get().accessGranted();

        // test call
        Certificate result = bean.enableApiAccess( lAccount, REVOLUT_BANK_CODE.toLowerCase(), certificate );

        assertWithMessage( "Updated certificate" )
                .that( result )
                .isNotNull();

        assertWithMessage( "Certificate client ID" )
                .that( result.getClientId() )
                .isEqualTo( CLIENT_ID );

        assertWithMessage( "Access authorised on" )
                .that( result.getAuthorisedOn() )
                .isNotNull();

        assertWithMessage( "Access authorised to Revolut API" )
                .that( result.isAccessAuthorised() )
                .isTrue();
    }

    @Test( expectedExceptions = BankCodeNotFound.class )
    public void enableApiAccess_BankCodeNotFound()
    {
        bean.enableApiAccess( lAccount, "blacode", new Certificate() );
    }

    @Test( expectedExceptions = ApiValidationException.class )
    public void enableApiAccess_UnsupportedBank()
    {
        bean.enableApiAccess( lAccount, "0900", new Certificate() );
    }

    @Test
    public void getBankAccount()
    {
        CompanyBankAccount bankAccount = bean.getBankAccount( 9999L );
        assertThat( bankAccount ).isNotNull();
        assertThat( bankAccount.getBankCode() ).isEqualTo( "0900" );
        assertThat( bankAccount.getIBAN().toPlainString() ).isEqualTo( "SK0509009774621357177405" );
    }

    @Test( expectedExceptions = BankAccountNotFound.class )
    public void getBankAccount_NotFound()
    {
        bean.getBankAccount( 8888L );
    }

    @Test
    public void insertBankAccount()
    {
        int originSize = 6;

        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( 0, 10, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( originSize );

        CompanyBankAccount bankAccount = injector.getInstance( CompanyBankAccount.class );
        bankAccount.setIban( "SK3702005771190028932408" );
        bankAccount.setPrimary( false );
        bankAccount.setPaymentGate( PaymentGate.EPLATBY_VUB );
        bankAccount.save();

        bankAccounts = bean.getBankAccounts( null, null, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( originSize + 1 );
    }

    @Test
    public void getBankAccounts_Paging()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( 0, 10, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 6 );

        bankAccounts = bean.getBankAccounts( 0, 10, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 6 );

        // paging test
        bankAccounts = bean.getBankAccounts( 0, 3, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 3 );

        bankAccounts = bean.getBankAccounts( 4, 3, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 2 );
    }

    @Test
    public void getBankAccounts_ByBankCode()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( "0900" );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 2 );
        assertThat( bankAccounts.get( 0 ).getBankCode() ).isEqualTo( "0900" );
        assertThat( bankAccounts.get( 1 ).getBankCode() ).isEqualTo( "0900" );

        bankAccounts = bean.getBankAccounts( "0900" );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 2 );
        assertThat( bankAccounts.get( 0 ).getBankCode() ).isEqualTo( "0900" );
        assertThat( bankAccounts.get( 1 ).getBankCode() ).isEqualTo( "0900" );

        // paging test
        bankAccounts = bean.getBankAccounts( "9952" );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 1 );
        assertThat( bankAccounts.get( 0 ).getBankCode() ).isEqualTo( "9952" );

        bankAccounts = bean.getBankAccounts( "0200" );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 1 );
        assertThat( bankAccounts.get( 0 ).getBankCode() ).isEqualTo( "0200" );
    }

    @Test
    public void getBankAccounts_CountryFilter()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( 0, 10, "SK", null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 4 );

        bankAccounts = bean.getBankAccounts( 0, 10, "cz", null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 2 );
    }

    @Test
    public void deleteBankAccount()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( 0, 10, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 6 );

        CompanyBankAccount bankAccount = bankAccounts.get( 1 );
        // test call
        CompanyBankAccount deleted = bean.deleteBankAccount( bankAccount.getId() );
        assertThat( deleted ).isNotNull();
        assertThat( deleted ).isEqualTo( bankAccount );
        assertThat( deleted ).isEquivalentAccordingToCompareTo( bankAccount );

        // after deletion number of records check
        bankAccounts = bean.getBankAccounts( null, null, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 5 );
    }

    @Test( expectedExceptions = ApiValidationException.class )
    public void deleteBankAccount_PrimaryCannotBeDeleted()
    {
        ImportTask task = new ImportTask( "/testdataset/changeset_local-account.xml" );
        task.run();

        CompanyBankAccount bankAccount = bean.getPrimaryBankAccount( null );
        bean.deleteBankAccount( bankAccount.getId() );
    }

    @Test( expectedExceptions = BankAccountNotFound.class )
    public void deleteBankAccount_NotFound()
    {
        bean.deleteBankAccount( 8888L );
    }

    @Test
    public void markBankAccountAsPrimary()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( 0, 10, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 6 );

        long numberOfPrimary = bankAccounts.stream().filter( new PaymentConfigBean.BankAccountPrimary() ).count();
        // in datastore 4 bank accounts are being marked as a primary account
        assertThat( numberOfPrimary ).isEqualTo( 4 );

        CompanyBankAccount bankAccount = bankAccounts.get( 1 );

        // test call
        CompanyBankAccount primary = bean.markBankAccountAsPrimary( bankAccount.getId() );
        assertThat( primary ).isNotNull();
        assertThat( primary ).isEqualTo( bankAccount );
        assertThat( primary ).isEquivalentAccordingToCompareTo( bankAccount );
        assertThat( primary.isPrimary() ).isTrue();

        bankAccounts = bean.getBankAccounts( null, null, null, null );
        assertThat( bankAccounts ).isNotNull();
        numberOfPrimary = bankAccounts.stream().filter( new PaymentConfigBean.BankAccountPrimary() ).count();
        assertWithMessage( "only single record is being marked as a primary bank account" )
                .that( numberOfPrimary )
                .isEqualTo( 1 );
    }

    @Test
    public void getBankAccount_ByExternalId()
    {
        // prepare company bank account to have an external Id
        CompanyBankAccount bankAccountWithExtId = null;
        for ( CompanyBankAccount ba : bean.getBankAccounts( "0900" ) )
        {
            bankAccountWithExtId = ba;
            ba.setExternalId( BANK_ACCOUNT_EXT_ID );
            ba.save();
            break;
        }
        // clear cache
        ofy().clear();

        // test call
        CompanyBankAccount bankAccount = bean.getBankAccount( BANK_ACCOUNT_EXT_ID );

        assertWithMessage( "Company bank account identified by external Id" )
                .that( bankAccount )
                .isNotNull();

        assertWithMessage( "Company bank account identified by external Id" )
                .that( bankAccount )
                .isEqualTo( bankAccountWithExtId );
    }

    @Test
    public void getBankAccount_ByExternalIdNotFound()
    {
        CompanyBankAccount bankAccount = bean.getBankAccount( BANK_ACCOUNT_EXT_ID );

        assertWithMessage( "Company bank account identified by external Id" )
                .that( bankAccount )
                .isNull();
    }

    @Test( expectedExceptions = BankAccountNotFound.class )
    public void markBankAccountAsPrimary_NotFound()
    {
        bean.markBankAccountAsPrimary( 8888L );
    }

    @Test
    public void beneficiaryInsert_ValidAndRecordExist()
    {
        // make sure record not exist yet
        assertWithMessage( "Beneficiary record found for " + REVOLUT_IBAN )
                .that( bean.isBeneficiary( REVOLUT_IBAN ) )
                .isFalse();

        String formatted = IBAN.valueOf( REVOLUT_IBAN ).toString();
        BeneficiaryBankAccount beneficiary = bean.insertBeneficiary( formatted, REVOLUT_BIC, "EUR" );

        assertWithMessage( "Beneficiary country" )
                .that( beneficiary.getCountry() )
                .isEqualTo( "GB" );

        assertWithMessage( "Beneficiary bank code" )
                .that( beneficiary.getBankCode() )
                .isEqualTo( "REVO" );

        assertWithMessage( "Beneficiary branch" )
                .that( beneficiary.getBranch() )
                .isEqualTo( "381337" );

        assertWithMessage( "Beneficiary IBAN" )
                .that( beneficiary.getIBAN().toPlainString() )
                .isEqualTo( REVOLUT_IBAN );

        assertWithMessage( "Beneficiary BIC" )
                .that( beneficiary.getBic() )
                .isEqualTo( REVOLUT_BIC );

        assertWithMessage( "Beneficiary currency" )
                .that( beneficiary.getCurrency() )
                .isEqualTo( "EUR" );

        assertWithMessage( "Beneficiary record found for " + REVOLUT_IBAN )
                .that( bean.isBeneficiary( REVOLUT_IBAN ) )
                .isTrue();
    }

    @Test
    public void beneficiaryInsert_SaveIgnoredReturnsExisting()
    {
        int numberOf = countBeneficiaries();
        BeneficiaryBankAccount beneficiary = bean.insertBeneficiary( REVOLUT_IBAN_SET, REVOLUT_BIC, "EUR" );

        assertWithMessage( "Beneficiary" )
                .that( beneficiary )
                .isNotNull();

        assertWithMessage( "Number of beneficiary records" )
                .that( countBeneficiaries() )
                .isEqualTo( numberOf );
    }

    @Test( expectedExceptions = IllegalArgumentException.class )
    public void beneficiaryInsert_InvalidBIC()
    {
        bean.insertBeneficiary( REVOLUT_IBAN, "ASPKAT2LXX", "EUR" );
    }


    @SuppressWarnings( "ConstantConditions" )
    @Test( expectedExceptions = IllegalArgumentException.class )
    public void beneficiaryInsert_InvalidIBAN()
    {
        String iban = "GB67REVO38133722681951";
        bean.insertBeneficiary( iban, null, "EUR" );
    }

    @Test
    public void beneficiaryIs_FoundInChangeset()
    {
        assertWithMessage( "Beneficiary record found for " + REVOLUT_IBAN_SET )
                .that( bean.isBeneficiary( REVOLUT_IBAN_SET ) )
                .isTrue();
    }

    @Test
    public void beneficiaryIs_FoundInChangeset_IBANFormatted()
    {
        String formattedIBAN = IBAN.valueOf( REVOLUT_IBAN_SET ).toString();
        assertWithMessage( "Beneficiary record found for " + formattedIBAN )
                .that( bean.isBeneficiary( formattedIBAN ) )
                .isTrue();
    }

    @Test( expectedExceptions = IllegalArgumentException.class )
    public void beneficiaryIs_InvalidIBAN()
    {
        bean.getBeneficiary( "GB05REV037687428278420" );
    }

    @Test
    public void beneficiaryGet_FoundInChangeset()
    {
        BeneficiaryBankAccount beneficiary = bean.getBeneficiary( REVOLUT_IBAN_SET );

        assertWithMessage( "Beneficiary country" )
                .that( beneficiary.getCountry() )
                .isEqualTo( "GB" );

        assertWithMessage( "Beneficiary bank code" )
                .that( beneficiary.getBankCode() )
                .isEqualTo( "REVO" );

        assertWithMessage( "Beneficiary branch" )
                .that( beneficiary.getBranch() )
                .isEqualTo( "009969" );

        assertWithMessage( "Beneficiary IBAN" )
                .that( beneficiary.getIBAN().toPlainString() )
                .isEqualTo( REVOLUT_IBAN_SET );

        assertWithMessage( "Beneficiary BIC" )
                .that( beneficiary.getBic() )
                .isEqualTo( REVOLUT_BIC );
    }

    @Test
    public void beneficiaryGet_FoundInChangeset_IBANFormatted()
    {
        String formattedIBAN = IBAN.valueOf( REVOLUT_IBAN_SET ).toString();
        assertWithMessage( "Beneficiary record found for " + formattedIBAN )
                .that( bean.getBeneficiary( formattedIBAN ) )
                .isNotNull();
    }

    @Test( expectedExceptions = IllegalArgumentException.class )
    public void beneficiaryGet_InvalidIBAN()
    {
        bean.getBeneficiary( "GB67REVO38133722681951" );
    }

    @Test
    public void initGetTransactionDraft_Idempotent()
    {
        CommonTransaction transaction = bean.initGetTransactionDraft( invoice );
        assertWithMessage( "Transaction draft for incoming invoice" )
                .that( transaction )
                .isNotNull();

        assertWithMessage( "Transaction draft key" )
                .that( transaction.entityKey() )
                .isNotNull();

        int count = ofy().load().type( CommonTransaction.class ).count();
        assertWithMessage( "Number of Transaction record in datastore" )
                .that( count )
                .isEqualTo( 1 );

        ofy().flush();
        // try to create a new record with the same incoming invoice
        transaction = bean.initGetTransactionDraft( invoice );
        assertWithMessage( "Transaction draft for incoming invoice" )
                .that( transaction )
                .isNotNull();

        count = ofy().load().type( CommonTransaction.class ).count();
        assertWithMessage( "Number of Transaction record in datastore" )
                .that( count )
                .isEqualTo( 1 );
    }

    @Test
    public void initGetTransaction_Idempotent()
    {
        String extId = "91b160cf-d524-43ee-a2ee-687b8b91a3fa";
        CommonTransaction transaction = bean.initGetTransaction( extId );

        assertWithMessage( "Transaction for external expense" )
                .that( transaction )
                .isNotNull();

        assertWithMessage( "Transaction for external expense key" )
                .that( transaction.entityKey() )
                .isNotNull();

        transaction.save();

        int count = ofy().load().type( CommonTransaction.class ).count();
        assertWithMessage( "Number of Transaction record in datastore" )
                .that( count )
                .isEqualTo( 1 );

        ofy().clear();

        // try to create a new record with the same external Id
        transaction = bean.initGetTransaction( extId );
        assertWithMessage( "Transaction for external expense" )
                .that( transaction )
                .isNotNull();

        count = ofy().load().type( CommonTransaction.class ).count();
        assertWithMessage( "Number of Transaction record in datastore" )
                .that( count )
                .isEqualTo( 1 );
    }

    @Test
    public void filterTransactions_All()
    {
        ImportTask task = new ImportTask( "/testdataset/changeset_transactions.xml" );
        task.run();

        List<CommonTransaction> transactions = bean.filterTransactions( new PaymentConfig.Filter() );

        assertWithMessage( "Number of all transactions" )
                .that( transactions )
                .hasSize( 8 );
    }

    @Test
    public void filterTransactions_Paging()
    {
        ImportTask task = new ImportTask( "/testdataset/changeset_transactions.xml" );
        task.run();

        PaymentConfig.Filter filter = new PaymentConfig.Filter().offset( 2 ).limit( 3 );
        List<CommonTransaction> transactions = bean.filterTransactions( filter );

        assertWithMessage( "Transaction list offset with size" )
                .that( transactions )
                .hasSize( 3 );
    }

    @Test
    public void filterTransactions_Ordering()
    {
        ImportTask task = new ImportTask( "/testdataset/changeset_transactions.xml" );
        task.run();

        PaymentConfig.Filter filter = new PaymentConfig.Filter();
        List<CommonTransaction> transactions = bean.filterTransactions( filter );

        assertWithMessage( "Number of invoice ordered" )
                .that( transactions )
                .hasSize( 8 );

        assertWithMessage( "Transaction list order %s", transactions.get( 0 ).getCreatedDate() )
                .that( transactions.get( 0 ).getCreatedDate() )
                .isEqualTo( localDateTime( 2020, 4, 12, 10, 0, 10 ) );

        assertWithMessage( "Transaction list order %s", transactions.get( 1 ).getCreatedDate() )
                .that( transactions.get( 1 ).getCreatedDate() )
                .isEqualTo( localDateTime( 2020, 3, 10, 6, 0, 10 ) );

        assertWithMessage( "Transaction list order %s", transactions.get( 2 ).getCreatedDate() )
                .that( transactions.get( 2 ).getCreatedDate() )
                .isEqualTo( localDateTime( 2020, 2, 20, 7, 30, 20 ) );

        assertWithMessage( "Transaction list order %s", transactions.get( 3 ).getCreatedDate() )
                .that( transactions.get( 3 ).getCreatedDate() )
                .isEqualTo( localDateTime( 2020, 2, 16, 7, 25, 10 ) );

        assertWithMessage( "Transaction list order %s", transactions.get( 4 ).getCreatedDate() )
                .that( transactions.get( 4 ).getCreatedDate() )
                .isEqualTo( localDateTime( 2020, 2, 12, 7, 25, 10 ) );

        assertWithMessage( "Transaction list order %s", transactions.get( 5 ).getCreatedDate() )
                .that( transactions.get( 5 ).getCreatedDate() )
                .isEqualTo( localDateTime( 2020, 2, 12, 7, 25, 10 ) );

        assertWithMessage( "Transaction list order %s", transactions.get( 6 ).getCreatedDate() )
                .that( transactions.get( 6 ).getCreatedDate() )
                .isEqualTo( localDateTime( 2020, 2, 10, 17, 30, 20 ) );

        assertWithMessage( "Transaction list order %s", transactions.get( 7 ).getCreatedDate() )
                .that( transactions.get( 7 ).getCreatedDate() )
                .isEqualTo( localDateTime( 2019, 12, 10, 6, 25, 10 ) );
    }

    @Test
    public void filterTransactions_ByCreatedDateRange()
    {
        ImportTask task = new ImportTask( "/testdataset/changeset_transactions.xml" );
        task.run();

        PaymentConfig.Filter filter = new PaymentConfig.Filter()
                .createdDateFrom( Date.from( LocalDate.of( 2020, 1, 1 ).atStartOfDay().atZone( ZoneId.systemDefault() ).toInstant() ) )
                .createdDateTo( Date.from( LocalDate.of( 2020, 2, 12 ).atTime( LocalTime.MAX ).atZone( ZoneId.systemDefault() ).toInstant() ) );
        List<CommonTransaction> transactions = bean.filterTransactions( filter );

        assertWithMessage( "Number of invoice by created date range" )
                .that( transactions )
                .hasSize( 3 );
    }

    @Test
    public void filterTransactions_ByInvoice()
    {
        ImportTask task = new ImportTask( "/testdataset/changeset_transactions.xml" );
        task.run();

        long orderId = 476807L;
        long invoiceId = 366806L;

        PaymentConfig.Filter filter = new PaymentConfig.Filter()
                .orderId( orderId )
                .invoiceId( invoiceId );

        List<CommonTransaction> transactions = bean.filterTransactions( filter );

        assertWithMessage( "Number of invoice transactions" )
                .that( transactions )
                .hasSize( 1 );

        // another verification
        TransactionInvoice t = ( TransactionInvoice ) transactions.get( 0 );
        assertThat( t.getOrderId() ).isEqualTo( orderId );
        assertThat( t.getInvoiceId() ).isEqualTo( invoiceId );
    }

    @Test
    public void filterTransactions_ByOrder()
    {
        ImportTask task = new ImportTask( "/testdataset/changeset_transactions.xml" );
        task.run();

        Long orderId = 476807L;
        PaymentConfig.Filter filter = new PaymentConfig.Filter().orderId( orderId );
        List<CommonTransaction> transactions = bean.filterTransactions( filter );

        int expectedSize = 3;
        assertWithMessage( "Number of order transactions" )
                .that( transactions )
                .hasSize( expectedSize );

        // another verification
        long counted = transactions
                .stream()
                .filter( t -> orderId.equals( ( ( TransactionInvoice ) t ).getOrderId() ) )
                .count();

        assertThat( counted ).isEqualTo( expectedSize );
    }

    @Test
    public void filterTransactions_ByOrderDebit()
    {
        ImportTask task = new ImportTask( "/testdataset/changeset_transactions.xml" );
        task.run();

        Long orderId = 476807L;
        PaymentConfig.Filter filter = new PaymentConfig.Filter()
                .orderId( orderId )
                .operation( "debit" );

        List<CommonTransaction> transactions = bean.filterTransactions( filter );

        int expectedSize = 2;
        assertWithMessage( "Number of order debit transactions" )
                .that( transactions )
                .hasSize( expectedSize );

        // another verification
        long counted = transactions
                .stream()
                .filter( t -> orderId.equals( ( ( TransactionInvoice ) t ).getOrderId() ) && !t.isCredit() )
                .count();

        assertThat( counted ).isEqualTo( expectedSize );
    }

    @Test
    public void filterTransactions_ByOrderForAnotherAccount()
    {
        ImportTask task = new ImportTask( "/testdataset/changeset_transactions.xml" );
        task.run();

        Long orderId = 476807L;
        Long secondAccountId = 689L;
        PaymentConfig.Filter filter = new PaymentConfig.Filter()
                .orderId( orderId )
                .accountId( secondAccountId );

        List<CommonTransaction> transactions = bean.filterTransactions( filter );

        int expectedSize = 1;
        assertWithMessage( "Number of order transactions for another account" )
                .that( transactions )
                .hasSize( expectedSize );

        // another verification
        long counted = transactions
                .stream()
                .filter( t -> orderId.equals( ( ( TransactionInvoice ) t ).getOrderId() )
                        && secondAccountId.equals( t.getBankAccountKey().getId() ) )
                .count();

        assertThat( counted ).isEqualTo( expectedSize );
    }

    @Test
    public void filterTransactions_ByPrimaryBankAccount()
    {
        ImportTask task = new ImportTask( "/testdataset/changeset_transactions.xml" );
        task.run();

        PaymentConfig.Filter filter = new PaymentConfig.Filter().accountId( ACCOUNT_ID );
        List<CommonTransaction> transactions = bean.filterTransactions( filter );

        int expectedSize = 5;
        assertWithMessage( "Number of transactions for primary bank account" )
                .that( transactions )
                .hasSize( expectedSize );

        // another verification
        long counted = transactions
                .stream()
                .filter( t -> ACCOUNT_ID.equals( t.getBankAccountKey().getId() ) )
                .count();

        assertThat( counted ).isEqualTo( expectedSize );
    }

    @Test
    public void filterTransactions_BySecondaryBankAccount()
    {
        ImportTask task = new ImportTask( "/testdataset/changeset_transactions.xml" );
        task.run();

        Long secondAccountId = 689L;
        PaymentConfig.Filter filter = new PaymentConfig.Filter().accountId( secondAccountId );
        List<CommonTransaction> transactions = bean.filterTransactions( filter );

        int expectedSize = 3;
        assertWithMessage( "Number of transactions for secondary bank account" )
                .that( transactions )
                .hasSize( expectedSize );

        // another verification
        long counted = transactions
                .stream()
                .filter( t -> secondAccountId.equals( t.getBankAccountKey().getId() ) )
                .count();

        assertThat( counted ).isEqualTo( expectedSize );
    }

    @Test
    public void filterTransactions_Credit()
    {
        ImportTask task = new ImportTask( "/testdataset/changeset_transactions.xml" );
        task.run();

        PaymentConfig.Filter filter = new PaymentConfig.Filter().operation( "creDIT" );
        List<CommonTransaction> transactions = bean.filterTransactions( filter );

        int expectedSize = 4;
        assertWithMessage( "Number of credit transactions" )
                .that( transactions )
                .hasSize( expectedSize );

        // another verification
        long counted = transactions.stream().filter( CommonTransaction::isCredit ).count();
        assertThat( counted ).isEqualTo( expectedSize );
    }

    @Test
    public void filterTransactions_Debit()
    {
        ImportTask task = new ImportTask( "/testdataset/changeset_transactions.xml" );
        task.run();

        PaymentConfig.Filter filter = new PaymentConfig.Filter().operation( "DEbit" );
        List<CommonTransaction> transactions = bean.filterTransactions( filter );

        int expectedSize = 4;
        assertWithMessage( "Number of debit transactions" )
                .that( transactions )
                .hasSize( expectedSize );

        // another verification
        long counted = transactions.stream().filter( t -> !t.isCredit() ).count();
        assertThat( counted ).isEqualTo( expectedSize );
    }

    @Test
    public void filterTransactions_ByPaymentType()
    {
        ImportTask task = new ImportTask( "/testdataset/changeset_transactions.xml" );
        task.run();

        // TRANSFER filter
        PaymentConfig.Filter filter = new PaymentConfig.Filter().type( "transfer" );
        List<CommonTransaction> transactions = bean.filterTransactions( filter );

        assertWithMessage( "Number of TRANSFER transactions" )
                .that( transactions )
                .hasSize( 4 );

        // TRANSFER account Id and debit filter
        filter = new PaymentConfig.Filter()
                .accountId( ACCOUNT_ID )
                .operation( "DEBIT" )
                .type( FormOfPayment.TRANSFER.name() );
        transactions = bean.filterTransactions( filter );

        assertWithMessage( "Number of debit TRANSFER transactions for single account" )
                .that( transactions )
                .hasSize( 2 );

        // REFUND account Id and credit filter
        filter = new PaymentConfig.Filter()
                .accountId( 689L )
                .operation( "CREDIT" )
                .type( FormOfPayment.REFUND.name() );
        transactions = bean.filterTransactions( filter );

        assertWithMessage( "Number of credit REFUND transactions for single account" )
                .that( transactions )
                .hasSize( 1 );
    }

    @Test
    public void filterTransactions_ByStatus()
    {
        ImportTask task = new ImportTask( "/testdataset/changeset_transactions.xml" );
        task.run();

        // COMPLETED filter
        PaymentConfig.Filter filter = new PaymentConfig.Filter().status( "COMPLETED" );
        List<CommonTransaction> transactions = bean.filterTransactions( filter );

        assertWithMessage( "Number of COMPLETED transactions" )
                .that( transactions )
                .hasSize( 7 );
    }

    @Test
    public void filterTransactions_AllFilterCriteria()
    {
        ImportTask task = new ImportTask( "/testdataset/changeset_transactions.xml" );
        task.run();

        long orderId = 476807L;
        long invoiceId = 43546568L;
        PaymentConfig.Filter filter = new PaymentConfig.Filter()
                .accountId( ACCOUNT_ID )
                .operation( "DEBIT" )
                .orderId( orderId )
                .invoiceId( invoiceId )
                .type( FormOfPayment.TRANSFER.name() )
                .status( CommonTransaction.State.COMPLETED.name() )
                .limit( 2 );

        List<CommonTransaction> transactions = bean.filterTransactions( filter );

        assertWithMessage( "Number of transactions with complex filter" )
                .that( transactions )
                .hasSize( 1 );

        // another verification
        TransactionInvoice t = ( TransactionInvoice ) transactions.get( 0 );
        assertThat( t.getOrderId() ).isEqualTo( orderId );
        assertThat( t.getInvoiceId() ).isEqualTo( invoiceId );
        assertThat( t.getBankAccountKey().getId() ).isEqualTo( ACCOUNT_ID );
        assertThat( t.isCredit() ).isFalse();
        assertThat( t.getType() ).isEqualTo( FormOfPayment.TRANSFER );
    }

    @Test( expectedExceptions = ApiValidationException.class )
    public void filterTransactions_InvalidOffset()
    {
        bean.filterTransactions( new PaymentConfig.Filter().offset( -1 ) );
    }

    @Test( expectedExceptions = ApiValidationException.class )
    public void filterTransactions_InvalidLimit()
    {
        bean.filterTransactions( new PaymentConfig.Filter().limit( -1 ) );
    }

    @Test( expectedExceptions = ApiValidationException.class )
    public void filterTransactions_InvalidOperation()
    {
        bean.filterTransactions( new PaymentConfig.Filter().operation( "INVALID_BOTH" ) );
    }

    @Test( expectedExceptions = ApiValidationException.class )
    public void filterTransactions_InvalidPaymentType()
    {
        bean.filterTransactions( new PaymentConfig.Filter().type( "INVALID_TRANSFER" ) );
    }

    @Test( expectedExceptions = ApiValidationException.class )
    public void filterTransactions_InvalidStatus()
    {
        bean.filterTransactions( new PaymentConfig.Filter().status( "INVALID_STATUS" ) );
    }

    private int countBeneficiaries()
    {
        return ofy().load().type( BeneficiaryBankAccount.class ).count();
    }

    private Date localDateTime( int year, int month, int day, int hour, int minute, int second )
    {
        return Date.from( LocalDateTime.of( year, month, day, hour, minute, second ).atZone( ZoneId.of( "UTC" ) ).toInstant() );
    }
}