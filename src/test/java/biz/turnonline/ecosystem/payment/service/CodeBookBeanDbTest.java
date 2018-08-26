package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.account.client.model.Account;
import biz.turnonline.ecosystem.payment.service.model.BankCode;
import org.ctoolkit.agent.service.impl.ImportTask;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

/**
 * {@link CodeBookBean} unit testing against emulated (local) App Engine services including datastore.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class CodeBookBeanDbTest
        extends BackendServiceTestCase
{
    @Inject
    private CodeBook tested;

    private Account account;

    @BeforeMethod
    public void before()
    {
        account = getFromFile( "account.json", Account.class );

        // import bank code code-book
        ImportTask task = new ImportTask( "/dataset/changeset_00001.xml" );
        task.run();
    }

    @Test
    public void getBankCodes()
    {
        // en-SK
        Map<String, BankCode> bankCodes = tested.getBankCodes( account, CodeBook.DEFAULT_LOCALE, CodeBook.DEFAULT_DOMICILE );
        assertThat( bankCodes ).isNotNull();
        assertThat( bankCodes ).hasSize( 35 );

        BankCode bankCode = bankCodes.get( "0200" );
        assertThat( bankCode ).isNotNull();
        assertThat( bankCode.getLocale() ).isEqualTo( "en" );
        assertThat( bankCode.getCountry() ).isEqualTo( CodeBook.DEFAULT_DOMICILE );

        // cached value retrieval
        bankCodes = tested.getBankCodes( account, CodeBook.DEFAULT_LOCALE, CodeBook.DEFAULT_DOMICILE );
        assertThat( bankCodes ).isNotNull();
        assertThat( bankCodes ).hasSize( 35 );

        // default locale and domicile taken from the account
        bankCodes = tested.getBankCodes( account, null, null );
        assertThat( bankCodes ).isNotNull();
        assertThat( bankCodes ).hasSize( 35 );

        // cs-SK
        bankCodes = tested.getBankCodes( account, new Locale( "cs" ), "SK" );
        assertThat( bankCodes ).isNotNull();
        assertThat( bankCodes ).hasSize( 35 );

        bankCode = bankCodes.get( "0200" );
        assertThat( bankCode ).isNotNull();
        assertThat( bankCode.getLocale() ).isEqualTo( "cs" );
        assertThat( bankCode.getCountry() ).isEqualTo( "SK" );

        // sk-SK
        bankCodes = tested.getBankCodes( account, new Locale( "sk" ), "SK" );
        assertThat( bankCodes ).isNotNull();
        assertThat( bankCodes ).hasSize( 35 );

        bankCode = bankCodes.get( "0200" );
        assertThat( bankCode ).isNotNull();
        assertThat( bankCode.getLocale() ).isEqualTo( "sk" );
        assertThat( bankCode.getCountry() ).isEqualTo( "SK" );

        // en-CZ
        bankCodes = tested.getBankCodes( account, new Locale( "en" ), "CZ" );
        assertThat( bankCodes ).isNotNull();
        assertThat( bankCodes ).hasSize( 52 );

        bankCode = bankCodes.get( "0100" );
        assertThat( bankCode ).isNotNull();
        assertThat( bankCode.getLocale() ).isEqualTo( "en" );
        assertThat( bankCode.getCountry() ).isEqualTo( "CZ" );

        // cs-CZ
        bankCodes = tested.getBankCodes( account, new Locale( "cs" ), "CZ" );
        assertThat( bankCodes ).isNotNull();
        assertThat( bankCodes ).hasSize( 52 );

        bankCode = bankCodes.get( "0100" );
        assertThat( bankCode ).isNotNull();
        assertThat( bankCode.getLocale() ).isEqualTo( "cs" );
        assertThat( bankCode.getCountry() ).isEqualTo( "CZ" );

        // sk-CZ
        bankCodes = tested.getBankCodes( account, new Locale( "sk" ), "CZ" );
        assertThat( bankCodes ).isNotNull();
        assertThat( bankCodes ).hasSize( 52 );

        bankCode = bankCodes.get( "0100" );
        assertThat( bankCode ).isNotNull();
        assertThat( bankCode.getLocale() ).isEqualTo( "sk" );
        assertThat( bankCode.getCountry() ).isEqualTo( "CZ" );
    }

    @Test
    public void singleBankCodeRetrieval()
    {
        BankCode bankCode = tested.getBankCode( account, "1111", new Locale( "en" ), "SK" );
        assertThat( bankCode ).isNotNull();
        assertThat( bankCode.getLocale() ).isEqualTo( "en" );
        assertThat( bankCode.getCountry() ).isEqualTo( "SK" );

        // testing caching
        BankCode cached = tested.getBankCode( account, "1111", new Locale( "en" ), "SK" );
        assertThat( cached ).isNotNull();
        assertThat( cached.getLocale() ).isEqualTo( "en" );
        assertThat( cached.getCountry() ).isEqualTo( "SK" );
    }

    @Test
    public void singleBankCodeRetrieval_WithDefaultLocaleDomicile()
    {
        BankCode bankCode = tested.getBankCode( account, "5600", null, null );
        assertThat( bankCode ).isNotNull();
        // account locale is 'en'
        assertThat( bankCode.getLocale() ).isEqualTo( "en" );
        // account business domicile is 'SK'
        assertThat( bankCode.getCountry() ).isEqualTo( "SK" );

        // testing caching
        BankCode cached = tested.getBankCode( account, "5600", null, null );
        assertThat( cached ).isNotNull();
        assertThat( cached.getLocale() ).isEqualTo( "en" );
        assertThat( cached.getCountry() ).isEqualTo( "SK" );
    }

    @Test
    public void singleBankCodeRetrieval_NotFound()
    {
        BankCode bankCode = tested.getBankCode( account, "0987", null, null );
        assertThat( bankCode ).isNull();
    }
}