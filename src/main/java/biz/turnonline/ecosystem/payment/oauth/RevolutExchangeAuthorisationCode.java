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

package biz.turnonline.ecosystem.payment.oauth;

import biz.turnonline.ecosystem.revolut.business.account.model.Account;
import biz.turnonline.ecosystem.revolut.business.facade.RevolutBusinessProvider;
import com.googlecode.objectify.Key;
import org.ctoolkit.restapi.client.ClientErrorException;
import org.ctoolkit.restapi.client.ForbiddenException;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.restapi.client.UnauthorizedException;
import org.ctoolkit.services.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Authorisation Code exchange will enable access to Revolut for Business API.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class RevolutExchangeAuthorisationCode
        extends Task<RevolutCertMetadata>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( RevolutExchangeAuthorisationCode.class );

    private static final long serialVersionUID = 7682871294183932878L;

    @Inject
    transient RestFacade facade;

    @Inject
    transient RevolutBusinessProvider revolut;

    public RevolutExchangeAuthorisationCode( @Nonnull Key<RevolutCertMetadata> key )
    {
        super( "Exchange-Authorisation-Code" );
        setEntityKey( checkNotNull( key, "Entity key can't be null" ) );
    }

    @Override
    protected void execute()
    {
        RevolutCertMetadata details;
        revolut.resetAccessToken();

        try
        {
            // Just call a GET operation to invoke a Revolut API, it causes authorisation code exchange.
            // Everything managed by RevolutCredential.
            facade.list( Account.class ).finish();

            details = workWith( null );
            details.accessGranted().save();
            LOGGER.info( "Authorisation code has been exchanged to authorize access to Revolut for Business API: "
                    + details );

        }
        catch ( UnauthorizedException | ClientErrorException | ForbiddenException e )
        {
            details = workWith( null );
            LOGGER.warn( "Authorisation code seems to be invalid and will be reset; " + details, e );
            details.setCode( null ).save();
        }
    }
}
