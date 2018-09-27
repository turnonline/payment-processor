package biz.turnonline.ecosystem.payment.guice;

import biz.turnonline.ecosystem.payment.api.BankAccountEndpoint;
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
                .setClientIdWhitelistEnabled( true ).build();

        configureEndpoints( ENDPOINTS_SERVLET_PATH, params );

        bind( ObjectifyFilter.class ).in( Singleton.class );

        filter( "/*" ).through( ObjectifyFilter.class );
        filter( CodeBookCacheControlFilter.FILTER_PATH ).through( CodeBookCacheControlFilter.class );

        //https://stackoverflow.com/questions/50339907/using-google-cloud-endpoint-framework-2-0-with-custom-domain
        //serve( "/*" ).with( GuiceEndpointsServlet.class, params.asMap() );
    }
}
