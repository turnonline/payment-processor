package biz.turnonline.ecosystem.payment.service;

import com.google.appengine.tools.development.testing.LocalServiceTestConfig;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import org.ctoolkit.services.storage.guice.GuicefiedOfyFactory;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Local Objectify Google Datastore helper, a configuration to initialize local datastore emulator for unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
class LocalObjectifyHelper
        implements LocalServiceTestConfig
{
    private double consistency;

    private int port;

    private Closeable session;

    private GuicefiedOfyFactory ofyFactory;

    private LocalDatastoreHelper lDatastoreHelper;

    /**
     * Creates a local Datastore helper with the default settings.
     */
    LocalObjectifyHelper()
    {
        this( 1.0, 0 );
    }

    /**
     * Creates a local Datastore helper with the default settings.
     *
     * @param consistency the fraction of Datastore writes that are immediately visible to global
     *                    queries, with 0.0 meaning no writes are immediately visible and 1.0 meaning all writes are
     *                    immediately visible. Note that setting this to 1.0 may mask incorrect assumptions about the
     *                    consistency of non-ancestor queries; non-ancestor queries are eventually consistent.
     */
    LocalObjectifyHelper( double consistency )
    {
        this( consistency, 0 );
    }

    /**
     * Creates a local Datastore helper with the specified settings for project ID and consistency.
     *
     * @param consistency the fraction of Datastore writes that are immediately visible to global
     *                    queries, with 0.0 meaning no writes are immediately visible and 1.0 meaning all writes are
     *                    immediately visible. Note that setting this to 1.0 may mask incorrect assumptions about the
     *                    consistency of non-ancestor queries; non-ancestor queries are eventually consistent.
     * @param port        the port to be used to start the emulator service. Note that setting this to 0 the
     *                    emulator will search for a free random port.
     */
    private LocalObjectifyHelper( double consistency, int port )
    {
        this.consistency = consistency;
        this.port = port;
    }

    void init( @Nullable LocalDatastoreHelper helper,
               @Nullable GuicefiedOfyFactory ofyFactory )
    {
        this.ofyFactory = ofyFactory;
        lDatastoreHelper = helper;
    }

    @Override
    public void setUp()
    {
        if ( lDatastoreHelper == null )
        {
            String message = LocalDatastoreHelper.class.getSimpleName()
                    + " not initialized yet, first call start( ITestContext )";

            throw new IllegalArgumentException( message );
        }

        try
        {
            lDatastoreHelper.reset();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }

        ObjectifyFactory factory;
        if ( ofyFactory == null )
        {
            Datastore datastore = lDatastoreHelper.getOptions().getService();
            factory = new ObjectifyFactory( datastore );
        }
        else
        {
            factory = ofyFactory;

        }

        ObjectifyService.init( factory );
        session = ObjectifyService.begin();
    }

    @Override
    public void tearDown()
    {
        try
        {
            session.close();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    /**
     * Starts the local Datastore emulator through {@code gcloud}.
     *
     * <p>Currently the emulator does not persist any state across runs.
     */
    public void start() throws IOException, InterruptedException
    {
        LocalDatastoreHelper helper = get();
        helper.start();
    }

    /**
     * Stops the Datastore emulator.
     */
    public void stop() throws InterruptedException, TimeoutException, IOException
    {
        LocalDatastoreHelper helper = get();
        helper.stop();
    }

    private LocalDatastoreHelper get()
    {
        if ( lDatastoreHelper == null )
        {
            lDatastoreHelper = LocalDatastoreHelper.create( consistency, port );
        }
        return lDatastoreHelper;
    }
}
