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

import biz.turnonline.ecosystem.payment.oauth.RevolutCertMetadata;
import biz.turnonline.ecosystem.payment.oauth.RevolutCredentialAdministration;
import biz.turnonline.ecosystem.payment.service.model.BankAccount;
import biz.turnonline.ecosystem.payment.service.model.BankCode;
import biz.turnonline.ecosystem.payment.service.model.BeneficiaryBankAccount;
import biz.turnonline.ecosystem.payment.service.model.CodeBookItem;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.payment.service.model.LocalAccountProviderImpl;
import biz.turnonline.ecosystem.payment.service.model.PaymentBeanMapperConfig;
import biz.turnonline.ecosystem.payment.service.model.Timestamp;
import biz.turnonline.ecosystem.payment.service.model.Transaction;
import biz.turnonline.ecosystem.payment.service.model.TransactionBill;
import biz.turnonline.ecosystem.payment.service.model.TransactionInvoice;
import biz.turnonline.ecosystem.payment.subscription.SubscriptionsModule;
import biz.turnonline.ecosystem.revolut.business.facade.RevolutBusinessAdapterModule;
import biz.turnonline.ecosystem.revolut.business.facade.RevolutBusinessClientModule;
import biz.turnonline.ecosystem.revolut.business.oauth.JwtFactory;
import biz.turnonline.ecosystem.revolut.business.oauth.RevolutCredential;
import biz.turnonline.ecosystem.steward.facade.AccountStewardAdapterModule;
import biz.turnonline.ecosystem.steward.facade.AccountStewardClientModule;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import net.sf.jsr107cache.Cache;
import org.ctoolkit.restapi.client.ApiCredential;
import org.ctoolkit.restapi.client.adapter.BeanMapperConfig;
import org.ctoolkit.restapi.client.appengine.CtoolkitRestFacadeAppEngineModule;
import org.ctoolkit.restapi.client.appengine.CtoolkitRestFacadeDefaultOrikaModule;
import org.ctoolkit.restapi.client.appengine.JCacheProvider;
import org.ctoolkit.restapi.client.firebase.GoogleApiFirebaseModule;
import org.ctoolkit.restapi.client.provider.TokenProvider;
import org.ctoolkit.services.guice.CtoolkitServicesAppEngineModule;
import org.ctoolkit.services.storage.CtoolkitServicesStorageModule;
import org.ctoolkit.services.storage.guice.EntityRegistrar;
import org.ctoolkit.services.storage.guice.GuicefiedOfyFactory;
import org.ctoolkit.services.task.CtoolkitServicesTaskModule;
import org.ctoolkit.services.task.Task;

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
        install( new CtoolkitRestFacadeDefaultOrikaModule() );
        install( new CtoolkitServicesAppEngineModule() );
        install( new CtoolkitServicesStorageModule() );
        install( new CtoolkitServicesTaskModule() );
        install( new AccountStewardClientModule() );
        install( new AccountStewardAdapterModule() );
        install( new GoogleApiFirebaseModule() );
        install( new SubscriptionsModule() );
        install( new RevolutBusinessClientModule() );
        install( new RevolutBusinessAdapterModule() );

        bind( PaymentConfig.class ).to( PaymentConfigBean.class );
        bind( CodeBook.class ).to( CodeBookBean.class );
        bind( LocalAccountProvider.class ).to( LocalAccountProviderImpl.class );
        bind( Cache.class ).toProvider( JCacheProvider.class ).in( Singleton.class );
        bind( RevolutCredential.Certificate.class ).to( RevolutCredentialAdministration.class );
        bind( RevolutCredential.Storage.class ).to( RevolutCredentialAdministration.class );
        bind( RevolutCredential.JwtTokenFactory.class ).to( JwtFactory.class ).in( Singleton.class );

        Multibinder<EntityRegistrar> registrar = Multibinder.newSetBinder( binder(), EntityRegistrar.class );
        registrar.addBinding().to( Entities.class );

        Multibinder<BeanMapperConfig> multi = Multibinder.newSetBinder( binder(), BeanMapperConfig.class );
        multi.addBinding().to( PaymentBeanMapperConfig.class );

        bind( new TypeLiteral<TokenProvider<LocalAccount>>()
        {
        } ).to( ClosedServerToServerTokenOfAccount.class );

        // single declaration to request static injection for all Task related injection
        requestStaticInjection( Task.class );

        ApiCredential config = new ApiCredential();
        config.setNumberOfRetries( 10 );
        config.load( "/config.properties" );
        Names.bindProperties( binder(), config );
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
            factory.register( CompanyBankAccount.class );
            factory.register( BeneficiaryBankAccount.class );
            factory.register( Timestamp.class );
            factory.register( RevolutCertMetadata.class );
            factory.register( Transaction.class );
            factory.register( TransactionInvoice.class );
            factory.register( TransactionBill.class );
        }
    }
}
