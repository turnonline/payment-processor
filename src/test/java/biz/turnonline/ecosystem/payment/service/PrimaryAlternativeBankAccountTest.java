package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.payment.service.model.BankAccount;
import biz.turnonline.ecosystem.payment.service.model.BankCode;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.steward.facade.Domicile;
import biz.turnonline.ecosystem.steward.model.Account;
import ma.glasnost.orika.MapperFacade;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import org.ctoolkit.services.storage.EntityExecutor;
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

    @Mocked
    private CodeBook codeBook;

    @Injectable
    private EntityExecutor datastore;

    @Injectable
    private MapperFacade mapper;

    @Injectable
    private LocalAccountProvider accProvider;

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
        List<BankAccount> list = getBankAccounts();

        new Expectations( tested, account )
        {
            {
                tested.getBankAccounts( account, null, null, null );
                result = list;

                account.getDomicile();
                result = Domicile.CZ;
            }
        };

        BankAccount primary = tested.getPrimaryBankAccount( account, null );

        // primary bank account country is SK
        assertEquals( Domicile.CZ.name(), primary.getCountry() );
        assertTrue( primary.isPrimary() );
    }

    @Test
    public void getPrimaryBankAccountSellerSkNullCountry()
    {
        final List<BankAccount> list = getBankAccounts();

        new Expectations( tested, account )
        {
            {
                tested.getBankAccounts( account, null, null, null );
                result = list;

                account.getDomicile();
                result = Domicile.SK;
            }
        };

        BankAccount primary = tested.getPrimaryBankAccount( account, null );

        // state is same to the domicile country
        assertEquals( Domicile.SK.name(), primary.getCountry() );
        // there is an account marked as primary
        assertEquals( "Primary Bank Account", primary.getName() );
        assertTrue( primary.isPrimary() );
        assertNotEquals( BankAccount.TRUST_PAY_BANK_CODE, primary.getBankCode() );
    }

    @Test
    public void getPrimaryBankAccountForSk()
    {
        final List<BankAccount> list = getBankAccounts();

        new Expectations( tested )
        {
            {
                tested.getBankAccounts( account, null, null, null );
                result = list;
            }
        };

        BankAccount primary = tested.getPrimaryBankAccount( account, "SK" );

        // asking for SK bank account
        assertEquals( Domicile.SK.name(), primary.getCountry() );
        // there is an account marked as primary
        assertEquals( "Primary Bank Account", primary.getName() );
        assertTrue( primary.isPrimary() );
    }

    @Test
    public void getPrimaryBankAccountForCz()
    {
        final List<BankAccount> list = getBankAccounts();

        new Expectations( tested )
        {
            {
                tested.getBankAccounts( account, null, null, null );
                result = list;
            }
        };

        BankAccount primary = tested.getPrimaryBankAccount( account, "CZ" );

        // asking for CZ bank account
        assertEquals( Domicile.CZ.name(), primary.getCountry() );
        assertEquals( "1 Account", primary.getName() );
        assertNotEquals( "0100", primary.getBankCode() );
    }

    @Test
    public void getPrimaryBankAccountNoCountryMatch()
    {
        final List<BankAccount> list = getBankAccounts();

        new Expectations( tested )
        {
            {
                tested.getBankAccounts( account, null, null, null );
                result = list;
            }
        };

        // ask for an account for non existing country AQ (Antarktida)
        BankAccount primary = tested.getPrimaryBankAccount( account, "AQ" );

        // asking for CZ bank account
        assertNotNull( primary );
        assertTrue( primary.isPrimary() );
    }

    @Test( expectedExceptions = BankAccountNotFound.class )
    public void getPrimaryBankAccount_NotFound()
    {
        final List<BankAccount> list = getBankAccounts();
        for ( BankAccount next : list )
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
        final List<BankAccount> list = getBankAccounts();
        expectationsBankAccountsDomicileSk( list );

        new Expectations( tested )
        {
            {
                tested.getInternalPrimaryBankAccount( account, anyString );
                result = null;
            }
        };

        List<BankAccount> descriptions = tested.getAlternativeBankAccounts( account, null, null, null, null );

        assertEquals( 4, descriptions.size() );
        assertEquals( getBankAccount1().getFormattedBankAccount(), descriptions.get( 0 ).getFormattedBankAccount() );
        assertEquals( getBankAccount3().getFormattedBankAccount(), descriptions.get( 1 ).getFormattedBankAccount() );

    }

    @Test
    public void getAlternativeBankAccountsSellerCzNoExclude()
    {
        final List<BankAccount> list = getBankAccounts();
        expectationsBankAccountsDomicileCz( list );

        new Expectations( tested )
        {
            {
                tested.getInternalPrimaryBankAccount( account, anyString );
                result = null;
            }
        };

        List<BankAccount> descriptions = tested.getAlternativeBankAccounts( account, null, null, null, null );

        assertEquals( 4, descriptions.size() );
        assertEquals( getBankAccount4().getFormattedBankAccount(), descriptions.get( 0 ).getFormattedBankAccount() );
        assertEquals( getBankAccount5().getFormattedBankAccount(), descriptions.get( 1 ).getFormattedBankAccount() );

    }

    @Test
    public void getAlternativeBankAccountsSellerSkExclude()
    {
        final List<BankAccount> list = getBankAccounts();
        expectationsBankAccountsDomicileSk( list );

        new Expectations( tested )
        {
            {
                tested.getInternalPrimaryBankAccount( account, anyString );
                result = getBankAccount1();
            }
        };

        List<BankAccount> descriptions = tested.getAlternativeBankAccounts( account, null, null, null, null );

        assertEquals( 3, descriptions.size() );
        assertEquals( getBankAccount3().getFormattedBankAccount(), descriptions.get( 0 ).getFormattedBankAccount() );
    }

    @Test
    public void getAlternativeBankAccountsSellerCzExclude()
    {
        final List<BankAccount> list = getBankAccounts();
        expectationsBankAccountsDomicileCz( list );

        new Expectations( tested )
        {
            {
                tested.getInternalPrimaryBankAccount( account, anyString );
                result = getBankAccount5();
            }
        };

        List<BankAccount> descriptions = tested.getAlternativeBankAccounts( account, null, null, null, null );

        assertEquals( 3, descriptions.size() );
        assertEquals( getBankAccount4().getFormattedBankAccount(), descriptions.get( 0 ).getFormattedBankAccount() );
    }

    @Test
    public void getAlternativeBankAccountsNoBankCode()
    {
        final List<BankAccount> list = getBankAccounts();

        new Expectations( tested, account )
        {
            {
                tested.getBankAccounts( account, null, null, null );
                result = list;

                account.getDomicile();
                result = Domicile.CZ;
            }
        };

        List<BankAccount> descriptions = tested.getAlternativeBankAccounts( account, null, null, null, null );

        assertEquals( 0, descriptions.size() );
    }

    private List<BankAccount> getBankAccounts()
    {
        List<BankAccount> list = new ArrayList<>();

        list.add( getBankAccount1() );
        list.add( getBankAccount2() );
        list.add( getBankAccount3() );
        list.add( getBankAccount4() );
        list.add( getBankAccount5() );
        list.add( getBankAccount6() );

        return list;
    }

    private BankAccount getBankAccount1()
    {
        BankAccount bankAccount = new BankAccount( codeBook )
        {
            @Override
            public LocalAccount getOwner()
            {
                return account;
            }
        };

        bankAccount.setName( "B Account" );
        bankAccount.setAccountNumber( "2629874222" );
        bankAccount.setBankCode( "1100" );
        bankAccount.setMerchantId( "1515" );
        bankAccount.setNotificationEmail( "seller.b@gmail.com" );
        bankAccount.setCountry( Domicile.SK.name() );
        bankAccount.setPrimary( false );

        return bankAccount;
    }

    private BankAccount getBankAccount2()
    {
        BankAccount bankAccount = new BankAccount( codeBook )
        {
            @Override
            public LocalAccount getOwner()
            {
                return account;
            }
        };

        bankAccount.setName( "A Account" );
        bankAccount.setAccountNumber( "2629874111" );
        bankAccount.setBankCode( BankAccount.TRUST_PAY_BANK_CODE );
        bankAccount.setMerchantId( "3215" );
        bankAccount.setNotificationEmail( "seller.a@gmail.com" );
        bankAccount.setCountry( Domicile.SK.name() );
        bankAccount.setPrimary( false );

        return bankAccount;
    }

    private BankAccount getBankAccount3()
    {
        BankAccount bankAccount = new BankAccount( codeBook )
        {
            @Override
            public LocalAccount getOwner()
            {
                return account;
            }
        };

        bankAccount.setName( "Primary Bank Account" );
        bankAccount.setAccountNumber( "2629874444" );
        bankAccount.setBankCode( "0200" );
        bankAccount.setMerchantId( "2205" );
        bankAccount.setNotificationEmail( "seller.d@gmail.com" );
        bankAccount.setCountry( Domicile.SK.name() );
        bankAccount.setPrimary( true );

        return bankAccount;
    }

    private BankAccount getBankAccount4()
    {
        BankAccount bankAccount = new BankAccount( codeBook )
        {
            @Override
            public LocalAccount getOwner()
            {
                return account;
            }
        };

        bankAccount.setName( "C Account" );
        bankAccount.setAccountNumber( "2629874333" );
        bankAccount.setBankCode( "0800" );
        bankAccount.setMerchantId( "1122" );
        bankAccount.setNotificationEmail( "seller.c@gmail.com" );
        bankAccount.setCountry( Domicile.CZ.name() );
        bankAccount.setPrimary( false );

        return bankAccount;
    }

    private BankAccount getBankAccount5()
    {
        BankAccount bankAccount = new BankAccount( codeBook )
        {
            @Override
            public LocalAccount getOwner()
            {
                return account;
            }
        };

        bankAccount.setName( "2 Account" );
        bankAccount.setAccountNumber( "2629874000" );
        bankAccount.setBankCode( "0100" );
        bankAccount.setMerchantId( "2020" );
        bankAccount.setNotificationEmail( "seller.1@gmail.com" );
        bankAccount.setCountry( Domicile.CZ.name() );
        bankAccount.setPrimary( false );

        return bankAccount;
    }

    private BankAccount getBankAccount6()
    {
        BankAccount bankAccount = new BankAccount( codeBook )
        {
            @Override
            public LocalAccount getOwner()
            {
                return account;
            }
        };

        bankAccount.setName( "1 Account" );
        bankAccount.setAccountNumber( "2629874555" );
        bankAccount.setBankCode( BankAccount.TRUST_PAY_BANK_CODE );
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

        bankCode = new BankCode( BankAccount.TRUST_PAY_BANK_CODE, "Trust Pay, a.s.", "sk", Domicile.SK.name() );
        codeBook.put( BankAccount.TRUST_PAY_BANK_CODE, ( T ) bankCode );

        bankCode = new BankCode( "0200", "VUB, a.s.", "sk", Domicile.SK.name() );
        codeBook.put( "0200", ( T ) bankCode );

        bankCode = new BankCode( "0800", "Ceska sporitelna, a.s.", "sk", Domicile.SK.name() );
        codeBook.put( "0800", ( T ) bankCode );

        bankCode = new BankCode( "0100", "Komercni banka, a.s.", "sk", Domicile.SK.name() );
        codeBook.put( "0100", ( T ) bankCode );

        return codeBook;
    }

    private void expectationsBankAccountsDomicileSk( List<BankAccount> list )
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

    private void expectationsBankAccountsDomicileCz( List<BankAccount> list )
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
