package biz.turnonline.ecosystem.payment.api;

import biz.turnonline.ecosystem.account.client.model.Account;
import biz.turnonline.ecosystem.payment.api.model.BankAccount;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiReference;
import com.google.api.server.spi.config.DefaultValue;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import ma.glasnost.orika.MapperFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * {@link BankAccount} REST API Endpoint.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Api
@ApiReference( PaymentsApiProfile.class )
public class BankAccountEndpoint
{
    private static final Logger logger = LoggerFactory.getLogger( BankAccountEndpoint.class );

    private final EndpointsCommon common;

    private final MapperFacade mapper;

    @Inject
    BankAccountEndpoint( EndpointsCommon common,
                         MapperFacade mapper )
    {
        this.common = common;
        this.mapper = mapper;
    }

    @ApiMethod( name = "bank_accounts.insert", path = "bank-accounts", httpMethod = ApiMethod.HttpMethod.POST )
    public BankAccount insertBankAccount( BankAccount bankAccount,
                                          HttpServletRequest request,
                                          User authUser )
            throws Exception
    {
        Account account = common.checkAccount( authUser, request );
        return null;
    }

    @ApiMethod( name = "bank_accounts.list", path = "bank-accounts", httpMethod = ApiMethod.HttpMethod.GET )
    public List<BankAccount> searchBankAccounts( @DefaultValue( "0" ) @Nullable @Named( "offset" ) Integer offset,
                                                 @DefaultValue( "10" ) @Nullable @Named( "limit" ) Integer limit,
                                                 HttpServletRequest request,
                                                 User authUser )
            throws Exception
    {
        Account account = common.checkAccount( authUser, request );
        return null;
    }

    @ApiMethod( name = "bank_accounts.get",
            path = "bank-accounts/{account_id}",
            httpMethod = ApiMethod.HttpMethod.GET )
    public BankAccount getBankAccount( @Named( "account_id" ) Long accountId,
                                       HttpServletRequest request,
                                       User authUser )
            throws Exception
    {
        Account account = common.checkAccount( authUser, request );
        return null;
    }

    @ApiMethod( name = "bank_accounts.update",
            path = "bank-accounts/{account_id}",
            httpMethod = ApiMethod.HttpMethod.PUT )
    public BankAccount updateBankAccount( @Named( "account_id" ) Long accountId,
                                          BankAccount bankAccount,
                                          HttpServletRequest request,
                                          User authUser )
            throws Exception
    {
        Account account = common.checkAccount( authUser, request );
        return null;
    }

    @ApiMethod( name = "bank_accounts.delete",
            path = "bank-accounts/{account_id}",
            httpMethod = ApiMethod.HttpMethod.DELETE )
    public void deleteBankAccount( @Named( "account_id" ) Long accountId, HttpServletRequest request, User authUser )
            throws Exception
    {
        Account account = common.checkAccount( authUser, request );
    }
}
