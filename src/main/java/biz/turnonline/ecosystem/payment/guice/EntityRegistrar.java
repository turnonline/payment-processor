package biz.turnonline.ecosystem.payment.guice;

/**
 * The class delegates timing of the objectify entities registration to the Guice,
 * with help of custom implementation of the {@link GuicefiedOfyFactory}.
 * <p>
 * The implementation of this interface to be taken into account must be configured via Guice:
 * <pre>
 *  Multibinder&#60;EntityRegistrar&#62; registrar = Multibinder.newSetBinder( binder(), EntityRegistrar.class );
 *  registrar.addBinding().to( MyEntityRegistrar.class );
 * </pre>
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public interface EntityRegistrar
{
    /**
     * Provide your own list of entities to be registered with Objectify, for example:
     * {@code factory.register( Product.class );}
     * <p>
     * Entities registered here will leverage full Guice injection.
     *
     * @param factory the factory used for entity registration
     */
    void register( GuicefiedOfyFactory factory );
}
