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

import biz.turnonline.ecosystem.payment.service.LocalAccountProvider;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.steward.model.Account;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.common.net.HttpHeaders;
import org.ctoolkit.services.endpoints.AudienceUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * The common services to handle REST API endpoints requests.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
class EndpointsCommon
{
    private static final Logger logger = LoggerFactory.getLogger( EndpointsCommon.class );

    private final LocalAccountProvider lap;

    @Inject
    EndpointsCommon( LocalAccountProvider lap )
    {
        this.lap = lap;
    }

    static String tryAgainLaterMessage()
    {
        return "Try again later";
    }

    private static String accountNotFoundMessage( @Nonnull String email )
    {
        return "Account with email '" + email + "' has not been found.";
    }

    static String bankCodeNotFoundMessage( String code )
    {
        String message = "Bank with code '%s' has not been found";
        return String.format( message, code );
    }

    static String bankAccountNotFoundMessage( Long id )
    {
        String message = "Bank account with ID '%d' has not been found";
        return String.format( message, id );
    }

    static String primaryBankAccountNotFoundMessage( String country )
    {
        String message = "Primary bank account for country '%s' has not been found";
        return String.format( message, country );
    }

    static String primaryBankAccountNotFoundMessage()
    {
        return "Primary bank account has not been found";
    }

    /**
     * For unauthorized user throws {@link UnauthorizedException} otherwise does nothing.
     *
     * @param authUser the authorized user instance
     */
    void authorize( User authUser )
            throws UnauthorizedException
    {
        if ( authUser == null )
        {
            throw new UnauthorizedException( "User is unauthorized." );
        }
    }

    /**
     * Returns the account taken from the account steward microservice
     * for given authenticated user if account has been found.
     *
     * @param authUser the endpoints authenticated user
     * @param request  the current HTTP request
     * @return the authorized account
     * @throws UnauthorizedException        if the authorization did not pass
     * @throws NotFoundException            if the auth email represents an unknown account
     * @throws InternalServerErrorException if execution has failed
     */
    LocalAccount checkAccount( User authUser, HttpServletRequest request )
            throws ServiceException
    {
        authorize( authUser );

        AudienceUser audienceUser = ( AudienceUser ) authUser;
        String authEmail = audienceUser.getEmail();
        LocalAccount account = null;
        boolean notFound = false;

        try
        {
            account = lap.initGet( new LocalAccountProvider.Builder()
                    .email( audienceUser.getEmail() )
                    .identityId( audienceUser.getId() )
                    .audience( audienceUser.getAudience() ) );

            request.setAttribute( Account.class.getName(), account );
        }
        catch ( org.ctoolkit.restapi.client.NotFoundException e )
        {
            notFound = true;
        }
        catch ( Exception e )
        {
            logger.error( "Account retrieval has failed for " + audienceUser, e );
            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }

        if ( notFound )
        {
            throw new NotFoundException( accountNotFoundMessage( authEmail ) );
        }

        return account;
    }

    /**
     * Returns the language taken from {@link HttpHeaders#ACCEPT_LANGUAGE} header,
     * or {@code null} if not provided by the client.
     *
     * @param request HTTP request
     * @return the Accept-Language based locale, or {@code null}
     */
    Locale getAcceptLanguage( HttpServletRequest request )
    {
        String language = request.getHeader( HttpHeaders.ACCEPT_LANGUAGE );

        if ( language == null )
        {
            return null;
        }

        return request.getLocale();
    }
}
