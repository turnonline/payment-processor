package biz.turnonline.ecosystem.payment.api;

import biz.turnonline.ecosystem.account.client.model.Account;
import biz.turnonline.ecosystem.payment.api.model.BankAccount;
import biz.turnonline.ecosystem.payment.service.BackendServiceTestCase;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.NotFoundException;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import org.ctoolkit.agent.service.impl.ImportTask;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.testng.FileAssert.fail;

/**
 * {@link BankAccount} service wide use case testing against emulated (local) App Engine services.
 * Calls crossing the boundaries of this microservice are mocked.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class BankAccountUseCaseTest
        extends BackendServiceTestCase
{
    @Inject
    private BankAccountEndpoint endpoint;

    @Mocked
    private HttpServletRequest request;

    @Mocked
    private User authUser;

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
    public void bankAccountFullLifecycle() throws Exception
    {
        mockUpCheckAccount();
        mockUpLocalAccount();

        // creating bank accounts
        BankAccount apiBankAccount = getFromFile( "bank-account-1.json", BankAccount.class );
        BankAccount firstBankAccount = endpoint.insertBankAccount( apiBankAccount, request, authUser );

        assertThat( firstBankAccount ).isNotNull();
        assertThat( firstBankAccount.getBank().getCode() ).isEqualTo( "1100" );
        assertThat( firstBankAccount.getBank().getCountry() ).isEqualTo( "SK" );
        assertThat( firstBankAccount.getBank().getLabel() ).isEqualTo( "Tatra banka, a.s." );
        assertThat( firstBankAccount.getPrimary() ).isFalse();

        apiBankAccount = getFromFile( "bank-account-2.json", BankAccount.class );
        BankAccount secondBankAccount = endpoint.insertBankAccount( apiBankAccount, request, authUser );

        assertThat( secondBankAccount ).isNotNull();
        assertThat( secondBankAccount.getBank().getLabel() ).isNotNull();
        assertThat( secondBankAccount.getBank().getCode() ).isEqualTo( "0900" );
        assertThat( secondBankAccount.getBank().getCountry() ).isEqualTo( "SK" );
        assertThat( secondBankAccount.getPrimary() ).isFalse();

        apiBankAccount = getFromFile( "bank-account-3.json", BankAccount.class );
        BankAccount thirdBankAccount = endpoint.insertBankAccount( apiBankAccount, request, authUser );

        assertThat( thirdBankAccount ).isNotNull();
        assertThat( thirdBankAccount.getBank().getLabel() ).isNotNull();
        assertThat( thirdBankAccount.getBank().getCode() ).isEqualTo( "8360" );
        assertThat( thirdBankAccount.getBank().getCountry() ).isEqualTo( "SK" );
        assertThat( thirdBankAccount.getFormatted() ).isEqualTo( "520700-4200012345/8360" );
        assertThat( thirdBankAccount.getPrimary() ).isFalse();

        apiBankAccount = getFromFile( "bank-account-2.json", BankAccount.class );
        BankAccount duplicatedBankAccount = endpoint.insertBankAccount( apiBankAccount, request, authUser );
        assertThat( duplicatedBankAccount ).isNotNull();
        assertThat( duplicatedBankAccount.getBank().getCode() ).isEqualTo( "0900" );
        assertThat( duplicatedBankAccount.getPrimary() ).isFalse();

        // getting bank account test
        ofy().clear();

        BankAccount bankAccount = endpoint.getBankAccount( firstBankAccount.getId(), request, authUser );
        assertThat( bankAccount ).isNotNull();
        assertThat( bankAccount ).isEqualTo( firstBankAccount );

        // bank account update
        duplicatedBankAccount.setName( "Bank account that will be removed later" );
        Long id = duplicatedBankAccount.getId();

        endpoint.updateBankAccount( id, duplicatedBankAccount, request, authUser );
        ofy().clear();

        bankAccount = endpoint.getBankAccount( id, request, authUser );
        assertThat( bankAccount ).isNotNull();
        assertThat( bankAccount ).isEqualTo( duplicatedBankAccount );

        // getting primary account
        try
        {
            endpoint.getPrimaryBankAccount( "SK", request, authUser );
            fail( "There should not be a primary account at all" );
        }
        catch ( NotFoundException ignored )
        {
        }

        // delete bank account
        endpoint.deleteBankAccount( id, request, authUser );
        try
        {
            endpoint.getBankAccount( id, request, authUser );
            fail( "The bank account '" + duplicatedBankAccount.getName() + "' should be already deleted." );
        }
        catch ( NotFoundException ignored )
        {
        }

        // marking primary bank account
        bankAccount = endpoint.markBankAccountAsPrimary( firstBankAccount.getId(), request, authUser );
        assertThat( bankAccount ).isNotNull();
        assertThat( bankAccount.getId() ).isEqualTo( firstBankAccount.getId() );
        assertThat( bankAccount.getPrimary() ).isTrue();

        BankAccount primary = endpoint.getPrimaryBankAccount( "SK", request, authUser );
        assertThat( primary ).isNotNull();
        assertThat( primary.getPrimary() ).isTrue();
        assertThat( primary ).isEqualTo( bankAccount );

        try
        {
            endpoint.deleteBankAccount( primary.getId(), request, authUser );
            fail( "The primary bank account cannot be deleted" );
        }
        catch ( BadRequestException ignored )
        {
        }

        // getting alternative bank accounts
        List<BankAccount> alternative = endpoint.searchBankAccounts( null, null, "Sk",
                true, request, authUser );

        assertThat( alternative ).isNotNull();
        assertThat( alternative ).hasSize( 2 );
        assertThat( alternative ).doesNotContain( primary );
    }

    private void mockUpCheckAccount()
    {
        new MockUp<EndpointsCommon>()
        {
            @Mock
            public Account checkAccount( User authUser, HttpServletRequest request )
            {
                return account;
            }
        };
    }

    private void mockUpLocalAccount()
    {
        new MockUp<LocalAccount>()
        {
            @Mock
            public Account getRemote()
            {
                return account;
            }
        };
    }
}
