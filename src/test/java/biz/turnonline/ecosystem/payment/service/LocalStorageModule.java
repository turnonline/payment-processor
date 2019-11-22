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
