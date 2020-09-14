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

package biz.turnonline.ecosystem.payment.service.model;

import biz.turnonline.ecosystem.payment.service.LocalAccountProvider;
import biz.turnonline.ecosystem.steward.model.Account;
import com.google.cloud.ServiceOptions;
import com.google.common.base.Stopwatch;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.restapi.client.pubsub.PubsubCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * The {@link LocalAccountProvider} implementation.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
public class LocalAccountProviderImpl
        implements LocalAccountProvider
{
    private static final Logger logger = LoggerFactory.getLogger( LocalAccountProviderImpl.class );

    private final RestFacade facade;

    @Inject
    LocalAccountProviderImpl( RestFacade facade )
    {
        this.facade = facade;
    }

    @Override
    public LocalAccount check( @Nonnull PubsubCommand command )
    {
        checkNotNull( command, "PubsubCommand can't be null" );

        LocalAccount localAccount;
        if ( ofy().load().type( LocalAccount.class ).count() == 0 )
        {
            Stopwatch stopwatch = Stopwatch.createStarted();
            Account builder = new Account()
                    .setEmail( command.getAccountEmail() )
                    .setIdentityId( command.getAccountIdentityId() )
                    .setId( command.getAccountId() );

            LocalAccount temp = new LocalAccount( builder );

            Account remote = facade.get( Account.class )
                    .identifiedBy( builder.getIdentityId() )
                    .onBehalfOf( temp )
                    .finish();

            localAccount = new LocalAccount( remote );
            localAccount.save();
            stopwatch.stop();
            logger.info( "Local account just has been created (" + stopwatch + "): " + localAccount );
        }
        else
        {
            localAccount = get();
        }

        if ( localAccount == null
                || !Objects.equals( localAccount.getIdentityId(), command.getAccountIdentityId() ) )
        {
            LocalDeputyAccount deputyAccount = get( command.getAccountEmail() );
            if ( ( localAccount != null
                    && deputyAccount != null
                    && deputyAccount.checkOwner( localAccount ) ) )
            {
                return localAccount;
            }

            logger.warn( "Account '" + command.getAccountIdentityId() + "' is not associated with this service" );
            return null;
        }
        return localAccount;

    }

    @Override
    public LocalAccount get()
    {
        String projectId = ServiceOptions.getDefaultProjectId();
        PaymentLocalAccount pla = ofy().load().type( PaymentLocalAccount.class ).id( projectId ).now();
        return pla == null ? null : pla.get();
    }

    @Override
    public LocalDeputyAccount get( @Nonnull String email )
    {
        return ofy().load().type( LocalDeputyAccount.class ).id( email ).now();
    }
}
