package biz.turnonline.ecosystem.payment.service.model;

import com.google.api.client.util.DateTime;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Index;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link org.ctoolkit.services.datastore.objectify.Timestamp}
 * configured as an objectify entity.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Entity( name = "PP_Timestamp" )
public class Timestamp
        extends org.ctoolkit.services.datastore.objectify.Timestamp
{
    @Index
    private Key<LocalAccount> owner;

    @SuppressWarnings( "unused" )
    Timestamp()
    {
    }

    /**
     * Constructor.
     * <p>
     * Sets the last modification date of the original resource.
     * It will be used to distinguish whether an incoming changes are obsolete or not.
     *
     * @param type      the type name of the resource the timestamp tracks modification date and time
     * @param uniqueKey the resource unique key as a list of IDs
     * @param last      the last modification date of incoming resource
     */
    @SuppressWarnings( "unused" )
    public Timestamp( @Nonnull String type, @Nonnull List<String> uniqueKey, @Nonnull Date last )
    {
        super( type, uniqueKey, last );
    }

    public static Timestamp of( @Nonnull String type,
                                @Nonnull List<String> uniqueKey,
                                @Nonnull LocalAccount owner,
                                @Nullable DateTime last )
    {
        Timestamp timestamp = of( type, uniqueKey, last, Timestamp.class );
        timestamp.owner = Key.create( checkNotNull( owner ) );
        return timestamp;
    }
}
