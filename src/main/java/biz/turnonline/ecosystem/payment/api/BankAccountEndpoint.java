package biz.turnonline.ecosystem.payment.api;

import biz.turnonline.ecosystem.account.client.model.Account;
import biz.turnonline.ecosystem.payment.api.model.BankAccount;
import biz.turnonline.ecosystem.payment.service.ApiValidationException;
import biz.turnonline.ecosystem.payment.service.BankAccountNotFound;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.WrongEntityOwner;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiReference;
import com.google.api.server.spi.config.DefaultValue;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.common.base.MoreObjects;
import com.google.common.net.HttpHeaders;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MappingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static biz.turnonline.ecosystem.payment.api.EndpointsCommon.bankAccountNotFoundMessage;
import static biz.turnonline.ecosystem.payment.api.EndpointsCommon.primaryBankAccountNotFoundMessage;
import static biz.turnonline.ecosystem.payment.api.EndpointsCommon.tryAgainLaterMessage;

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

    private final PaymentConfig config;

    @Inject
    BankAccountEndpoint( EndpointsCommon common,
                         MapperFacade mapper,
                         PaymentConfig config )
    {
        this.common = common;
        this.mapper = mapper;
        this.config = config;
    }

    @ApiMethod( name = "bank_accounts.insert", path = "bank-accounts", httpMethod = ApiMethod.HttpMethod.POST )
    public BankAccount insertBankAccount( BankAccount bankAccount,
                                          HttpServletRequest request,
                                          User authUser )
            throws Exception
    {
        Account account = common.checkAccount( authUser, request );

        BankAccount result;
        try
        {
            MappingContext context = new MappingContext( new HashMap<>() );
            context.setProperty( Account.class, account );

            biz.turnonline.ecosystem.payment.service.model.BankAccount dbBankAccount;
            dbBankAccount = mapper.map( bankAccount, biz.turnonline.ecosystem.payment.service.model.BankAccount.class,
                    context );

            config.insertBankAccount( account, dbBankAccount );
            result = mapper.map( dbBankAccount, BankAccount.class );
        }
        catch ( ApiValidationException e )
        {
            logger.warn( "BankAccount validation has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getEmail() )
                    .add( "bankAccount", bankAccount )
                    .toString(), e );
            throw new BadRequestException( e.getMessage() );
        }
        catch ( IllegalArgumentException e )
        {
            logger.error( "BankAccount business flow has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getEmail() )
                    .add( "bankAccount", bankAccount )
                    .toString(), e );
            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }
        catch ( Exception e )
        {
            logger.error( "BankAccount creation has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getEmail() )
                    .add( "bankAccount", bankAccount )
                    .toString(), e );
            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }

        return result;
    }

    @ApiMethod( name = "bank_accounts.list", path = "bank-accounts", httpMethod = ApiMethod.HttpMethod.GET )
    public List<BankAccount> searchBankAccounts( @DefaultValue( "0" ) @Nullable @Named( "offset" ) Integer offset,
                                                 @DefaultValue( "10" ) @Nullable @Named( "limit" ) Integer limit,
                                                 @Nullable @Named( "country" ) String country,
                                                 @DefaultValue( "false" ) @Nullable @Named( "alternative" ) Boolean alternative,
                                                 HttpServletRequest request,
                                                 User authUser )
            throws Exception
    {
        Account account = common.checkAccount( authUser, request );
        Locale language = common.getAcceptLanguage( request );
        List<BankAccount> result;

        try
        {
            List<biz.turnonline.ecosystem.payment.service.model.BankAccount> bankAccounts;
            if ( alternative )
            {
                biz.turnonline.ecosystem.payment.service.model.BankAccount primary;
                primary = config.getPrimaryBankAccount( account, country );
                bankAccounts = config.getAlternativeBankAccounts( account, primary, offset, limit, language, country );
            }
            else
            {
                bankAccounts = config.getBankAccounts( account, offset, limit, country );
            }

            MappingContext context = new MappingContext( new HashMap<>() );
            context.setProperty( HttpHeaders.ACCEPT_LANGUAGE, language );
            result = mapper.mapAsList( bankAccounts, BankAccount.class, context );
        }
        catch ( Exception e )
        {
            logger.error( "BankAccount list retrieval has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getEmail() )
                    .add( "offset", offset )
                    .add( "limit", limit )
                    .add( "country", country )
                    .add( "alternative", alternative )
                    .toString(), e );

            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }

        return result;
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
        BankAccount result;

        try
        {
            biz.turnonline.ecosystem.payment.service.model.BankAccount bankAccount;
            bankAccount = config.getBankAccount( account, accountId );
            result = mapper.map( bankAccount, BankAccount.class );
        }
        catch ( WrongEntityOwner e )
        {
            throw new NotFoundException( bankAccountNotFoundMessage( accountId ) );
        }
        catch ( BankAccountNotFound e )
        {
            throw new NotFoundException( bankAccountNotFoundMessage( e.getBankAccountId() ) );
        }
        catch ( Exception e )
        {
            logger.error( "BankAccount retrieval has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getEmail() )
                    .add( "account_id", accountId )
                    .toString(), e );

            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }

        return result;
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
        BankAccount result;

        try
        {
            biz.turnonline.ecosystem.payment.service.model.BankAccount dbBankAccount;
            dbBankAccount = config.getBankAccount( account, accountId );

            MappingContext context = new MappingContext( new HashMap<>() );
            context.setProperty( Account.class, account );
            mapper.map( bankAccount, dbBankAccount, context );

            config.updateBankAccount( account, dbBankAccount );
            result = mapper.map( dbBankAccount, BankAccount.class );
        }
        catch ( ApiValidationException e )
        {
            logger.warn( "BankAccount validation has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getEmail() )
                    .add( "account_id", accountId )
                    .add( "bankAccount", bankAccount )
                    .toString(), e );

            throw new BadRequestException( e.getMessage() );
        }
        catch ( WrongEntityOwner e )
        {
            throw new NotFoundException( bankAccountNotFoundMessage( accountId ) );
        }
        catch ( BankAccountNotFound e )
        {
            throw new NotFoundException( bankAccountNotFoundMessage( e.getBankAccountId() ) );
        }
        catch ( IllegalArgumentException e )
        {
            logger.error( "BankAccount business flow has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getEmail() )
                    .add( "account_id", accountId )
                    .add( "bankAccount", bankAccount )
                    .toString(), e );

            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }
        catch ( Exception e )
        {
            logger.error( "BankAccount update has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getEmail() )
                    .add( "account_id", accountId )
                    .add( "bankAccount", bankAccount )
                    .toString(), e );

            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }

        return result;
    }

    @ApiMethod( name = "bank_accounts.delete",
            path = "bank-accounts/{account_id}",
            httpMethod = ApiMethod.HttpMethod.DELETE )
    public void deleteBankAccount( @Named( "account_id" ) Long accountId, HttpServletRequest request, User authUser )
            throws Exception
    {
        Account account = common.checkAccount( authUser, request );

        try
        {
            config.deleteBankAccount( account, accountId );
        }
        catch ( ApiValidationException e )
        {
            logger.warn( "BankAccount validation has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getEmail() )
                    .add( "account_id", accountId )
                    .toString(), e );

            throw new BadRequestException( e.getMessage() );
        }
        catch ( WrongEntityOwner e )
        {
            throw new NotFoundException( bankAccountNotFoundMessage( accountId ) );
        }
        catch ( BankAccountNotFound e )
        {
            throw new NotFoundException( bankAccountNotFoundMessage( e.getBankAccountId() ) );
        }
        catch ( Exception e )
        {
            logger.error( "BankAccount deletion has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getEmail() )
                    .add( "account_id", accountId )
                    .toString(), e );

            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }
    }

    @ApiMethod( name = "bank_accounts.primary.get",
            path = "bank-accounts/primary",
            httpMethod = ApiMethod.HttpMethod.GET )
    public BankAccount getPrimaryBankAccount( @Nullable @Named( "country" ) String country,
                                              HttpServletRequest request,
                                              User authUser )
            throws Exception
    {
        Account account = common.checkAccount( authUser, request );
        BankAccount result;

        try
        {
            biz.turnonline.ecosystem.payment.service.model.BankAccount primary;
            primary = config.getPrimaryBankAccount( account, country );
            result = mapper.map( primary, BankAccount.class );
        }
        catch ( BankAccountNotFound e )
        {
            throw new NotFoundException( primaryBankAccountNotFoundMessage() );
        }
        catch ( Exception e )
        {
            logger.error( "Primary bank account retrieval has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getEmail() )
                    .add( "country", country )
                    .toString(), e );

            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }

        return result;
    }

    @ApiMethod( name = "bank_accounts.primary.update",
            path = "bank-accounts/{account_id}/primary",
            httpMethod = ApiMethod.HttpMethod.PUT )
    public BankAccount markBankAccountAsPrimary( @Named( "account_id" ) Long accountId,
                                                 HttpServletRequest request,
                                                 User authUser )
            throws Exception
    {
        Account account = common.checkAccount( authUser, request );
        BankAccount result;

        try
        {
            biz.turnonline.ecosystem.payment.service.model.BankAccount primary;
            primary = config.markBankAccountAsPrimary( account, accountId );
            result = mapper.map( primary, BankAccount.class );
        }
        catch ( ApiValidationException e )
        {
            logger.warn( "Primary bank account validation has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getEmail() )
                    .add( "account_id", accountId )
                    .toString(), e );

            throw new BadRequestException( e.getMessage() );
        }
        catch ( WrongEntityOwner e )
        {
            throw new NotFoundException( bankAccountNotFoundMessage( accountId ) );
        }
        catch ( BankAccountNotFound e )
        {
            throw new NotFoundException( bankAccountNotFoundMessage( e.getBankAccountId() ) );
        }
        catch ( Exception e )
        {
            logger.error( "Update of the bank account to be marked as primary has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getEmail() )
                    .add( "account_id", accountId )
                    .toString(), e );

            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }

        return result;
    }
}
