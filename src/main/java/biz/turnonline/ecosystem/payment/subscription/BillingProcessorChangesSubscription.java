/*
 * Copyright (c) 2021 TurnOnline.biz s.r.o. All Rights Reserved.
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

package biz.turnonline.ecosystem.payment.subscription;

import com.google.api.services.pubsub.model.PubsubMessage;
import org.ctoolkit.restapi.client.pubsub.PubsubCommand;
import org.ctoolkit.restapi.client.pubsub.PubsubMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Singleton;

/**
 * The 'bill.changes' subscription listener implementation.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
class BillingProcessorChangesSubscription
        implements PubsubMessageListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger( BillingProcessorChangesSubscription.class );

    private static final long serialVersionUID = 5590828228043735446L;

    @Override
    public void onMessage( @Nonnull PubsubMessage message, @Nonnull String subscription ) throws Exception
    {
        PubsubCommand command = new PubsubCommand( message );
        LOGGER.info( "Received data type: " + command.getDataType() );
        LOGGER.info( command.getData() );
    }
}