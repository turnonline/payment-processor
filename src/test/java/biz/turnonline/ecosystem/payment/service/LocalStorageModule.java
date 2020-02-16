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
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import com.google.cloud.storage.Storage;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.ctoolkit.services.storage.DefaultStorageProvider;

import javax.inject.Singleton;

/**
 * Dedicated module for Google Cloud Datastore emulator configuration, intended for local development or unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class LocalStorageModule
        extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind( Storage.class ).toProvider( DefaultStorageProvider.class ).in( Singleton.class );
    }

    @Provides
    @Singleton
    Datastore providesDatastore( LocalDatastoreHelper helper )
    {
        return helper.getOptions().getService();
    }

    @Provides
    @Singleton
    LocalDatastoreHelper providesLocalDatastoreHelper()
    {
        return LocalDatastoreHelper.create( 1.0 );
    }
}
