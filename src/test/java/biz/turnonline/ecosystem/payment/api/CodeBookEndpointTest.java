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

import biz.turnonline.ecosystem.payment.api.model.BankCode;
import biz.turnonline.ecosystem.payment.service.CodeBook;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.steward.model.Account;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.NotFoundException;
import ma.glasnost.orika.MapperFacade;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;

import static com.google.common.truth.Truth.assertThat;

/**
 * {@link CodeBookEndpoint} unit testing, mainly negative scenarios.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class CodeBookEndpointTest
{
    @Tested
    private CodeBookEndpoint endpoint;

    @Injectable
    private EndpointsCommon common;

    @Injectable
    private MapperFacade mapper;

    @Injectable
    private CodeBook service;

    @Mocked
    private HttpServletRequest request;

    @Mocked
    private User authUser;

    private LocalAccount account;

    @Mocked
    private biz.turnonline.ecosystem.payment.service.model.BankCode dbBankCode;

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
    public void listCodebookBankCodes() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                common.getAcceptLanguage( request );

                service.getBankCodes( account, ( Locale ) any, "SK" );
                //noinspection unchecked,ConstantConditions
                mapper.mapAsList( ( List<biz.turnonline.ecosystem.payment.service.model.BankCode> ) any, BankCode.class );
            }
        };

        List<BankCode> numberSeries = endpoint.listCodebookBankCodes( "SK", request, authUser );
        assertThat( numberSeries ).isNotNull();
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void listCodebookBankCodes_ServerError() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                //noinspection ConstantConditions
                service.getBankCodes( ( LocalAccount ) any, ( Locale ) any, anyString );
                result = new RuntimeException();
            }
        };

        endpoint.listCodebookBankCodes( null, request, authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void listCodebookBankCodes_MappingError() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                //noinspection unchecked
                mapper.mapAsList( ( List ) any, BankCode.class );
                result = new RuntimeException();
            }
        };

        endpoint.listCodebookBankCodes( null, request, authUser );
    }

    @Test
    public void getCodebookBankCode() throws Exception
    {
        Locale locale = Locale.ENGLISH;
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                common.getAcceptLanguage( request );

                common.getAcceptLanguage( request );
                result = locale;

                service.getBankCode( account, "0900", locale, "SK" );
                mapper.map( dbBankCode, BankCode.class );
            }
        };

        BankCode bankCode = endpoint.getCodebookBankCode( "0900", "SK", request, authUser );
        assertThat( bankCode ).isNotNull();
    }

    @Test( expectedExceptions = NotFoundException.class )
    public void getCodebookBankCode_NotFound() throws Exception
    {
        new Expectations()
        {
            {
                //noinspection ConstantConditions
                service.getBankCode( ( LocalAccount ) any, anyString, ( Locale ) any, anyString );
                result = null;
            }
        };

        endpoint.getCodebookBankCode( "0900", "SK", request, authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void getCodebookBankCode_MappingFailure() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                mapper.map( dbBankCode, BankCode.class );
                result = new RuntimeException();
            }
        };

        endpoint.getCodebookBankCode( "0900", "SK", request, authUser );
    }

    @Test( expectedExceptions = InternalServerErrorException.class )
    public void getCodebookBankCode_RetrievalFailure() throws Exception
    {
        new Expectations()
        {
            {
                common.checkAccount( authUser, request );
                result = account;

                //noinspection ConstantConditions
                service.getBankCode( ( LocalAccount ) any, anyString, ( Locale ) any, anyString );
                result = new RuntimeException();
            }
        };

        endpoint.getCodebookBankCode( "0900", "SK", request, authUser );
    }
}