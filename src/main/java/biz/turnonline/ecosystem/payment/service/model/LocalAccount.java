package biz.turnonline.ecosystem.payment.service.model;

import biz.turnonline.ecosystem.payment.service.LocalAccountProvider;
import biz.turnonline.ecosystem.payment.service.NoRetryException;
import biz.turnonline.ecosystem.steward.model.Account;
import biz.turnonline.ecosystem.steward.model.AccountBusiness;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import org.ctoolkit.restapi.client.NotFoundException;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.services.storage.appengine.objectify.EntityLongIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Locale;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * The datastore local account acts as an owner of the entities associated with an account.
 * It represents the TurnOnline.biz account taken from the Account Steward microservice.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Cache( expirationSeconds = 3600 )
@Entity( name = "PP_LocalAccount" )
public class LocalAccount
        extends EntityLongIdentity
{
    private static final long serialVersionUID = 6766053569523752265L;

    private static final Logger LOGGER = LoggerFactory.getLogger( LocalAccount.class );

    private static final Locale DEFAULT_LOCALE = new Locale( "en" );

    private static final String DEFAULT_DOMICILE = "SK";

    @Ignore
    private transient RestFacade facade;

    @Ignore
    private transient Account tAccount;

    @Index
    private String email;

    @Index
    private String identityId;

    @Index
    private String audience;

    private String domicile;

    private String locale;

    @Inject
    LocalAccount( RestFacade facade )
    {
        this.facade = facade;
    }

    LocalAccount( @Nonnull LocalAccountProvider.Builder builder )
    {
        checkNotNull( builder, "Builder can't be null" );
        this.email = checkNotNull( builder.email, "Account email is mandatory" );
        this.audience = checkNotNull( builder.audience, "Account audience is mandatory" );
        this.identityId = checkNotNull( builder.identityId, "Account Identity ID is mandatory" );
    }

    public LocalAccount( @Nonnull Account account )
    {
        checkNotNull( account, "Account is mandatory" );

        super.setId( checkNotNull( account.getId(), "Account ID is mandatory" ) );
        this.email = checkNotNull( account.getEmail(), "Account email is mandatory" );
        this.audience = checkNotNull( account.getAudience(), "Account audience is mandatory" );
        this.identityId = checkNotNull( account.getIdentityId(), "Account Identity ID is mandatory" );
        this.tAccount = account;
        this.locale = account.getLocale();

        AccountBusiness business = account.getBusiness();
        if ( business != null )
        {
            this.domicile = business.getDomicile();
        }
    }

    public String getIdentityId()
    {
        return identityId;
    }

    public String getEmail()
    {
        return email;
    }

    /**
     * The audience unique identification. The user identified by login email address
     * within one audience is a different user within another audience even with the same login email.
     * Those users have different Account.IDs.
     **/
    public String getAudience()
    {
        return audience;
    }

    /**
     * Retrieves remote account identified by {@link #getEmail()}.
     * Authentication against microservice is via service account.
     *
     * @return the account or {@code null} if not found
     */
    public Account getRemote()
    {
        if ( tAccount != null )
        {
            return tAccount;
        }

        try
        {
            tAccount = facade.get( Account.class )
                    .identifiedBy( String.valueOf( getId() ) )
                    .onBehalfOf( this )
                    .finish();
        }
        catch ( NotFoundException e )
        {
            LOGGER.warn( "The remote account with email: '" + getEmail() + "' not found." );
            tAccount = null;
        }
        return tAccount;
    }

    /**
     * Returns remote {@link Account} instance otherwise exception will be thrown.
     *
     * @return the remote account business instance
     * @throws NoRetryException if remote account not found
     * @see #getRemote()
     */
    public Account getAccount()
    {
        Account account = getRemote();
        if ( account == null )
        {
            String message = "The remote account " + this + " not found.";
            throw new NoRetryException( message );
        }

        return account;
    }

    /**
     * Returns {@link AccountBusiness} instance otherwise exception will be thrown.
     *
     * @return the remote account business instance
     * @throws NoRetryException if not configured yet
     */
    public AccountBusiness getBusiness()
    {
        Account account = getRemote();
        if ( account == null )
        {
            String message = "The remote account " + this + " not found.";
            throw new NoRetryException( message );
        }

        AccountBusiness business = account.getBusiness();
        if ( business == null )
        {
            String message = "Account " + this + " has no business setup yet.";
            throw new NoRetryException( message );
        }

        return business;
    }

    /**
     * Returns the final locale based preferable on {@link Account#getLocale()}. Always returns a value.
     * If none of the values has been found a {@link #DEFAULT_LOCALE} will be returned.
     *
     * @return the final locale, ISO 639 alpha-2 or alpha-3 language code
     */
    public Locale getLocale()
    {
        return getLocale( null );
    }

    /**
     * Sets the preferred account language. ISO 639 alpha-2 or alpha-3 language code.
     *
     * @param locale the language to be set
     */
    public void setLocale( String locale )
    {
        this.locale = locale;
    }

    /**
     * Returns the final locale based preferable on {@link Account#getLocale()} with specified preference.
     * Always returns a value. If none of the values has been found a {@link #DEFAULT_LOCALE} will be returned.
     *
     * @param locale the optional however preferred language
     * @return the final locale
     */
    public Locale getLocale( @Nullable Locale locale )
    {
        if ( locale == null )
        {
            if ( Strings.isNullOrEmpty( this.locale ) )
            {
                locale = DEFAULT_LOCALE;
                LOGGER.warn( "Using service default locale: " + locale );
            }
            else
            {
                locale = new Locale( this.locale );
            }
        }
        return locale;
    }

    /**
     * Returns the account domicile with optional preference. Always returns a value.
     * If none domicile value found a {@link #DEFAULT_DOMICILE} will be returned.
     *
     * @param domicile the optional (preferred) ISO 3166 alpha-2 country code that represents a target domicile
     * @return the final domicile
     */
    public String getDomicile( @Nullable String domicile )
    {
        if ( domicile == null )
        {
            if ( Strings.isNullOrEmpty( this.domicile ) )
            {
                domicile = DEFAULT_DOMICILE;
                LOGGER.warn( "Using service default locale: " + domicile );
            }
            else
            {
                domicile = this.domicile;
            }
        }
        return domicile.toUpperCase();
    }

    /**
     * Sets the ISO 3166 alpha-2 country code that represents account domicile.
     *
     * @param domicile the domicile to be set
     */
    void setDomicile( String domicile )
    {
        this.domicile = domicile;
    }

    @Override
    public void save()
    {
        ofy().transact( () -> ofy().save().entity( LocalAccount.this ).now() );
    }

    @Override
    public void delete()
    {
        ofy().transact( () -> ofy().delete().entity( LocalAccount.this ).now() );
    }

    @Override
    protected long getModelVersion()
    {
        //21.10.2017 08:00:00 GMT+0200
        return 1508565600000L;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( !( o instanceof LocalAccount ) ) return false;
        LocalAccount that = ( LocalAccount ) o;
        return Objects.equals( this.getId(), that.getId() );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( getId() );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
                .add( "id", getId() )
                .add( "identityId", identityId )
                .add( "audience", audience )
                .add( "email", email )
                .toString();
    }
}
