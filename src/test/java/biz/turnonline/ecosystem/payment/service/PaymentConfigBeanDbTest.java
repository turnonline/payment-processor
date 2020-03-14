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
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.payment.service.model.PaymentGate;
import biz.turnonline.ecosystem.payment.service.revolut.RevolutDebtorBankAccountsInit;
import biz.turnonline.ecosystem.steward.model.Account;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.inject.Injector;
import mockit.Mock;
import mockit.MockUp;
import nl.garvelink.iban.IBAN;
import org.ctoolkit.agent.service.impl.ImportTask;
import org.ctoolkit.services.task.Task;
import org.ctoolkit.services.task.TaskExecutor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static biz.turnonline.ecosystem.payment.service.PaymentConfig.REVOLUT_BANK_CODE;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * {@link PaymentConfigBean} unit testing incl. tests against emulated (local) App Engine datastore.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class PaymentConfigBeanDbTest
        extends BackendServiceTestCase
{
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
    private Injector injector;

    private LocalAccount lAccount;

    private LocalAccount lAnother;

    private IncomingInvoice invoice;

    @BeforeMethod
    public void before()
    {
        invoice = genericJsonFromFile( "incoming-invoice.pubsub.json", IncomingInvoice.class );

        Account account = genericJsonFromFile( "account.json", Account.class );
        lAccount = new LocalAccount( account );
        lAccount.save();

        Account another = genericJsonFromFile( "account.json", Account.class );
        another.setId( 998877L );
        another.setIdentityId( "111DN78L2233" );
        another.setEmail( "another.account@turnonline.biz" );
        lAnother = new LocalAccount( another );
        lAnother.save();

        // import test bank accounts
        ImportTask task = new ImportTask( "/testdataset/changeset_00001.xml" );
        task.run();

        // import bank codes
        task = new ImportTask( "/dataset/changeset_00001.xml" );
        task.run();
    }

    @Test
    public <T extends TaskExecutor> void enableApiAccess_RevolutTaskScheduled()
    {
        // precondition check
        assertWithMessage( "Local Account should not be configured yet, thus" )
                .that( bean.getLocalAccount() )
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

        Certificate result = bean.enableApiAccess( lAccount, REVOLUT_BANK_CODE.toLowerCase(), certificate );
        ofy().clear();

        assertWithMessage( "Local Account should be already configured, thus" )
                .that( bean.getLocalAccount() )
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
                .isEqualTo( lAccount.entityKey() );
    }

    @Test
    public <T extends TaskExecutor> void enableApiAccess_RevolutAccessAuthorised()
    {
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
        CompanyBankAccount bankAccount = bean.getBankAccount( lAnother, 9999L );
        assertThat( bankAccount ).isNotNull();
        assertThat( bankAccount.getBankCode() ).isEqualTo( "0900" );
        assertThat( bankAccount.getIBAN().toPlainString() ).isEqualTo( "SK0509009774621357177405" );
    }

    @Test( expectedExceptions = WrongEntityOwner.class )
    public void getBankAccount_WrongOwner()
    {
        bean.getBankAccount( lAccount, 9999L );
    }

    @Test( expectedExceptions = BankAccountNotFound.class )
    public void getBankAccount_NotFound()
    {
        bean.getBankAccount( lAccount, 8888L );
    }

    @Test
    public void insertBankAccount()
    {
        int originSize = 5;

        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( lAccount, 0, 10, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( originSize );

        CompanyBankAccount bankAccount = injector.getInstance( CompanyBankAccount.class );
        bankAccount.setIban( "SK3702005771190028932408" );
        bankAccount.setPrimary( false );
        bankAccount.setPaymentGate( PaymentGate.EPLATBY_VUB );

        bean.insert( lAccount, bankAccount );

        bankAccounts = bean.getBankAccounts( lAccount, null, null, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 6 );
    }

    @Test
    public void getBankAccounts_Paging()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( lAccount, 0, 10, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 5 );

        bankAccounts = bean.getBankAccounts( lAnother, 0, 10, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 1 );

        assertThat( bankAccounts.get( 0 ).getOwner() ).isEqualTo( lAnother );

        // paging test
        bankAccounts = bean.getBankAccounts( lAccount, 0, 3, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 3 );

        bankAccounts = bean.getBankAccounts( lAccount, 3, 3, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 2 );
    }

    @Test
    public void getBankAccounts_ByBankCode()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( lAccount, "0900" );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 1 );
        assertThat( bankAccounts.get( 0 ).getOwner() ).isEqualTo( lAccount );
        assertThat( bankAccounts.get( 0 ).getBankCode() ).isEqualTo( "0900" );

        bankAccounts = bean.getBankAccounts( lAnother, "0900" );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 1 );
        assertThat( bankAccounts.get( 0 ).getOwner() ).isEqualTo( lAnother );
        assertThat( bankAccounts.get( 0 ).getBankCode() ).isEqualTo( "0900" );

        // paging test
        bankAccounts = bean.getBankAccounts( lAccount, "9952" );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 1 );
        assertThat( bankAccounts.get( 0 ).getBankCode() ).isEqualTo( "9952" );

        bankAccounts = bean.getBankAccounts( lAccount, "0200" );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 1 );
        assertThat( bankAccounts.get( 0 ).getBankCode() ).isEqualTo( "0200" );
    }

    @Test
    public void getBankAccounts_CountryFilter()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( lAccount, 0, 10, "SK", null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 3 );

        bankAccounts = bean.getBankAccounts( lAccount, 0, 10, "cz", null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 2 );
    }

    @Test
    public void updateBankAccount_OwnerOk()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( lAccount, 0, 10, null, null );
        assertThat( bankAccounts ).isNotEmpty();

        CompanyBankAccount bankAccount = bankAccounts.get( 0 );
        bankAccount.setPaymentGate( PaymentGate.TRANSFER );

        bean.update( lAccount, bankAccount );
    }

    @Test( expectedExceptions = WrongEntityOwner.class )
    public void updateBankAccount_WrongOwner()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( lAccount, null, null, null, null );
        assertThat( bankAccounts ).isNotEmpty();

        CompanyBankAccount bankAccount = bankAccounts.get( 0 );
        bankAccount.setPaymentGate( PaymentGate.EPLATBY_VUB );

        bean.update( lAnother, bankAccount );
    }

    @Test
    public void deleteBankAccount()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( lAccount, 0, 10, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 5 );

        CompanyBankAccount bankAccount = bankAccounts.get( 1 );
        // test call
        CompanyBankAccount deleted = bean.deleteBankAccount( lAccount, bankAccount.getId() );
        assertThat( deleted ).isNotNull();
        assertThat( deleted ).isEqualTo( bankAccount );
        assertThat( deleted ).isEquivalentAccordingToCompareTo( bankAccount );

        // after deletion number of records check
        bankAccounts = bean.getBankAccounts( lAccount, null, null, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 4 );
    }

    @Test( expectedExceptions = WrongEntityOwner.class )
    public void deleteBankAccount_WrongOwner()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( lAccount, 0, 10, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 5 );

        CompanyBankAccount bankAccount = bankAccounts.get( 0 );
        bean.deleteBankAccount( lAnother, bankAccount.getId() );
    }

    @Test( expectedExceptions = ApiValidationException.class )
    public void deleteBankAccount_PrimaryCannotBeDeleted()
    {
        CompanyBankAccount bankAccount = bean.getPrimaryBankAccount( lAccount, null );
        bean.deleteBankAccount( lAccount, bankAccount.getId() );
    }

    @Test( expectedExceptions = BankAccountNotFound.class )
    public void deleteBankAccount_NotFound()
    {
        bean.deleteBankAccount( lAccount, 8888L );
    }

    @Test
    public void markBankAccountAsPrimary()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( lAccount, 0, 10, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 5 );

        long numberOfPrimary = bankAccounts.stream().filter( new PaymentConfigBean.BankAccountPrimary() ).count();
        // in datastore 2 bank accounts are being marked as a primary account
        assertThat( numberOfPrimary ).isEqualTo( 3 );

        CompanyBankAccount bankAccount = bankAccounts.get( 1 );

        // test call
        CompanyBankAccount primary = bean.markBankAccountAsPrimary( lAccount, bankAccount.getId() );
        assertThat( primary ).isNotNull();
        assertThat( primary ).isEqualTo( bankAccount );
        assertThat( primary ).isEquivalentAccordingToCompareTo( bankAccount );
        assertThat( primary.isPrimary() ).isTrue();

        bankAccounts = bean.getBankAccounts( lAccount, null, null, null, null );
        assertThat( bankAccounts ).isNotNull();
        numberOfPrimary = bankAccounts.stream().filter( new PaymentConfigBean.BankAccountPrimary() ).count();
        assertWithMessage( "only single record is being marked as a primary bank account" )
                .that( numberOfPrimary )
                .isEqualTo( 1 );
    }

    @Test( expectedExceptions = WrongEntityOwner.class )
    public void markBankAccountAsPrimary_WrongOwner()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( lAccount, 0, 10, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 5 );

        bean.markBankAccountAsPrimary( lAnother, bankAccounts.get( 0 ).getId() );
    }

    @Test
    public void getBankAccount_ByExternalId()
    {
        // prepare company bank account to have an external Id
        CompanyBankAccount bankAccountWithExtId = null;
        for ( CompanyBankAccount ba : bean.getBankAccounts( lAccount, "0900" ) )
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
        bean.markBankAccountAsPrimary( lAccount, 8888L );
    }

    @Test
    public void beneficiaryInsert_ValidAndRecordExist()
    {
        // make sure record not exist yet
        assertWithMessage( "Beneficiary record found for " + REVOLUT_IBAN )
                .that( bean.isBeneficiary( lAccount, REVOLUT_IBAN ) )
                .isFalse();

        String formatted = IBAN.valueOf( REVOLUT_IBAN ).toString();
        BeneficiaryBankAccount beneficiary = bean.insertBeneficiary( lAccount, formatted, REVOLUT_BIC, "EUR" );

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
                .that( bean.isBeneficiary( lAccount, REVOLUT_IBAN ) )
                .isTrue();
    }

    @Test
    public void beneficiaryInsert_SaveIgnoredReturnsExisting()
    {
        int numberOf = countBeneficiaries( lAccount );
        BeneficiaryBankAccount beneficiary = bean.insertBeneficiary( lAccount, REVOLUT_IBAN_SET, REVOLUT_BIC, "EUR" );

        assertWithMessage( "Beneficiary" )
                .that( beneficiary )
                .isNotNull();

        assertWithMessage( "Number of beneficiary records" )
                .that( countBeneficiaries( lAccount ) )
                .isEqualTo( numberOf );
    }

    @Test( expectedExceptions = IllegalArgumentException.class )
    public void beneficiaryInsert_InvalidBIC()
    {
        bean.insertBeneficiary( lAccount, REVOLUT_IBAN, "ASPKAT2LXX", "EUR" );
    }


    @SuppressWarnings( "ConstantConditions" )
    @Test( expectedExceptions = IllegalArgumentException.class )
    public void beneficiaryInsert_InvalidIBAN()
    {
        String iban = "GB67REVO38133722681951";
        bean.insertBeneficiary( lAccount, iban, null, "EUR" );
    }

    @Test
    public void beneficiaryIs_FoundInChangeset()
    {
        assertWithMessage( "Beneficiary record found for " + REVOLUT_IBAN_SET )
                .that( bean.isBeneficiary( lAccount, REVOLUT_IBAN_SET ) )
                .isTrue();
    }

    @Test
    public void beneficiaryIs_FoundInChangeset_IBANFormatted()
    {
        String formattedIBAN = IBAN.valueOf( REVOLUT_IBAN_SET ).toString();
        assertWithMessage( "Beneficiary record found for " + formattedIBAN )
                .that( bean.isBeneficiary( lAccount, formattedIBAN ) )
                .isTrue();
    }

    @Test( expectedExceptions = IllegalArgumentException.class )
    public void beneficiaryIs_InvalidIBAN()
    {
        bean.getBeneficiary( lAccount, "GB05REV037687428278420" );
    }

    @Test
    public void beneficiaryGet_FoundInChangeset()
    {
        BeneficiaryBankAccount beneficiary = bean.getBeneficiary( lAccount, REVOLUT_IBAN_SET );

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
                .that( bean.getBeneficiary( lAccount, formattedIBAN ) )
                .isNotNull();
    }

    @Test( expectedExceptions = IllegalArgumentException.class )
    public void beneficiaryGet_InvalidIBAN()
    {
        bean.getBeneficiary( lAccount, "GB67REVO38133722681951" );
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

    private int countBeneficiaries( LocalAccount owner )
    {
        return ofy().load().type( BeneficiaryBankAccount.class ).filter( "owner", owner ).count();
    }
}