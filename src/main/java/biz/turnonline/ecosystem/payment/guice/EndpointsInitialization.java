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

package biz.turnonline.ecosystem.payment.guice;

import biz.turnonline.ecosystem.payment.api.BankAccountEndpoint;
import biz.turnonline.ecosystem.payment.api.CategoryEndpoint;
import biz.turnonline.ecosystem.payment.api.CodeBookCacheControlFilter;
import biz.turnonline.ecosystem.payment.api.CodeBookEndpoint;
import com.google.api.server.spi.ServletInitializationParameters;
import com.google.api.server.spi.guice.EndpointsModule;
import com.googlecode.objectify.ObjectifyFilter;

import javax.inject.Singleton;

import static org.ctoolkit.services.endpoints.EndpointsMonitorConfig.ENDPOINTS_SERVLET_PATH;

/**
 * The endpoints service classes configuration.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 * @see EndpointsModule
 */
public class EndpointsInitialization
        extends EndpointsModule
{
    @Override
    protected void configureServlets()
    {
        ServletInitializationParameters params = ServletInitializationParameters.builder()
                // add your endpoint service implementation
                .addServiceClass( CodeBookEndpoint.class )
                .addServiceClass( BankAccountEndpoint.class )
                .addServiceClass( CategoryEndpoint.class )
                .setClientIdWhitelistEnabled( false ).build();

        configureEndpoints( ENDPOINTS_SERVLET_PATH, params );

        bind( ObjectifyFilter.class ).in( Singleton.class );

        filter( "/*" ).through( ObjectifyFilter.class );
        filter( CodeBookCacheControlFilter.FILTER_PATH ).through( CodeBookCacheControlFilter.class );

        //https://stackoverflow.com/questions/50339907/using-google-cloud-endpoint-framework-2-0-with-custom-domain
        //serve( "/*" ).with( GuiceEndpointsServlet.class, params.asMap() );
    }
}
