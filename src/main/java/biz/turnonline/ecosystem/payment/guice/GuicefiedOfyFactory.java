package biz.turnonline.ecosystem.payment.guice;

import com.google.inject.Injector;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

/**
 * TODO once GuicefiedOfyFactory will became available in ctoolkit-services, reuse it.
 * Objectify factory with plugged-in Guice to be responsible for entity instantiation.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
public class GuicefiedOfyFactory
        extends ObjectifyFactory
{
    private final Injector injector;

    @Inject
    public GuicefiedOfyFactory( Injector injector, Set<EntityRegistrar> configs )
    {
        this.injector = injector;
        ObjectifyService.setFactory( this );

        for ( EntityRegistrar next : configs )
        {
            next.register( this );
        }
    }

    @Override
    public <T> T construct( Class<T> type )
    {
        return injector.getInstance( type );
    }
}
