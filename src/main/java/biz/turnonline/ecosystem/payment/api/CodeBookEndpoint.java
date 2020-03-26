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
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiReference;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.common.base.MoreObjects;
import ma.glasnost.orika.MapperFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static biz.turnonline.ecosystem.payment.api.EndpointsCommon.bankCodeNotFoundMessage;
import static biz.turnonline.ecosystem.payment.api.EndpointsCommon.tryAgainLaterMessage;

/**
 * REST API Endpoint of the payment processor's code-books.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Api
@ApiReference( EndpointsApiProfile.class )
public class CodeBookEndpoint
{
    private static final Logger logger = LoggerFactory.getLogger( CodeBookEndpoint.class );

    private final EndpointsCommon common;

    private final MapperFacade mapper;

    private final CodeBook service;

    @Inject
    CodeBookEndpoint( EndpointsCommon common,
                      MapperFacade mapper,
                      CodeBook service )
    {
        this.common = common;
        this.mapper = mapper;
        this.service = service;
    }

    @ApiMethod( name = "bank_code.list", path = "codebook/bank-code", httpMethod = ApiMethod.HttpMethod.GET )
    public List<BankCode> listCodebookBankCodes( @Nullable @Named( "country" ) String country,
                                                 HttpServletRequest request,
                                                 User authUser )
            throws Exception
    {
        LocalAccount account = common.checkAccount( authUser, request );
        Locale language = common.getAcceptLanguage( request );
        List<BankCode> bankCodes;

        try
        {
            Map<String, biz.turnonline.ecosystem.payment.service.model.BankCode> dbBankCodes;
            dbBankCodes = service.getBankCodes( account, language, country );

            bankCodes = mapper.mapAsList( dbBankCodes.values(), BankCode.class );
        }
        catch ( Exception e )
        {
            logger.error( "BankCode code-book list retrieval has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
                    .add( "country", country )
                    .toString(), e );

            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }

        return bankCodes;
    }

    @ApiMethod( name = "bank_code.get", path = "codebook/bank-code/{code}", httpMethod = ApiMethod.HttpMethod.GET )
    public BankCode getCodebookBankCode( @Named( "code" ) String code,
                                         @Nullable @Named( "country" ) String country,
                                         HttpServletRequest request,
                                         User authUser )
            throws Exception
    {
        LocalAccount account = common.checkAccount( authUser, request );
        Locale language = common.getAcceptLanguage( request );
        BankCode bankCode;
        biz.turnonline.ecosystem.payment.service.model.BankCode dbBankCode;

        try
        {
            dbBankCode = service.getBankCode( account, code, language, country );
        }
        catch ( Exception e )
        {
            logger.error( "BankCode code-book single record retrieval has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
                    .add( "country", country )
                    .toString(), e );

            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }

        if ( dbBankCode == null )
        {
            throw new NotFoundException( bankCodeNotFoundMessage( code ) );
        }

        try
        {
            bankCode = mapper.map( dbBankCode, BankCode.class );
        }
        catch ( Exception e )
        {
            logger.error( "BankCode code-book mapping has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "Account", account.getId() )
                    .add( "country", country )
                    .toString(), e );

            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }

        return bankCode;
    }
}
