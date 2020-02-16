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
import com.google.common.base.Stopwatch;
import org.ctoolkit.restapi.client.RestFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

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
    public LocalAccount initGet( @Nonnull Builder builder )
    {
        checkNotNull( builder, "Builder can't be null" );
        checkNotNull( builder.getEmail(), "Account email can't be null" );
        checkNotNull( builder.getIdentityId(), "Account Identity ID is mandatory" );
        checkNotNull( builder.getAudience(), "Account audience can't be null" );

        LocalAccount localAccount = get( builder.getEmail(), builder.getAudience() );

        if ( localAccount == null )
        {
            Stopwatch stopwatch = Stopwatch.createStarted();
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

        if ( !builder.getIdentityId().equals( localAccount.getIdentityId() ) )
        {
            logger.error( "IdentityId mismatch. Current " + LocalAccount.class.getSimpleName() + " '"
                    + localAccount.getIdentityId() + "' does not match to the authenticated account: " + builder );
            throw new IllegalArgumentException( "Identity mismatch." );
        }

        return localAccount;
    }

    @Override
    public LocalAccount get( @Nonnull String email, @Nonnull String audience )
    {
        checkNotNull( email, "Account email can't be null" );
        checkNotNull( audience, "Account audience can't be null" );

        return ofy()
                .load()
                .type( LocalAccount.class )
                .filter( "email", email )
                .filter( "audience", audience )
                .first()
                .now();
    }
}
