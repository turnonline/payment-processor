package biz.turnonline.ecosystem.payment.subscription;

import biz.turnonline.ecosystem.payment.service.NoRetryException;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import com.google.api.services.pubsub.model.PubsubMessage;
import com.googlecode.objectify.Key;
import org.ctoolkit.restapi.client.pubsub.PubsubMessageListener;
import org.ctoolkit.services.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * The base task that accepts JSON string to be deserialized to target entity (data type) while processing the task.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public abstract class JsonTask<T>
        extends Task<T>
        implements PubsubMessageListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger( JsonTask.class );

    private static final long serialVersionUID = 8448437186747171180L;

    private final Key<LocalAccount> accountKey;

    private final boolean delete;

    private final String json;

    /**
     * Constructor.
     *
     * @param accountKey the key of a local account as an owner of the payload
     * @param json       the JSON payload
     * @param delete     {@code true} if message represents a deletion
     */
    public JsonTask( @Nonnull Key<LocalAccount> accountKey,
                     @Nonnull String json,
                     boolean delete )
    {
        this.accountKey = checkNotNull( accountKey, "Debtor's account key can't be null" );
        this.json = checkNotNull( json, "JSON can't be null" );
        this.delete = delete;
    }

    @Override
    protected final void execute()
    {
        LocalAccount localAccount = ofy().load().key( accountKey ).now();
        if ( localAccount == null )
        {
            LOGGER.warn( "Local account " + accountKey + " not found" );
            return;
        }

        execute( localAccount, workWith() );
    }

    /**
     * De-serializes the JSON by the same implementation as Pub/Sub,
     * see {@link PubsubMessageListener#fromString(String, Class)}
     *
     * @return the de-serialized instance
     */
    @Override
    public final T workWith()
    {
        try
        {
            Class<T> type = checkNotNull( type(), "Data type can't be null" );
            return fromString( json, type );
        }
        catch ( IOException e )
        {
            LOGGER.error( "Deserialization from JSON has failed: \n" + json, e );
            throw new NoRetryException();
        }
    }

    /**
     * Returns the boolean indication whether Pub/Sub message represents a deletion.
     *
     * @return {@code true} if message represents a deletion
     */
    public boolean isDelete()
    {
        return delete;
    }

    /**
     * The client implementation to be executed asynchronously.
     *
     * @param account  the account that published the message
     * @param resource the de-serialized instance
     */
    protected abstract void execute( @Nonnull LocalAccount account, @Nonnull T resource );

    /**
     * The JSON data type, the type to be de-serialized to.
     *
     * @return the JSON data type
     */
    protected abstract Class<T> type();

    /**
     * TODO will be refactored once PubsubMessageListener functionality is moved to PubsubCommand
     * This task implements {@link PubsubMessageListener} only in order to reuse
     * the {@link PubsubMessageListener#fromString(String, Class)} method.
     */
    @Override
    public final void onMessage( @Nonnull PubsubMessage message, @Nonnull String subscription )
    {
        throw new IllegalArgumentException( "Not intended to be called" );
    }
}
