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
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.FormOfPayment;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.steward.model.Account;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.common.net.HttpHeaders;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MappingContext;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static biz.turnonline.ecosystem.payment.service.PaymentConfig.REVOLUT_BANK_CODE;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * {@link BankAccountEndpoint} unit testing, mainly negative scenarios.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@SuppressWarnings( {"unchecked", "ConstantConditions"} )
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

    private LocalAccount account;

    @Mocked
    private BankAccount bankAccount;

    @Mocked
    private CompanyBankAccount dbBankAccount;

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
    public void insertBankAccount() throws Exception
    {
        Locale locale = Locale.ENGLISH;
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                dbBankAccount.save();

                common.getAcceptLanguage( request );
                result = locale;

                mapper.map( bankAccount, CompanyBankAccount.class, ( MappingContext ) any );
            }
        };

        BankAccount result = endpoint.insertBankAccount( bankAccount, request, authUser );
        assertThat( result ).isNotNull();

        new Verifications()
        {
            {
                MappingContext context;
                mapper.map( dbBankAccount, BankAccount.class, context = withCapture() );

                assertWithMessage( "Mapping context Backend to API" )
                        .that( context )
                        .isNotNull();

                assertWithMessage( "Mapping context ACCEPT_LANGUAGE property" )
                        .that( context.getProperty( HttpHeaders.ACCEPT_LANGUAGE ) )
                        .isEqualTo( locale );

                assertWithMessage( "Mapping context local account property" )
                        .that( context.getProperty( LocalAccount.class ) )
                        .isEqualTo( account );
            }
        };
    }

    @Test( expectedExceptions = BadRequestException.class )
    public void insertBankAccount_ApiValidationFailure() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                mapper.map( bankAccount, CompanyBankAccount.class, ( MappingContext ) any );
                result = new ApiValidationException( "Validation failure" );
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

                dbBankAccount.save();
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

                mapper.map( dbBankAccount, BankAccount.class, ( MappingContext ) any );
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
        Locale locale = Locale.ENGLISH;

        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.getBankAccounts( 5, 15, null, null );

                mapper.mapAsList( ( List<CompanyBankAccount> ) any, BankAccount.class, ( MappingContext ) any );
                result = bankAccounts;

                common.getAcceptLanguage( request );
                result = locale;
            }
        };

        List<BankAccount> result = endpoint.searchBankAccounts( 5, 15, null, null, false, request, authUser );
        assertThat( result ).isNotNull();
        assertThat( result ).hasSize( 1 );

        new Verifications()
        {
            {
                MappingContext context;
                mapper.mapAsList( ( List<CompanyBankAccount> ) any, BankAccount.class, context = withCapture() );

                assertWithMessage( "Mapping context Backend to API" )
                        .that( context )
                        .isNotNull();

                assertWithMessage( "Mapping context ACCEPT_LANGUAGE property" )
                        .that( context.getProperty( HttpHeaders.ACCEPT_LANGUAGE ) )
                        .isEqualTo( locale );

                assertWithMessage( "Mapping context local account property" )
                        .that( context.getProperty( LocalAccount.class ) )
                        .isEqualTo( account );
            }
        };
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void getBankAccounts_BackendError() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.getBankAccounts( anyInt, anyInt, null, null );
                result = new RuntimeException();
            }
        };

        endpoint.searchBankAccounts( 5, 15, null, null, false, request, authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void getBankAccounts_BackendMappingError() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                mapper.mapAsList( ( List<CompanyBankAccount> ) any, BankAccount.class, ( MappingContext ) any );
                result = new RuntimeException();
            }
        };

        endpoint.searchBankAccounts( 0, null, null, null, false, request, authUser );
    }

    @Test
    public void getBankAccount() throws Exception
    {
        long accountId = 199L;
        Locale locale = Locale.ENGLISH;

        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.getBankAccount( accountId );
                result = dbBankAccount;

                mapper.map( dbBankAccount, BankAccount.class, ( MappingContext ) any );
                result = bankAccount;

                common.getAcceptLanguage( request );
                result = locale;
            }
        };

        BankAccount result = endpoint.getBankAccount( accountId, request, authUser );
        assertThat( result ).isNotNull();

        new Verifications()
        {
            {
                MappingContext context;
                mapper.map( dbBankAccount, BankAccount.class, context = withCapture() );

                assertWithMessage( "Mapping context Backend to API" )
                        .that( context )
                        .isNotNull();

                assertWithMessage( "Mapping context ACCEPT_LANGUAGE property" )
                        .that( context.getProperty( HttpHeaders.ACCEPT_LANGUAGE ) )
                        .isEqualTo( locale );

                assertWithMessage( "Mapping context local account property" )
                        .that( context.getProperty( LocalAccount.class ) )
                        .isEqualTo( account );
            }
        };
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

                config.getBankAccount( accountId );
                result = new BankAccountNotFound( accountId );
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

                config.getBankAccount( anyLong );
                result = new RuntimeException();
            }
        };

        endpoint.getBankAccount( 67L, request, authUser );
    }

    @Test
    public void updateBankAccount() throws Exception
    {
        long accountId = 219;
        Locale locale = Locale.ENGLISH;

        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.getBankAccount( accountId );
                result = dbBankAccount;

                dbBankAccount.save();

                common.getAcceptLanguage( request );
                result = locale;
            }
        };

        BankAccount result = endpoint.updateBankAccount( accountId, bankAccount, request, authUser );
        assertThat( result ).isNotNull();

        new Verifications()
        {
            {
                MappingContext context;
                mapper.map( dbBankAccount, BankAccount.class, context = withCapture() );

                assertWithMessage( "Mapping context Backend to API" )
                        .that( context )
                        .isNotNull();

                assertWithMessage( "Mapping context ACCEPT_LANGUAGE property" )
                        .that( context.getProperty( HttpHeaders.ACCEPT_LANGUAGE ) )
                        .isEqualTo( locale );

                assertWithMessage( "Mapping context local account property" )
                        .that( context.getProperty( LocalAccount.class ) )
                        .isEqualTo( account );
            }
        };
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

                config.getBankAccount( accountId );
                result = new BankAccountNotFound( accountId );
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

                config.getBankAccount( accountId );
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

                dbBankAccount.save();
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

                mapper.map( dbBankAccount, BankAccount.class, ( MappingContext ) any );
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

                dbBankAccount.save();
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

                config.deleteBankAccount( accountId );
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

                config.deleteBankAccount( accountId );
                result = new BankAccountNotFound( accountId );
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

                config.deleteBankAccount( accountId );
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

                config.deleteBankAccount( accountId );
                result = new ApiValidationException( "Validation failure" );
            }
        };

        endpoint.deleteBankAccount( accountId, request, authUser );
    }

    @Test
    public void getPrimaryBankAccount() throws Exception
    {
        String country = "SK";
        Locale locale = Locale.ENGLISH;

        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.getPrimaryBankAccount( country );
                result = dbBankAccount;

                common.getAcceptLanguage( request );
                result = locale;
            }
        };

        assertThat( endpoint.getPrimaryBankAccount( country, request, authUser ) ).isNotNull();

        new Verifications()
        {
            {
                MappingContext context;
                mapper.map( dbBankAccount, BankAccount.class, context = withCapture() );

                assertWithMessage( "Mapping context Backend to API" )
                        .that( context )
                        .isNotNull();

                assertWithMessage( "Mapping context ACCEPT_LANGUAGE property" )
                        .that( context.getProperty( HttpHeaders.ACCEPT_LANGUAGE ) )
                        .isEqualTo( locale );

                assertWithMessage( "Mapping context local account property" )
                        .that( context.getProperty( LocalAccount.class ) )
                        .isEqualTo( account );
            }
        };
    }

    @Test( expectedExceptions = NotFoundException.class )
    public void getPrimaryBankAccount_NotFound() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.getPrimaryBankAccount( anyString );
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

                config.getPrimaryBankAccount( anyString );
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

                mapper.map( dbBankAccount, BankAccount.class, ( MappingContext ) any );
                result = new RuntimeException();
            }
        };

        endpoint.getPrimaryBankAccount( null, request, authUser );
    }

    @Test
    public void markBankAccountAsPrimary() throws Exception
    {
        long accountId = 560L;
        Locale locale = Locale.ENGLISH;

        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.markBankAccountAsPrimary( accountId );
                result = dbBankAccount;

                common.getAcceptLanguage( request );
                result = locale;
            }
        };

        assertThat( endpoint.markBankAccountAsPrimary( accountId, request, authUser ) ).isNotNull();

        new Verifications()
        {
            {
                MappingContext context;
                mapper.map( dbBankAccount, BankAccount.class, context = withCapture() );

                assertWithMessage( "Mapping context Backend to API" )
                        .that( context )
                        .isNotNull();

                assertWithMessage( "Mapping context ACCEPT_LANGUAGE property" )
                        .that( context.getProperty( HttpHeaders.ACCEPT_LANGUAGE ) )
                        .isEqualTo( locale );

                assertWithMessage( "Mapping context local account property" )
                        .that( context.getProperty( LocalAccount.class ) )
                        .isEqualTo( account );
            }
        };
    }

    @Test( expectedExceptions = BadRequestException.class )
    public void markBankAccountAsPrimary_ValidationFailure() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.markBankAccountAsPrimary( anyLong );
                result = new ApiValidationException( "Validation failure" );
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

                config.markBankAccountAsPrimary( anyLong );
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

                config.markBankAccountAsPrimary( anyLong );
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

                mapper.map( dbBankAccount, BankAccount.class, ( MappingContext ) any );
                result = new RuntimeException();
            }
        };

        endpoint.markBankAccountAsPrimary( 2L, request, authUser );
    }

    @Test
    public void enableApiAccess_Revolut() throws Exception
    {
        Certificate certificate = new Certificate();
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.enableApiAccess( account, REVOLUT_BANK_CODE, certificate );
            }
        };

        endpoint.enableApiAccess( REVOLUT_BANK_CODE, certificate, request, authUser );
    }

    @Test( expectedExceptions = NotFoundException.class )
    public void enableApiAccess_BankCodeNotFound() throws Exception
    {
        Certificate certificate = new Certificate();
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.enableApiAccess( account, anyString, certificate );
                result = new BankCodeNotFound( "blacode" );
            }
        };

        endpoint.enableApiAccess( "blacode", certificate, request, authUser );
    }

    @Test( expectedExceptions = BadRequestException.class )
    public void enableApiAccess_UnsupportedBank() throws Exception
    {
        Certificate certificate = new Certificate();
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.enableApiAccess( account, anyString, certificate );
                result = new ApiValidationException( "Onboarding unsupported" );
            }
        };

        endpoint.enableApiAccess( "1100", certificate, request, authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void enableApiAccess_BackendError() throws Exception
    {
        Certificate certificate = new Certificate();
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.enableApiAccess( account, anyString, certificate );
                result = new RuntimeException();
            }
        };

        endpoint.enableApiAccess( REVOLUT_BANK_CODE, certificate, request, authUser );
    }

    @Test
    public void filterTransactions() throws Exception
    {
        List<Transaction> transactions = new ArrayList<>();
        transactions.add( new Transaction() );

        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.filterTransactions( ( PaymentConfig.Filter ) any );

                mapper.mapAsList( ( List<CommonTransaction> ) any, Transaction.class );
                result = transactions;
            }
        };

        int offset = 5;
        int limit = 15;
        long accountId = 246753L;
        Long invoiceId = 635442412L;
        Long orderId = 975467243L;
        String type = FormOfPayment.TRANSFER.name();
        String credit = "credit";

        List<Transaction> result = endpoint.filterTransactions( offset,
                limit,
                credit,
                accountId,
                invoiceId,
                orderId,
                type,
                request,
                authUser );

        assertThat( result ).isNotNull();
        assertThat( result ).hasSize( 1 );

        new Verifications()
        {
            {
                PaymentConfig.Filter filter;
                config.filterTransactions( filter = withCapture() );

                assertWithMessage( "Transaction filter" )
                        .that( filter )
                        .isNotNull();

                assertWithMessage( "Transaction filter offset" )
                        .that( filter.getOffset() )
                        .isEqualTo( offset );

                assertWithMessage( "Transaction filter limit" )
                        .that( filter.getLimit() )
                        .isEqualTo( limit );

                assertWithMessage( "Transaction filter account Id" )
                        .that( filter.getAccountId() )
                        .isEqualTo( accountId );

                assertWithMessage( "Transaction filter credit" )
                        .that( filter.getOperation() )
                        .isEqualTo( credit );

                assertWithMessage( "Transaction filter invoice Id" )
                        .that( filter.getInvoiceId() )
                        .isEqualTo( invoiceId );

                assertWithMessage( "Transaction filter order Id" )
                        .that( filter.getOrderId() )
                        .isEqualTo( orderId );

                assertWithMessage( "Transaction filter type" )
                        .that( filter.getType() )
                        .isEqualTo( type );
            }
        };
    }

    @Test( expectedExceptions = BadRequestException.class )
    public void filterTransactions_ApiValidationFailure() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.filterTransactions( ( PaymentConfig.Filter ) any );
                result = new ApiValidationException( "Validation failure" );
            }
        };

        endpoint.filterTransactions( 0,
                5,
                null,
                null,
                null,
                null,
                null,
                request,
                authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void filterTransactions_BackendServiceError() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                config.filterTransactions( ( PaymentConfig.Filter ) any );
                result = new RuntimeException( "Backend service error" );
            }
        };

        endpoint.filterTransactions( 0,
                5,
                null,
                null,
                null,
                null,
                null,
                request,
                authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void filterTransactions_BackendMappingError() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                mapper.mapAsList( ( List<CommonTransaction> ) any, Transaction.class );
                result = new RuntimeException( "Mapping failure" );
            }
        };

        endpoint.filterTransactions( 0,
                5,
                null,
                null,
                null,
                null,
                null,
                request,
                authUser );
    }
}