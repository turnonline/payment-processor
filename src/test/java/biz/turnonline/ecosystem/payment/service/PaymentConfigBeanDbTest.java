package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.payment.api.ApiValidationException;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.payment.service.model.PaymentGate;
import biz.turnonline.ecosystem.steward.model.Account;
import com.google.inject.Injector;
import org.ctoolkit.agent.service.impl.ImportTask;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * {@link PaymentConfigBean} unit testing incl. tests against emulated (local) App Engine datastore.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class PaymentConfigBeanDbTest
        extends BackendServiceTestCase
{
    @Inject
    private PaymentConfig bean;

    @Inject
    private Injector injector;

    private LocalAccount lAccount;

    private LocalAccount lAnother;

    @BeforeMethod
    public void before()
    {
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

        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( lAccount, 0, 10, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( originSize );

        CompanyBankAccount bankAccount = injector.getInstance( CompanyBankAccount.class );
        bankAccount.setIban( "SK3702005771190028932408" );
        bankAccount.setPrimary( false );
        bankAccount.setPaymentGate( PaymentGate.EPLATBY_VUB );

        assertThat( bankAccount.getCode() ).isNull();

        bean.insertBankAccount( lAccount, bankAccount );

        String expectedCode = String.format( PaymentConfig.BANK_ACCOUNT_CODE_FORMAT, originSize + 1 );
        assertThat( bankAccount.getCode() ).isEqualTo( expectedCode );

        bankAccounts = bean.getBankAccounts( lAccount, null, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 6 );
    }

    @Test
    public void getBankAccounts()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( lAccount, 0, 10, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 5 );

        bankAccounts = bean.getBankAccounts( lAnother, 0, 10, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 1 );

        assertThat( bankAccounts.get( 0 ).getOwner() ).isEqualTo( lAnother );

        // paging test
        bankAccounts = bean.getBankAccounts( lAccount, 0, 3, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 3 );

        bankAccounts = bean.getBankAccounts( lAccount, 3, 3, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 2 );
    }

    @Test
    public void getBankAccounts_CountryFilter()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( lAccount, 0, 10, "SK" );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 3 );

        bankAccounts = bean.getBankAccounts( lAccount, 0, 10, "cz" );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 2 );
    }

    @Test
    public void updateBankAccount_OwnerOk()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( lAccount, 0, 10, null );
        assertThat( bankAccounts ).isNotEmpty();

        CompanyBankAccount bankAccount = bankAccounts.get( 0 );
        bankAccount.setPaymentGate( PaymentGate.TRANSFER );

        bean.updateBankAccount( lAccount, bankAccount );
    }

    @Test( expectedExceptions = WrongEntityOwner.class )
    public void updateBankAccount_WrongOwner()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( lAccount, null, null, null );
        assertThat( bankAccounts ).isNotEmpty();

        CompanyBankAccount bankAccount = bankAccounts.get( 0 );
        bankAccount.setPaymentGate( PaymentGate.EPLATBY_VUB );

        bean.updateBankAccount( lAnother, bankAccount );
    }

    @Test
    public void deleteBankAccount()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( lAccount, 0, 10, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 5 );

        CompanyBankAccount bankAccount = bankAccounts.get( 1 );
        // test call
        CompanyBankAccount deleted = bean.deleteBankAccount( lAccount, bankAccount.getId() );
        assertThat( deleted ).isNotNull();
        assertThat( deleted ).isEqualTo( bankAccount );
        assertThat( deleted ).isEquivalentAccordingToCompareTo( bankAccount );

        // after deletion number of records check
        bankAccounts = bean.getBankAccounts( lAccount, null, null, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 4 );
    }

    @Test( expectedExceptions = WrongEntityOwner.class )
    public void deleteBankAccount_WrongOwner()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( lAccount, 0, 10, null );
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
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( lAccount, 0, 10, null );
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

        bankAccounts = bean.getBankAccounts( lAccount, null, null, null );
        assertThat( bankAccounts ).isNotNull();
        numberOfPrimary = bankAccounts.stream().filter( new PaymentConfigBean.BankAccountPrimary() ).count();
        assertWithMessage( "only single record is being marked as a primary bank account" )
                .that( numberOfPrimary )
                .isEqualTo( 1 );
    }

    @Test( expectedExceptions = WrongEntityOwner.class )
    public void markBankAccountAsPrimary_WrongOwner()
    {
        List<CompanyBankAccount> bankAccounts = bean.getBankAccounts( lAccount, 0, 10, null );
        assertThat( bankAccounts ).isNotNull();
        assertThat( bankAccounts ).hasSize( 5 );

        bean.markBankAccountAsPrimary( lAnother, bankAccounts.get( 0 ).getId() );
    }

    @Test( expectedExceptions = BankAccountNotFound.class )
    public void markBankAccountAsPrimary_NotFound()
    {
        bean.markBankAccountAsPrimary( lAccount, 8888L );
    }
}