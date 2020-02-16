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

package biz.turnonline.ecosystem.payment.subscription;

import biz.turnonline.ecosystem.payment.service.model.AccountStewardChangesSubscription;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import org.ctoolkit.restapi.client.pubsub.PubsubMessageListener;
import org.ctoolkit.restapi.client.pubsub.SubscriptionsListenerModule;

/**
 * Pub/Sub subscription configuration for following:
 * <ul>
 * <li>account.changes</li>
 * <li>billing.changes</li>
 * </ul>
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class SubscriptionsModule
        extends AbstractModule
{
    @Override
    protected void configure()
    {
        install( new SubscriptionsListenerModule() );

        MapBinder<String, PubsubMessageListener> map;
        map = MapBinder.newMapBinder( binder(), String.class, PubsubMessageListener.class );
        map.addBinding( "account.changes" ).to( AccountStewardChangesSubscription.class );
        map.addBinding( "billing.changes" ).to( ProductBillingChangesSubscription.class );
    }
}
