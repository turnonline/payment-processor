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

package biz.turnonline.ecosystem.payment.service;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.storage.Storage;
import com.google.inject.AbstractModule;
import org.ctoolkit.services.datastore.DefaultDatastoreProvider;
import org.ctoolkit.services.storage.DefaultStorageProvider;
import org.ctoolkit.services.storage.guice.GuicefiedOfyFactory;

import javax.inject.Singleton;

/**
 * Dedicated module solely for Google Cloud Datastore/Storage configuration.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class StorageModule
        extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind( Storage.class ).toProvider( DefaultStorageProvider.class ).in( Singleton.class );
        bind( Datastore.class ).toProvider( DefaultDatastoreProvider.class ).in( Singleton.class );
        bind( GuicefiedOfyFactory.class ).asEagerSingleton();
    }
}
