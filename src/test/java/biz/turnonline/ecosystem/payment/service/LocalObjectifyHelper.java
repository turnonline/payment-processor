package biz.turnonline.ecosystem.payment.service;

import com.google.appengine.tools.development.testing.LocalServiceTestConfig;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import org.ctoolkit.services.storage.guice.GuicefiedOfyFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Local Objectify Google Datastore helper, a configuration to initialize local datastore emulator for unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
class LocalObjectifyHelper
        implements LocalServiceTestConfig
{
    private final GuicefiedOfyFactory ofyFactory;

    private final LocalDatastoreHelper lDatastoreHelper;

    private Closeable session;

    @Inject
    public LocalObjectifyHelper( GuicefiedOfyFactory ofyFactory, LocalDatastoreHelper helper )
    {
        this.ofyFactory = checkNotNull( ofyFactory );
        this.lDatastoreHelper = checkNotNull( helper );
    }

    @Override
    public void setUp()
    {
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
        lDatastoreHelper.start();
    }

    /**
     * Stops the Datastore emulator.
     */
    public void stop() throws InterruptedException, TimeoutException, IOException
    {
        lDatastoreHelper.stop();
    }
}
