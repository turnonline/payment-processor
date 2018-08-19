package biz.turnonline.ecosystem.payment.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import biz.turnonline.ecosystem.payment.service.MicroserviceModule;
import org.ctoolkit.services.endpoints.EndpointsMonitorConfig;
import org.ctoolkit.services.guice.AppEngineEnvironmentContextListener;

/**
 * The main entry point to configure guice injection.
 *
 * @author <a href="mailto:aurel.medvegy@ctoolkit.org">Aurel Medvegy</a>
 */
public class GuiceInjectorListener
        extends AppEngineEnvironmentContextListener
{
    @Override
    protected Injector getDevelopmentInjector()
    {
        return Guice.createInjector( new MicroserviceModule(), new EndpointsInitialization() );
    }

    @Override
    protected Injector getProductionInjector()
    {
        return Guice.createInjector( new MicroserviceModule(),
                new EndpointsInitialization(),
                new EndpointsMonitorConfig() );
    }
}
