package biz.turnonline.ecosystem.payment.guice;

import com.google.api.server.spi.ServletInitializationParameters;
import com.google.api.server.spi.guice.EndpointsModule;
import com.googlecode.objectify.ObjectifyFilter;
import biz.turnonline.ecosystem.payment.api.MessageEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger( EndpointsInitialization.class );

    @Override
    protected void configureServlets()
    {
        ServletInitializationParameters params = ServletInitializationParameters.builder()
                // add your endpoint service implementation
                .addServiceClass( MessageEndpoint.class )
                .setClientIdWhitelistEnabled( true ).build();

        configureEndpoints( ENDPOINTS_SERVLET_PATH, params );

        bind( ObjectifyFilter.class ).in( Singleton.class );
        filter( "/*" ).through( ObjectifyFilter.class );
    }
}
