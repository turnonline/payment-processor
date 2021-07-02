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

package biz.turnonline.ecosystem.payment.api;

import biz.turnonline.ecosystem.payment.api.model.BankAccount;
import biz.turnonline.ecosystem.payment.api.model.Certificate;
import biz.turnonline.ecosystem.payment.api.model.Transaction;
import biz.turnonline.ecosystem.payment.service.BankAccountNotFound;
import biz.turnonline.ecosystem.payment.service.BankCodeNotFound;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.TransactionNotFound;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static biz.turnonline.ecosystem.payment.api.EndpointsCommon.bankAccountNotFoundMessage;
import static biz.turnonline.ecosystem.payment.api.EndpointsCommon.bankCodeNotFoundMessage;
import static biz.turnonline.ecosystem.payment.api.EndpointsCommon.primaryBankAccountNotFoundMessage;
import static biz.turnonline.ecosystem.payment.api.EndpointsCommon.transactionNotFoundMessage;
import static biz.turnonline.ecosystem.payment.api.EndpointsCommon.tryAgainLaterMessage;

/**
 * {@link BankAccount} REST API Endpoint.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Api
@ApiReference( EndpointsApiProfile.class )
public class BankAccountEndpoint
{
    private static final Logger LOGGER = LoggerFactory.getLogger( BankAccountEndpoint.class );

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
        LocalAccount account = common.checkAccount( authUser, request );
        Locale language = common.getAcceptLanguage( request );

        BankAccount result;
        try
        {
            MappingContext context = new MappingContext( new HashMap<>() );
            context.setProperty( HttpHeaders.ACCEPT_LANGUAGE, language );
            context.setProperty( LocalAccount.class, account );

            CompanyBankAccount dbBankAccount;
            dbBankAccount = mapper.map( bankAccount, CompanyBankAccount.class, context );
            dbBankAccount.save();

            result = mapper.map( dbBankAccount, BankAccount.class, context );
        }
        catch ( ApiValidationException e )
        {
            LOGGER.warn( "BankAccount validation has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
                    .add( "bankAccount", bankAccount )
                    .toString(), e );
            throw new BadRequestException( e.getMessage() );
        }
        catch ( Exception e )
        {
            LOGGER.error( "BankAccount creation has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
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
                                                 @Nullable @Named( "bank" ) String bankCode,
                                                 @DefaultValue( "false" ) @Nullable @Named( "alternative" ) Boolean alternative,
                                                 HttpServletRequest request,
                                                 User authUser )
            throws Exception
    {
        LocalAccount account = common.checkAccount( authUser, request );
        Locale language = common.getAcceptLanguage( request );
        List<BankAccount> result;

        try
        {
            List<CompanyBankAccount> bankAccounts;
            if ( alternative )
            {
                bankAccounts = config.getAlternativeBankAccounts( offset, limit, language, country );
            }
            else
            {
                bankAccounts = config.getBankAccounts( offset, limit, country, bankCode );
            }

            MappingContext context = new MappingContext( new HashMap<>() );
            context.setProperty( HttpHeaders.ACCEPT_LANGUAGE, language );
            context.setProperty( LocalAccount.class, account );
            result = mapper.mapAsList( bankAccounts, BankAccount.class, context );
        }
        catch ( Exception e )
        {
            LOGGER.error( "BankAccount list retrieval has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
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
        LocalAccount account = common.checkAccount( authUser, request );
        Locale language = common.getAcceptLanguage( request );
        BankAccount result;

        try
        {
            CompanyBankAccount bankAccount;
            bankAccount = config.getBankAccount( accountId );

            MappingContext context = new MappingContext( new HashMap<>() );
            context.setProperty( HttpHeaders.ACCEPT_LANGUAGE, language );
            context.setProperty( LocalAccount.class, account );
            result = mapper.map( bankAccount, BankAccount.class, context );
        }
        catch ( BankAccountNotFound e )
        {
            throw new NotFoundException( bankAccountNotFoundMessage( e.getBankAccountId() ) );
        }
        catch ( Exception e )
        {
            LOGGER.error( "BankAccount retrieval has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
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
        LocalAccount account = common.checkAccount( authUser, request );
        Locale language = common.getAcceptLanguage( request );
        BankAccount result;

        try
        {
            CompanyBankAccount dbBankAccount;
            dbBankAccount = config.getBankAccount( accountId );

            MappingContext context = new MappingContext( new HashMap<>() );
            context.setProperty( HttpHeaders.ACCEPT_LANGUAGE, language );
            context.setProperty( LocalAccount.class, account );
            mapper.map( bankAccount, dbBankAccount, context );

            dbBankAccount.save();
            result = mapper.map( dbBankAccount, BankAccount.class, context );
        }
        catch ( ApiValidationException e )
        {
            LOGGER.warn( "BankAccount validation has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
                    .add( "account_id", accountId )
                    .add( "bankAccount", bankAccount )
                    .toString(), e );

            throw new BadRequestException( e.getMessage() );
        }
        catch ( BankAccountNotFound e )
        {
            throw new NotFoundException( bankAccountNotFoundMessage( e.getBankAccountId() ) );
        }
        catch ( IllegalArgumentException e )
        {
            LOGGER.error( "BankAccount business flow has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
                    .add( "account_id", accountId )
                    .add( "bankAccount", bankAccount )
                    .toString(), e );

            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }
        catch ( Exception e )
        {
            LOGGER.error( "BankAccount update has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
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
        LocalAccount account = common.checkAccount( authUser, request );

        try
        {
            config.deleteBankAccount( accountId );
        }
        catch ( ApiValidationException e )
        {
            LOGGER.warn( "BankAccount validation has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
                    .add( "account_id", accountId )
                    .toString(), e );

            throw new BadRequestException( e.getMessage() );
        }
        catch ( BankAccountNotFound e )
        {
            throw new NotFoundException( bankAccountNotFoundMessage( e.getBankAccountId() ) );
        }
        catch ( Exception e )
        {
            LOGGER.error( "BankAccount deletion has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
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
        LocalAccount account = common.checkAccount( authUser, request );
        Locale language = common.getAcceptLanguage( request );
        BankAccount result;

        try
        {
            CompanyBankAccount primary;
            primary = config.getPrimaryBankAccount( country );

            MappingContext context = new MappingContext( new HashMap<>() );
            context.setProperty( HttpHeaders.ACCEPT_LANGUAGE, language );
            context.setProperty( LocalAccount.class, account );
            result = mapper.map( primary, BankAccount.class, context );
        }
        catch ( BankAccountNotFound e )
        {
            throw new NotFoundException( primaryBankAccountNotFoundMessage() );
        }
        catch ( Exception e )
        {
            LOGGER.error( "Primary bank account retrieval has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
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
        LocalAccount account = common.checkAccount( authUser, request );
        Locale language = common.getAcceptLanguage( request );
        BankAccount result;

        try
        {
            CompanyBankAccount primary;
            primary = config.markBankAccountAsPrimary( accountId );

            MappingContext context = new MappingContext( new HashMap<>() );
            context.setProperty( HttpHeaders.ACCEPT_LANGUAGE, language );
            context.setProperty( LocalAccount.class, account );
            result = mapper.map( primary, BankAccount.class, context );
        }
        catch ( ApiValidationException e )
        {
            LOGGER.warn( "Primary bank account validation has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
                    .add( "account_id", accountId )
                    .toString(), e );

            throw new BadRequestException( e.getMessage() );
        }
        catch ( BankAccountNotFound e )
        {
            throw new NotFoundException( bankAccountNotFoundMessage( e.getBankAccountId() ) );
        }
        catch ( Exception e )
        {
            LOGGER.error( "Update of the bank account to be marked as primary has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
                    .add( "account_id", accountId )
                    .toString(), e );

            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }

        return result;
    }

    @ApiMethod( name = "bank_accounts.certificates",
            path = "bank-accounts/{bank_code}/certificates/actual",
            httpMethod = ApiMethod.HttpMethod.PUT )
    public Certificate enableApiAccess( @Named( "bank_code" ) String bankCode,
                                        Certificate certificate,
                                        HttpServletRequest request,
                                        User authUser )
            throws Exception
    {
        LocalAccount account = common.checkAccount( authUser, request );
        Certificate result;

        try
        {
            result = config.enableApiAccess( account, bankCode, certificate );
        }
        catch ( BankCodeNotFound e )
        {
            LOGGER.warn( "Bank code not found: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
                    .add( "bank_code", bankCode )
                    .addValue( certificate )
                    .toString(), e );

            throw new NotFoundException( bankCodeNotFoundMessage( e.getBankCode() ) );
        }
        catch ( ApiValidationException e )
        {
            LOGGER.warn( "Bank onboard has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
                    .add( "bank_code", bankCode )
                    .addValue( certificate )
                    .toString(), e );

            throw new BadRequestException( e.getMessage() );
        }
        catch ( Exception e )
        {
            LOGGER.error( "Bank onboard has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
                    .add( "bank_code", bankCode )
                    .addValue( certificate )
                    .toString(), e );

            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }

        return result;
    }

    @ApiMethod( name = "transactions.list", path = "transactions", httpMethod = ApiMethod.HttpMethod.GET )
    public List<Transaction> filterTransactions( @DefaultValue( "0" ) @Nullable @Named( "offset" ) Integer offset,
                                                 @DefaultValue( "20" ) @Nullable @Named( "limit" ) Integer limit,
                                                 @DefaultValue( "both" ) @Nullable @Named( "operation" ) String operation,
                                                 @Nullable @Named( "accountId" ) Long accountId,
                                                 @Nullable @Named( "invoiceId" ) Long invoiceId,
                                                 @Nullable @Named( "orderId" ) Long orderId,
                                                 @Nullable @Named( "type" ) String type,
                                                 @Nullable @Named( "status" ) String status,
                                                 @Nullable @Named( "from" ) Date createdDateFrom,
                                                 @Nullable @Named( "to" ) Date createdDateTo,
                                                 HttpServletRequest request,
                                                 User authUser )
            throws Exception
    {
        LocalAccount account = common.checkAccount( authUser, request );
        List<Transaction> result;

        // fix 'create date from' time to 00:00:00
        if ( createdDateFrom != null )
        {
            LocalDateTime startOfDay = createdDateFrom.toInstant().atZone( account.getZoneId() ).toLocalDate().atStartOfDay();
            createdDateFrom = Date.from( startOfDay.atZone( account.getZoneId() ).toInstant() );
        }
        // fix 'create date to' time to 23:59:59
        if ( createdDateTo != null )
        {
            LocalDateTime endOfDay = createdDateTo.toInstant().atZone( account.getZoneId() ).toLocalDate().atTime( LocalTime.MAX );
            createdDateTo = Date.from( endOfDay.atZone( account.getZoneId() ).toInstant() );
        }

        try
        {
            PaymentConfig.Filter filter = new PaymentConfig.Filter()
                    .offset( offset )
                    .limit( limit )
                    .accountId( accountId )
                    .operation( operation )
                    .invoiceId( invoiceId )
                    .orderId( orderId )
                    .type( type )
                    .status( status )
                    .createdDateFrom( createdDateFrom )
                    .createdDateTo( createdDateTo );

            List<CommonTransaction> transactions;
            transactions = config.filterTransactions( filter );

            result = mapper.mapAsList( transactions, Transaction.class );
        }
        catch ( ApiValidationException e )
        {
            LOGGER.warn( "Transaction query params are invalid: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
                    .add( "Bank account ID", accountId )
                    .add( "operation", operation )
                    .add( "invoiceId", invoiceId )
                    .add( "orderId", orderId )
                    .add( "type", type )
                    .add( "offset", offset )
                    .add( "limit", limit )
                    .toString(), e );

            throw new BadRequestException( e.getMessage() );
        }
        catch ( Exception e )
        {
            LOGGER.error( "Transaction list retrieval has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
                    .add( "Bank account ID", accountId )
                    .add( "credit", operation )
                    .add( "invoiceId", invoiceId )
                    .add( "orderId", orderId )
                    .add( "type", type )
                    .add( "offset", offset )
                    .add( "limit", limit )
                    .toString(), e );

            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }

        return result;
    }

    @ApiMethod( name = "transactions.get",
            path = "transactions/{transaction_id}",
            httpMethod = ApiMethod.HttpMethod.GET )
    public Transaction getTransaction( @Named( "transaction_id" ) Long transactionId,
                                       HttpServletRequest request,
                                       User authUser )
            throws Exception
    {
        LocalAccount account = common.checkAccount( authUser, request );
        Transaction result;

        try
        {
            CommonTransaction transaction = config.getTransaction( transactionId );
            result = mapper.map( transaction, Transaction.class );
        }
        catch ( TransactionNotFound e )
        {
            LOGGER.warn( "Transaction not found: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
                    .add( "Transaction ID", transactionId )
                    .toString(), e );

            throw new NotFoundException( transactionNotFoundMessage( transactionId ) );
        }
        catch ( Exception e )
        {
            LOGGER.error( "Transaction retrieval has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
                    .add( "Transaction ID", transactionId )
                    .toString(), e );

            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }

        return result;
    }
}
