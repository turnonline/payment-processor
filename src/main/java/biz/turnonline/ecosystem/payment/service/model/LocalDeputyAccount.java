package biz.turnonline.ecosystem.payment.service.model;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import org.ctoolkit.services.datastore.objectify.EntityStringIdentity;
import org.ctoolkit.services.storage.HasOwner;

import javax.annotation.Nonnull;
import java.util.Locale;

import static biz.turnonline.ecosystem.payment.service.model.LocalAccount.DEFAULT_LOCALE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * The local deputy account representation that's associated with a payment local account.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Entity( name = "PP_DeputyAccount" )
public class LocalDeputyAccount
        extends EntityStringIdentity
        implements HasOwner<LocalAccount>
{
    private static final long serialVersionUID = 5513598523531839581L;

    private Ref<LocalAccount> account;

    private String locale;

    private String role;

    LocalDeputyAccount()
    {
    }

    LocalDeputyAccount( @Nonnull String email )
    {
        super.setId( checkNotNull( email, "Deputy account email can't be null" ).trim().toLowerCase() );
    }

    public final void setId( String id )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * The login email address of the deputy account.
     *
     * @return the account login email
     */
    public final String getEmail()
    {
        return super.getId();
    }

    /**
     * Returns the role of the user that logs in with associated email.
     *
     * @return the role of the deputy user
     */
    public String getRole()
    {
        return role;
    }

    void setRole( String role )
    {
        this.role = role;
    }

    /**
     * Returns the account locale or default {@link LocalAccount#DEFAULT_LOCALE} locale.
     *
     * @return the final locale, ISO 639 alpha-2 or alpha-3 language code
     */
    public Locale getLocale()
    {
        return convertJavaLocale( this.locale, DEFAULT_LOCALE );
    }

    /**
     * Sets the preferred account language. ISO 639 alpha-2 or alpha-3 language code.
     *
     * @param locale the language to be set
     */
    void setLocale( String locale )
    {
        this.locale = locale;
    }

    @Override
    protected long getModelVersion()
    {
        //21.10.2017 08:00:00 GMT+0200
        return 1508565600000L;
    }

    @Override
    public void save()
    {
        ofy().transact( () -> ofy().defer().save().entity( this ) );
    }

    @Override
    public void delete()
    {
        ofy().transact( () -> ofy().defer().delete().entity( this ) );
    }

    @Override
    public LocalAccount getOwner()
    {
        return fromRef( account, null );
    }

    @Override
    public void setOwner( @Nonnull LocalAccount owner )
    {
        account = Ref.create( checkNotNull( owner, "LocalAccount can't be null" ) );
    }
}