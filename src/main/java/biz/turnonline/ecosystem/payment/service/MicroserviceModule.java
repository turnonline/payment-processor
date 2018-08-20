package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.account.client.AccountStewardAdapterModule;
import biz.turnonline.ecosystem.account.client.AccountStewardApiModule;
import biz.turnonline.ecosystem.payment.guice.EntityRegistrar;
import biz.turnonline.ecosystem.payment.guice.GuicefiedOfyFactory;
import biz.turnonline.ecosystem.payment.service.model.BankAccount;
import biz.turnonline.ecosystem.payment.service.model.BankCode;
import biz.turnonline.ecosystem.payment.service.model.CodeBookItem;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.payment.service.model.LocalAccountProviderImpl;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import net.sf.jsr107cache.Cache;
import org.ctoolkit.restapi.client.appengine.CtoolkitRestFacadeAppEngineModule;
import org.ctoolkit.restapi.client.appengine.DefaultOrikaMapperFactoryModule;
import org.ctoolkit.restapi.client.appengine.JCacheProvider;
import org.ctoolkit.services.guice.CtoolkitServicesAppEngineModule;
import org.ctoolkit.services.storage.CtoolkitServicesStorageModule;
import org.ctoolkit.services.storage.DefaultStorageProvider;
import org.ctoolkit.services.task.CtoolkitServicesTaskModule;

import javax.inject.Singleton;

/**
 * The application injection configuration.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class MicroserviceModule
        extends AbstractModule
{
    public static final String API_PREFIX = "payment";

    @Override
    protected void configure()
    {
        install( new CtoolkitRestFacadeAppEngineModule() );
        install( new DefaultOrikaMapperFactoryModule() );
        install( new CtoolkitServicesAppEngineModule() );
        install( new CtoolkitServicesStorageModule() );
        install( new CtoolkitServicesTaskModule() );
        install( new AccountStewardApiModule() );
        install( new AccountStewardAdapterModule() );

        bind( PaymentConfig.class ).to( PaymentConfigBean.class );
        bind( CodeBook.class ).to( CodeBookBean.class );
        bind( LocalAccountProvider.class ).to( LocalAccountProviderImpl.class );
        bind( GuicefiedOfyFactory.class ).asEagerSingleton();

        bind( Cache.class ).toProvider( JCacheProvider.class ).in( Singleton.class );
        bind( Storage.class ).toProvider( DefaultStorageProvider.class ).in( Singleton.class );

        Multibinder<EntityRegistrar> registrar = Multibinder.newSetBinder( binder(), EntityRegistrar.class );
        registrar.addBinding().to( Entities.class );
    }

    @Provides
    @Singleton
    ObjectMapper provideObjectMapper()
    {
        JsonFactory factory = new JsonFactory();
        factory.enable( JsonParser.Feature.ALLOW_COMMENTS );

        ObjectMapper mapper = new ObjectMapper( factory );
        mapper.setSerializationInclusion( JsonInclude.Include.NON_NULL );

        return mapper;
    }

    private static class Entities
            implements EntityRegistrar
    {
        @Override
        public void register( GuicefiedOfyFactory factory )
        {
            factory.register( CodeBookItem.class );
            factory.register( BankCode.class );
            factory.register( LocalAccount.class );
            factory.register( BankAccount.class );
        }
    }
}
