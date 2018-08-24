package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.account.client.model.Account;
import biz.turnonline.ecosystem.account.client.model.Domicile;
import biz.turnonline.ecosystem.payment.service.model.BankAccount;
import biz.turnonline.ecosystem.payment.service.model.BankCode;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import org.ctoolkit.services.storage.EntityExecutor;
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
    private LocalAccountProvider accProvider;

    @Mocked
    private LocalAccount owner;

    @Mocked
    private Account account;

    @Test
    public void getPrimaryBankAccountSellerCzNullState()
    {
        final List<BankAccount> list = getBankAccounts();

        new Expectations( tested )
        {
            {
                tested.getBankAccounts( account, null, null );
                result = list;

                codeBook.getDomicile( account, null );
                result = Domicile.CZ.name();
            }
        };

        BankAccount account = tested.getPrimaryBankAccount( this.account, null );

        // primary bank account state is SK
        assertEquals( Domicile.SK, account.getCountry() );
        assertEquals( "Primary Bank Account", account.getName() );
        assertTrue( account.isPrimary() );
        assertNotEquals( BankAccount.TRUST_PAY_BANK_CODE, account.getBankCode() );
    }

    @Test
    public void getPrimaryBankAccount()
    {
        final List<BankAccount> list = getBankAccounts();

        new Expectations( tested )
        {
            {
                tested.getBankAccounts( account, null, null );
                result = list;

                codeBook.getDomicile( account, null );
                result = Domicile.CZ.name();
            }
        };

        BankAccount account = tested.getPrimaryBankAccount( this.account );

        // primary bank account state is SK
        assertEquals( Domicile.SK, account.getCountry() );
        assertEquals( "Primary Bank Account", account.getName() );
        assertTrue( account.isPrimary() );
        assertNotEquals( BankAccount.TRUST_PAY_BANK_CODE, account.getBankCode() );
    }

    @Test
    public void getPrimaryBankAccountSellerSkNullState()
    {
        final List<BankAccount> list = getBankAccounts();

        new Expectations( tested )
        {
            {
                tested.getBankAccounts( account, null, null );
                result = list;
            }
        };

        BankAccount account = tested.getPrimaryBankAccount( this.account, null );

        // state is same to the domicile state
        assertEquals( Domicile.SK, account.getCountry() );
        // there is an account marked as primary
        assertEquals( "Primary Bank Account", account.getName() );
        assertTrue( account.isPrimary() );
        assertNotEquals( BankAccount.TRUST_PAY_BANK_CODE, account.getBankCode() );
    }

    @Test
    public void getPrimaryBankAccountForSk()
    {
        final List<BankAccount> list = getBankAccounts();

        new Expectations( tested )
        {
            {
                tested.getBankAccounts( account, null, null );
                result = list;
            }
        };

        BankAccount account = tested.getPrimaryBankAccount( this.account, "SK" );

        // asking for SK bank account
        assertEquals( Domicile.SK, account.getCountry() );
        // there is an account marked as primary
        assertEquals( "Primary Bank Account", account.getName() );
        assertTrue( account.isPrimary() );
        assertNotEquals( BankAccount.TRUST_PAY_BANK_CODE, account.getBankCode() );
    }

    @Test
    public void getPrimaryBankAccountForCz()
    {
        final List<BankAccount> list = getBankAccounts();

        new Expectations( tested )
        {
            {
                tested.getBankAccounts( account, null, null );
                result = list;
            }
        };

        BankAccount account = tested.getPrimaryBankAccount( this.account, "CZ" );

        // asking for CZ bank account
        assertEquals( Domicile.CZ, account.getCountry() );
        assertEquals( "2 Account", account.getName() );
        assertNotEquals( BankAccount.TRUST_PAY_BANK_CODE, account.getBankCode() );
    }

    @Test
    public void getPrimaryBankAccountNoCountryMatch()
    {
        final List<BankAccount> list = getBankAccounts();

        new Expectations( tested )
        {
            {
                tested.getBankAccounts( account, null, null );
                result = list;
            }
        };

        // ask for an account for non existing country AQ (Antarktida)
        BankAccount account = tested.getPrimaryBankAccount( this.account, "AQ" );

        // asking for CZ bank account
        assertNotNull( account );
        assertEquals( "Primary Bank Account", account.getName() );
        assertNotEquals( BankAccount.TRUST_PAY_BANK_CODE, account.getBankCode() );
    }

    @Test
    public void getAlternativeBankAccountsSellerSkNoExclude()
    {
        final List<BankAccount> list = getBankAccounts();
        expectationsBankAccountsDomicileSk( list );

        List<BankAccount.Description> descriptions = tested.getAlternativeBankAccounts( account, null );

        assertEquals( 4, descriptions.size() );
        assertEquals( getBankAccount1().getFormattedBankAccount(), descriptions.get( 0 ).getFullBankAccountNumber() );
        assertEquals( getBankAccount3().getFormattedBankAccount(), descriptions.get( 1 ).getFullBankAccountNumber() );

    }

    @Test
    public void getAlternativeBankAccountsSellerCzNoExclude()
    {
        final List<BankAccount> list = getBankAccounts();
        expectationsBankAccountsDomicileCz( list );

        List<BankAccount.Description> descriptions = tested.getAlternativeBankAccounts( account, null );

        assertEquals( 4, descriptions.size() );
        assertEquals( getBankAccount5().getFormattedBankAccount(), descriptions.get( 0 ).getFullBankAccountNumber() );
        assertEquals( getBankAccount4().getFormattedBankAccount(), descriptions.get( 1 ).getFullBankAccountNumber() );

    }

    @Test
    public void getAlternativeBankAccountsSellerSkExclude()
    {
        final List<BankAccount> list = getBankAccounts();
        expectationsBankAccountsDomicileSk( list );

        List<BankAccount.Description> descriptions = tested.getAlternativeBankAccounts( account, getBankAccount1() );

        assertEquals( 3, descriptions.size() );
        assertEquals( getBankAccount3().getFormattedBankAccount(), descriptions.get( 0 ).getFullBankAccountNumber() );
    }

    @Test
    public void getAlternativeBankAccountsSellerCzExclude()
    {
        final List<BankAccount> list = getBankAccounts();
        expectationsBankAccountsDomicileCz( list );

        List<BankAccount.Description> descriptions = tested.getAlternativeBankAccounts( account, getBankAccount5() );

        assertEquals( 3, descriptions.size() );
        assertEquals( getBankAccount4().getFormattedBankAccount(), descriptions.get( 0 ).getFullBankAccountNumber() );
    }

    @Test
    public void getAlternativeBankAccountsNoBankCode()
    {
        final List<BankAccount> list = getBankAccounts();

        new Expectations( tested )
        {
            {
                tested.getBankAccounts( account, null, null );
                result = list;

                codeBook.getDomicile( account, null );
                result = Domicile.CZ.name();
            }
        };

        List<BankAccount.Description> descriptions = tested.getAlternativeBankAccounts( account, null );

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
                return owner;
            }
        };

        bankAccount.setName( "B Account" );
        bankAccount.setAccountNumber( "2629874222" );
        bankAccount.setBankCode( "1100" );
        bankAccount.setMerchantId( "1515" );
        bankAccount.setNotificationEmail( "seller.b@gmail.com" );
        bankAccount.setCountry( Domicile.SK );
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
                return owner;
            }
        };

        bankAccount.setName( "A Account" );
        bankAccount.setAccountNumber( "2629874111" );
        bankAccount.setBankCode( BankAccount.TRUST_PAY_BANK_CODE );
        bankAccount.setMerchantId( "3215" );
        bankAccount.setNotificationEmail( "seller.a@gmail.com" );
        bankAccount.setCountry( Domicile.SK );
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
                return owner;
            }
        };

        bankAccount.setName( "Primary Bank Account" );
        bankAccount.setAccountNumber( "2629874444" );
        bankAccount.setBankCode( "0200" );
        bankAccount.setMerchantId( "2205" );
        bankAccount.setNotificationEmail( "seller.d@gmail.com" );
        bankAccount.setCountry( Domicile.SK );
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
                return owner;
            }
        };

        bankAccount.setName( "C Account" );
        bankAccount.setAccountNumber( "2629874333" );
        bankAccount.setBankCode( "0800" );
        bankAccount.setMerchantId( "1122" );
        bankAccount.setNotificationEmail( "seller.c@gmail.com" );
        bankAccount.setCountry( Domicile.CZ );
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
                return owner;
            }
        };

        bankAccount.setName( "2 Account" );
        bankAccount.setAccountNumber( "2629874000" );
        bankAccount.setBankCode( "0100" );
        bankAccount.setMerchantId( "2020" );
        bankAccount.setNotificationEmail( "seller.1@gmail.com" );
        bankAccount.setCountry( Domicile.CZ );
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
                return owner;
            }
        };

        bankAccount.setName( "1 Account" );
        bankAccount.setAccountNumber( "2629874555" );
        bankAccount.setBankCode( BankAccount.TRUST_PAY_BANK_CODE );
        bankAccount.setMerchantId( "3722" );
        bankAccount.setNotificationEmail( "seller.e@gmail.com" );
        bankAccount.setCountry( Domicile.CZ );
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
        new Expectations( tested )
        {
            {
                tested.getBankAccounts( account, null, null );
                result = list;

                codeBook.getDomicile( account, null );
                result = Domicile.SK.name();

                codeBook.getBankCodes( account, ( Locale ) any, anyString );
                result = getCodeBookMap();
            }
        };
    }

    private void expectationsBankAccountsDomicileCz( List<BankAccount> list )
    {
        new Expectations( tested )
        {
            {
                tested.getBankAccounts( account, null, null );
                result = list;

                codeBook.getDomicile( account, null );
                result = Domicile.CZ.name();

                codeBook.getBankCodes( account, ( Locale ) any, anyString );
                result = getCodeBookMap();
            }
        };
    }
}
