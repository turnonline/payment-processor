package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.payment.service.model.BankCode;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.steward.facade.Domicile;
import biz.turnonline.ecosystem.steward.model.Account;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import org.ctoolkit.services.storage.EntityExecutor;
import org.ctoolkit.services.task.TaskExecutor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

/**
 * {@link PaymentConfigBean} unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@SuppressWarnings( "serial" )
public class PrimaryAlternativeBankAccountTest
{
    @Tested
    private PaymentConfigBean tested;

    @Injectable
    private CodeBook codeBook;

    @Injectable
    private EntityExecutor datastore;

    @Injectable
    private TaskExecutor executor;

    private LocalAccount account;

    @BeforeMethod
    public void before()
    {
        account = new LocalAccount( new Account()
                .setId( 1735L )
                .setEmail( "my.account@turnonline.biz" )
                .setIdentityId( "64HGtr6ks" )
                .setAudience( "turn-online" ) );
    }

    @Test
    public void getPrimaryBankAccountSellerCzNullCountry()
    {
        List<CompanyBankAccount> list = getBankAccounts();

        new Expectations( tested, account )
        {
            {
                tested.getBankAccounts( account, null, null, null );
                result = list;

                account.getDomicile();
                result = Domicile.CZ;
            }
        };

        CompanyBankAccount primary = tested.getPrimaryBankAccount( account, null );

        // primary bank account country is SK
        assertEquals( Domicile.CZ.name(), primary.getCountry() );
        assertTrue( primary.isPrimary() );
    }

    @Test
    public void getPrimaryBankAccountSellerSkNullCountry()
    {
        final List<CompanyBankAccount> list = getBankAccounts();

        new Expectations( tested, account )
        {
            {
                tested.getBankAccounts( account, null, null, null );
                result = list;

                account.getDomicile();
                result = Domicile.SK;
            }
        };

        CompanyBankAccount primary = tested.getPrimaryBankAccount( account, null );

        // state is same to the domicile country
        assertEquals( Domicile.SK.name(), primary.getCountry() );
        // there is an account marked as primary
        assertEquals( "Primary Bank Account", primary.getName() );
        assertTrue( primary.isPrimary() );
        assertNotEquals( PaymentConfig.TRUST_PAY_BANK_CODE, primary.getBankCode() );
    }

    @Test
    public void getPrimaryBankAccountForSk()
    {
        final List<CompanyBankAccount> list = getBankAccounts();

        new Expectations( tested )
        {
            {
                tested.getBankAccounts( account, null, null, null );
                result = list;
            }
        };

        CompanyBankAccount primary = tested.getPrimaryBankAccount( account, "SK" );

        // asking for SK bank account
        assertEquals( Domicile.SK.name(), primary.getCountry() );
        // there is an account marked as primary
        assertEquals( "Primary Bank Account", primary.getName() );
        assertTrue( primary.isPrimary() );
    }

    @Test
    public void getPrimaryBankAccountForCz()
    {
        final List<CompanyBankAccount> list = getBankAccounts();

        new Expectations( tested )
        {
            {
                tested.getBankAccounts( account, null, null, null );
                result = list;
            }
        };

        CompanyBankAccount primary = tested.getPrimaryBankAccount( account, "CZ" );

        // asking for CZ bank account
        assertEquals( Domicile.CZ.name(), primary.getCountry() );
        assertEquals( "1 Account", primary.getName() );
        assertNotEquals( "0100", primary.getBankCode() );
    }

    @Test
    public void getPrimaryBankAccountNoCountryMatch()
    {
        final List<CompanyBankAccount> list = getBankAccounts();

        new Expectations( tested )
        {
            {
                tested.getBankAccounts( account, null, null, null );
                result = list;
            }
        };

        // ask for an account for non existing country AQ (Antarktida)
        CompanyBankAccount primary = tested.getPrimaryBankAccount( account, "AQ" );

        // asking for CZ bank account
        assertNotNull( primary );
        assertTrue( primary.isPrimary() );
    }

    @Test( expectedExceptions = BankAccountNotFound.class )
    public void getPrimaryBankAccount_NotFound()
    {
        final List<CompanyBankAccount> list = getBankAccounts();
        for ( CompanyBankAccount next : list )
        {
            next.setPrimary( false );
        }

        new Expectations( tested, account )
        {
            {
                tested.getBankAccounts( account, null, null, null );
                result = list;
            }
        };

        tested.getPrimaryBankAccount( account, "AQ" );
    }

    @Test
    public void getAlternativeBankAccountsSellerSkNoExclude()
    {
        final List<CompanyBankAccount> list = getBankAccounts();
        expectationsBankAccountsDomicileSk( list );

        new Expectations( tested )
        {
            {
                tested.getInternalPrimaryBankAccount( account, anyString );
                result = null;
            }
        };

        List<CompanyBankAccount> descriptions = tested.getAlternativeBankAccounts( account, null, null, null, null );

        assertEquals( 4, descriptions.size() );
        assertEquals( getBankAccount1().getIbanString(), descriptions.get( 0 ).getIbanString() );
        assertEquals( getBankAccount3().getIbanString(), descriptions.get( 1 ).getIbanString() );

    }

    @Test
    public void getAlternativeBankAccountsSellerCzNoExclude()
    {
        final List<CompanyBankAccount> list = getBankAccounts();
        expectationsBankAccountsDomicileCz( list );

        new Expectations( tested )
        {
            {
                tested.getInternalPrimaryBankAccount( account, anyString );
                result = null;
            }
        };

        List<CompanyBankAccount> descriptions = tested.getAlternativeBankAccounts( account, null, null, null, null );

        assertEquals( 4, descriptions.size() );
        assertEquals( getBankAccount4().getIbanString(), descriptions.get( 0 ).getIbanString() );
        assertEquals( getBankAccount5().getIbanString(), descriptions.get( 1 ).getIbanString() );

    }

    @Test
    public void getAlternativeBankAccountsSellerSkExclude()
    {
        final List<CompanyBankAccount> list = getBankAccounts();
        expectationsBankAccountsDomicileSk( list );

        new Expectations( tested )
        {
            {
                tested.getInternalPrimaryBankAccount( account, anyString );
                result = getBankAccount1();
            }
        };

        List<CompanyBankAccount> descriptions = tested.getAlternativeBankAccounts( account, null, null, null, null );

        assertEquals( 3, descriptions.size() );
        assertEquals( getBankAccount3().getIbanString(), descriptions.get( 0 ).getIbanString() );
    }

    @Test
    public void getAlternativeBankAccountsSellerCzExclude()
    {
        final List<CompanyBankAccount> list = getBankAccounts();
        expectationsBankAccountsDomicileCz( list );

        new Expectations( tested )
        {
            {
                tested.getInternalPrimaryBankAccount( account, anyString );
                result = getBankAccount5();
            }
        };

        List<CompanyBankAccount> descriptions = tested.getAlternativeBankAccounts( account, null, null, null, null );

        assertEquals( 3, descriptions.size() );
        assertEquals( getBankAccount4().getIbanString(), descriptions.get( 0 ).getIbanString() );
    }

    @Test
    public void getAlternativeBankAccountsNoBankCode()
    {
        final List<CompanyBankAccount> list = getBankAccounts();

        new Expectations( tested, account )
        {
            {
                tested.getBankAccounts( account, null, null, null );
                result = list;

                account.getDomicile();
                result = Domicile.CZ;
            }
        };

        List<CompanyBankAccount> descriptions = tested.getAlternativeBankAccounts( account, null, null, null, null );

        assertEquals( 0, descriptions.size() );
    }

    private List<CompanyBankAccount> getBankAccounts()
    {
        List<CompanyBankAccount> list = new ArrayList<>();

        list.add( getBankAccount1() );
        list.add( getBankAccount2() );
        list.add( getBankAccount3() );
        list.add( getBankAccount4() );
        list.add( getBankAccount5() );
        list.add( getBankAccount6() );

        return list;
    }

    private CompanyBankAccount getBankAccount1()
    {
        CompanyBankAccount bankAccount = new CompanyBankAccount( codeBook )
        {
            @Override
            public LocalAccount getOwner()
            {
                return account;
            }
        };

        bankAccount.setName( "B Account" );
        bankAccount.setIban( "SK6311003786278998869772" );
        bankAccount.setMerchantId( "1515" );
        bankAccount.setNotificationEmail( "seller.b@gmail.com" );
        bankAccount.setCountry( Domicile.SK.name() );
        bankAccount.setPrimary( false );

        return bankAccount;
    }

    private CompanyBankAccount getBankAccount2()
    {
        CompanyBankAccount bankAccount = new CompanyBankAccount( codeBook )
        {
            @Override
            public LocalAccount getOwner()
            {
                return account;
            }
        };

        bankAccount.setName( "A Account" );
        bankAccount.setIban( "SK4799529907645940188477" );
        bankAccount.setMerchantId( "3215" );
        bankAccount.setNotificationEmail( "seller.a@gmail.com" );
        bankAccount.setCountry( Domicile.SK.name() );
        bankAccount.setPrimary( false );

        return bankAccount;
    }

    private CompanyBankAccount getBankAccount3()
    {
        CompanyBankAccount bankAccount = new CompanyBankAccount( codeBook )
        {
            @Override
            public LocalAccount getOwner()
            {
                return account;
            }
        };

        bankAccount.setName( "Primary Bank Account" );
        bankAccount.setIban( "SK4702005866284676590760" );
        bankAccount.setMerchantId( "2205" );
        bankAccount.setNotificationEmail( "seller.d@gmail.com" );
        bankAccount.setCountry( Domicile.SK.name() );
        bankAccount.setPrimary( true );

        return bankAccount;
    }

    private CompanyBankAccount getBankAccount4()
    {
        CompanyBankAccount bankAccount = new CompanyBankAccount( codeBook )
        {
            @Override
            public LocalAccount getOwner()
            {
                return account;
            }
        };

        bankAccount.setName( "C Account" );
        bankAccount.setIban( "SK6108004743795632498503" );
        bankAccount.setMerchantId( "1122" );
        bankAccount.setNotificationEmail( "seller.c@gmail.com" );
        bankAccount.setCountry( Domicile.CZ.name() );
        bankAccount.setPrimary( false );

        return bankAccount;
    }

    private CompanyBankAccount getBankAccount5()
    {
        CompanyBankAccount bankAccount = new CompanyBankAccount( codeBook )
        {
            @Override
            public LocalAccount getOwner()
            {
                return account;
            }
        };

        bankAccount.setName( "2 Account" );
        bankAccount.setIban( "SK7201008812386074021228" );
        bankAccount.setMerchantId( "2020" );
        bankAccount.setNotificationEmail( "seller.1@gmail.com" );
        bankAccount.setCountry( Domicile.CZ.name() );
        bankAccount.setPrimary( false );

        return bankAccount;
    }

    private CompanyBankAccount getBankAccount6()
    {
        CompanyBankAccount bankAccount = new CompanyBankAccount( codeBook )
        {
            @Override
            public LocalAccount getOwner()
            {
                return account;
            }
        };

        bankAccount.setName( "1 Account" );
        bankAccount.setIban( "SK5799525711661487522498" );
        bankAccount.setMerchantId( "3722" );
        bankAccount.setNotificationEmail( "seller.e@gmail.com" );
        bankAccount.setCountry( Domicile.CZ.name() );
        // even marked as primary it cannot be included into the bank account list as it is TrustPay
        bankAccount.setPrimary( true );

        return bankAccount;
    }

    @SuppressWarnings( "unchecked" )
    private <T extends BankCode> Map<String, T> getCodeBookMap()
    {
        Map<String, T> codeBook = new HashMap<>();

        BankCode bankCode = new BankCode( "1100", "Tatra banka, a.s.", "sk", Domicile.SK.name() );
        codeBook.put( "1100", ( T ) bankCode );

        bankCode = new BankCode( PaymentConfig.TRUST_PAY_BANK_CODE, "Trust Pay, a.s.", "sk", Domicile.SK.name() );
        codeBook.put( PaymentConfig.TRUST_PAY_BANK_CODE, ( T ) bankCode );

        bankCode = new BankCode( "0200", "VUB, a.s.", "sk", Domicile.SK.name() );
        codeBook.put( "0200", ( T ) bankCode );

        bankCode = new BankCode( "0800", "Ceska sporitelna, a.s.", "sk", Domicile.SK.name() );
        codeBook.put( "0800", ( T ) bankCode );

        bankCode = new BankCode( "0100", "Komercni banka, a.s.", "sk", Domicile.SK.name() );
        codeBook.put( "0100", ( T ) bankCode );

        return codeBook;
    }

    private void expectationsBankAccountsDomicileSk( List<CompanyBankAccount> list )
    {
        new Expectations( tested, account )
        {
            {
                tested.getBankAccounts( account, null, null, null );
                result = list;

                account.getDomicile();
                result = Domicile.SK;

                codeBook.getBankCodes( account, ( Locale ) any, anyString );
                result = getCodeBookMap();
            }
        };
    }

    private void expectationsBankAccountsDomicileCz( List<CompanyBankAccount> list )
    {
        new Expectations( tested, account )
        {
            {
                tested.getBankAccounts( account, null, null, null );
                result = list;

                account.getDomicile();
                result = Domicile.CZ;

                //noinspection ConstantConditions
                codeBook.getBankCodes( ( LocalAccount ) any, ( Locale ) any, anyString );
                result = getCodeBookMap();
            }
        };
    }
}
