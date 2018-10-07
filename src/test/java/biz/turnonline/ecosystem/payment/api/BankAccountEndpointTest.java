package biz.turnonline.ecosystem.payment.api;

import biz.turnonline.ecosystem.payment.api.model.BankAccount;
import biz.turnonline.ecosystem.payment.service.ApiValidationException;
import biz.turnonline.ecosystem.payment.service.BankAccountNotFound;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.WrongEntityOwner;
import biz.turnonline.ecosystem.steward.model.Account;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.NotFoundException;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MappingContext;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * {@link BankAccountEndpoint} unit testing, mainly negative scenarios.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class BankAccountEndpointTest
{
    @Tested
    private BankAccountEndpoint endpoint;

    @Injectable
    private EndpointsCommon common;

    @Injectable
    private MapperFacade mapper;

    @Injectable
    private PaymentConfig config;

    @Mocked
    private HttpServletRequest request;

    @Mocked
    private User authUser;

    private Account account = new Account();

    @Mocked
    private BankAccount bankAccount;

    @Mocked
    private biz.turnonline.ecosystem.payment.service.model.BankAccount dbBankAccount;

    @Test
    public void insertBankAccount() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.insertBankAccount( account, dbBankAccount );
            }
        };

        BankAccount result = endpoint.insertBankAccount( bankAccount, request, authUser );
        assertThat( result ).isNotNull();
    }

    @Test( expectedExceptions = BadRequestException.class )
    public void insertBankAccount_ValidationFailure() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.insertBankAccount( account, dbBankAccount );
                result = new ApiValidationException( "Validation failure" );
            }
        };

        endpoint.insertBankAccount( bankAccount, request, authUser );
    }

    @Test( expectedExceptions = BadRequestException.class )
    public void insertBankAccount_ApiValidationFailure() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                mapper.map( bankAccount, biz.turnonline.ecosystem.payment.service.model.BankAccount.class,
                        ( MappingContext ) any );
                result = new ApiValidationException( "Validation failure" );
            }
        };

        endpoint.insertBankAccount( bankAccount, request, authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void insertBankAccount_BusinessFlowFailure() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.insertBankAccount( account, dbBankAccount );
                result = new IllegalArgumentException();
            }
        };

        endpoint.insertBankAccount( bankAccount, request, authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void insertBankAccount_BackendError() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.insertBankAccount( account, dbBankAccount );
                result = new RuntimeException();
            }
        };

        endpoint.insertBankAccount( bankAccount, request, authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void insertBankAccount_BackendMappingError() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                mapper.map( dbBankAccount, BankAccount.class );
                result = new RuntimeException();
            }
        };

        endpoint.insertBankAccount( bankAccount, request, authUser );
    }

    @Test
    public void getBankAccounts() throws Exception
    {
        List<BankAccount> bankAccounts = new ArrayList<>();
        bankAccounts.add( bankAccount );

        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.getBankAccounts( account, 5, 15, null );

                //noinspection unchecked
                mapper.mapAsList( ( List<biz.turnonline.ecosystem.payment.service.model.BankAccount> ) any,
                        BankAccount.class, ( MappingContext ) any );
                result = bankAccounts;
            }
        };

        List<BankAccount> result = endpoint.searchBankAccounts( 5, 15, null, false, request, authUser );
        assertThat( result ).isNotNull();
        assertThat( result ).hasSize( 1 );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void getBankAccounts_BackendError() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.getBankAccounts( account, anyInt, anyInt, null );
                result = new RuntimeException();
            }
        };

        endpoint.searchBankAccounts( 5, 15, null, false, request, authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void getBankAccounts_BackendMappingError() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                //noinspection unchecked
                mapper.mapAsList( ( List<biz.turnonline.ecosystem.payment.service.model.BankAccount> ) any,
                        BankAccount.class, ( MappingContext ) any );
                result = new RuntimeException();
            }
        };

        endpoint.searchBankAccounts( 0, null, null, false, request, authUser );
    }

    @Test
    public void getBankAccount() throws Exception
    {
        long accountId = 199L;
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.getBankAccount( account, accountId );
                result = dbBankAccount;

                mapper.map( dbBankAccount, BankAccount.class );
                result = bankAccount;
            }
        };

        BankAccount result = endpoint.getBankAccount( accountId, request, authUser );
        assertThat( result ).isNotNull();
    }

    @Test( expectedExceptions = NotFoundException.class )
    public void getBankAccount_NotFound() throws Exception
    {
        long accountId = 23;
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.getBankAccount( account, accountId );
                result = new BankAccountNotFound( accountId );
            }
        };

        endpoint.getBankAccount( accountId, request, authUser );
    }

    @Test( expectedExceptions = NotFoundException.class )
    public void getBankAccount_WrongOwner() throws Exception
    {
        long accountId = 19;
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.getBankAccount( account, accountId );
                result = new WrongEntityOwner();
            }
        };

        endpoint.getBankAccount( accountId, request, authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void getBankAccount_BackendError() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.getBankAccount( account, anyLong );
                result = new RuntimeException();
            }
        };

        endpoint.getBankAccount( 67L, request, authUser );
    }

    @Test
    public void updateBankAccount() throws Exception
    {
        long accountId = 219;
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.getBankAccount( account, accountId );
                result = dbBankAccount;

                config.updateBankAccount( account, dbBankAccount );
            }
        };

        BankAccount result = endpoint.updateBankAccount( accountId, bankAccount, request, authUser );
        assertThat( result ).isNotNull();
    }

    @Test( expectedExceptions = NotFoundException.class )
    public void updateBankAccount_NotFound() throws Exception
    {
        long accountId = 229;
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.getBankAccount( account, accountId );
                result = new BankAccountNotFound( accountId );
            }
        };

        endpoint.updateBankAccount( accountId, bankAccount, request, authUser );
    }

    @Test( expectedExceptions = NotFoundException.class )
    public void updateBankAccount_WrongOwner() throws Exception
    {
        long accountId = 239;
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.getBankAccount( account, accountId );
                result = new WrongEntityOwner();
            }
        };

        endpoint.updateBankAccount( accountId, bankAccount, request, authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void updateBankAccount_BackendRetrievalError() throws Exception
    {
        long accountId = 279;
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.getBankAccount( account, accountId );
                result = new RuntimeException();
            }
        };

        endpoint.updateBankAccount( accountId, bankAccount, request, authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void updateBankAccount_BackendError() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.updateBankAccount( account, dbBankAccount );
                result = new RuntimeException();
            }
        };

        endpoint.updateBankAccount( 1L, bankAccount, request, authUser );
    }

    @Test( expectedExceptions = BadRequestException.class )
    public void updateBankAccount_ApiValidationFailure() throws Exception
    {
        long accountId = 269;
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                mapper.map( bankAccount, dbBankAccount, ( MappingContext ) any );
                result = new ApiValidationException( "Validation failure" );
            }
        };

        endpoint.updateBankAccount( accountId, bankAccount, request, authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void updateBankAccount_BackendMappingError() throws Exception
    {
        long accountId = 259;
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                mapper.map( dbBankAccount, BankAccount.class );
                result = new RuntimeException();
            }
        };

        endpoint.updateBankAccount( accountId, bankAccount, request, authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void updateBankAccount_BusinessFlowFailure() throws Exception
    {
        long accountId = 249;
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.updateBankAccount( account, dbBankAccount );
                result = new IllegalArgumentException();
            }
        };

        endpoint.updateBankAccount( accountId, bankAccount, request, authUser );
    }

    @Test
    public void deleteBankAccount() throws Exception
    {
        long accountId = 289;
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.deleteBankAccount( account, accountId );
            }
        };

        endpoint.deleteBankAccount( accountId, request, authUser );
    }

    @Test( expectedExceptions = NotFoundException.class )
    public void deleteBankAccount_NotFound() throws Exception
    {
        long accountId = 289;
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.deleteBankAccount( account, accountId );
                result = new BankAccountNotFound( accountId );
            }
        };

        endpoint.deleteBankAccount( accountId, request, authUser );
    }

    @Test( expectedExceptions = NotFoundException.class )
    public void deleteBankAccount_WrongOwner() throws Exception
    {
        long accountId = 289;
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.deleteBankAccount( account, accountId );
                result = new WrongEntityOwner();
            }
        };

        endpoint.deleteBankAccount( accountId, request, authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void deleteBankAccount_BackendError() throws Exception
    {
        long accountId = 389;
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.deleteBankAccount( account, accountId );
                result = new RuntimeException();
            }
        };

        endpoint.deleteBankAccount( accountId, request, authUser );
    }

    @Test( expectedExceptions = BadRequestException.class )
    public void deleteBankAccount_ValidationFailure() throws Exception
    {
        long accountId = 489;
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.deleteBankAccount( account, accountId );
                result = new ApiValidationException( "Validation failure" );
            }
        };

        endpoint.deleteBankAccount( accountId, request, authUser );
    }

    @Test
    public void getPrimaryBankAccount() throws Exception
    {
        String country = "SK";
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.getPrimaryBankAccount( account, country );
                result = dbBankAccount;
            }
        };

        assertThat( endpoint.getPrimaryBankAccount( country, request, authUser ) ).isNotNull();
    }

    @Test( expectedExceptions = NotFoundException.class )
    public void getPrimaryBankAccount_NotFound() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.getPrimaryBankAccount( account, anyString );
                result = new BankAccountNotFound( -1L );
            }
        };

        endpoint.getPrimaryBankAccount( "SK", request, authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void getPrimaryBankAccount_BackendError() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.getPrimaryBankAccount( account, anyString );
                result = new RuntimeException();
            }
        };

        endpoint.getPrimaryBankAccount( null, request, authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void getPrimaryBankAccount_MappingError() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                mapper.map( dbBankAccount, BankAccount.class );
                result = new RuntimeException();
            }
        };

        endpoint.getPrimaryBankAccount( null, request, authUser );
    }

    @Test
    public void markBankAccountAsPrimary() throws Exception
    {
        long accountId = 560L;
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.markBankAccountAsPrimary( account, accountId );
                result = dbBankAccount;
            }
        };

        assertThat( endpoint.markBankAccountAsPrimary( accountId, request, authUser ) ).isNotNull();
    }

    @Test( expectedExceptions = BadRequestException.class )
    public void markBankAccountAsPrimary_ValidationFailure() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.markBankAccountAsPrimary( account, anyLong );
                result = new ApiValidationException( "Validation failure" );
            }
        };

        endpoint.markBankAccountAsPrimary( 2L, request, authUser );
    }

    @Test( expectedExceptions = NotFoundException.class )
    public void markBankAccountAsPrimary_WrongOwner() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.markBankAccountAsPrimary( account, anyLong );
                result = new WrongEntityOwner();
            }
        };

        endpoint.markBankAccountAsPrimary( 2L, request, authUser );
    }

    @Test( expectedExceptions = NotFoundException.class )
    public void markBankAccountAsPrimary_NotFound() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.markBankAccountAsPrimary( account, anyLong );
                result = new BankAccountNotFound( 2L );
            }
        };

        endpoint.markBankAccountAsPrimary( 2L, request, authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void markBankAccountAsPrimary_BackendError() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.markBankAccountAsPrimary( account, anyLong );
                result = new RuntimeException();
            }
        };

        endpoint.markBankAccountAsPrimary( 2L, request, authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void markBankAccountAsPrimary_MappingError() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                mapper.map( dbBankAccount, BankAccount.class );
                result = new RuntimeException();
            }
        };

        endpoint.markBankAccountAsPrimary( 2L, request, authUser );
    }
}