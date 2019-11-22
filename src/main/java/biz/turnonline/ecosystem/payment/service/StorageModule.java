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
