package biz.turnonline.ecosystem.payment.service.model;

import biz.turnonline.ecosystem.account.client.model.Account;
import biz.turnonline.ecosystem.account.client.model.AccountBusiness;
import biz.turnonline.ecosystem.payment.service.NoRetryException;
import com.google.common.base.MoreObjects;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Ignore;
import org.ctoolkit.restapi.client.NotFoundException;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.services.storage.appengine.objectify.EntityStringIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * The datastore local account acts as an owner of the entities associated with an account.
 * It represents the TurnOnline.biz account taken from the Account Steward microservice.
 * The 'identityId' property is being treated as an entity ID.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Cache( expirationSeconds = 3600 )
@Entity( name = "PP_LocalAccount" )
public class LocalAccount
        extends EntityStringIdentity
{
    private static final Logger logger = LoggerFactory.getLogger( LocalAccount.class );

    private static final long serialVersionUID = 437608003749056818L;

    @Ignore
    private transient RestFacade facade;

    @Ignore
    private transient Account tAccount;

    private String email;

    @Inject
    LocalAccount( RestFacade facade )
    {
        this.facade = facade;
    }

    LocalAccount( @Nonnull String identityId, @Nonnull String email, @Nonnull Account account )
    {
        super.setId( checkNotNull( identityId, "Identity ID is mandatory." ) );
        this.email = checkNotNull( email, "Account email is mandatory." );
        this.tAccount = checkNotNull( account );
    }

    public String getIdentityId()
    {
        return super.getId();
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail( String email )
    {
        this.email = email;
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
            tAccount = facade.get( Account.class ).identifiedBy( getEmail() ).finish();
        }
        catch ( NotFoundException e )
        {
            logger.warn( "The remote account with email: '" + getEmail() + "' not found." );
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

    @Override
    public void save()
    {
        ofy().transact( () -> {
            ofy().save().entity( LocalAccount.this ).now();
        } );
    }

    @Override
    public void delete()
    {
        ofy().transact( () -> {
            ofy().delete().entity( LocalAccount.this ).now();
        } );

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
        return Objects.equals( this.getIdentityId(), that.getIdentityId() );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( email, this.getIdentityId() );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
                .add( "identityId", getIdentityId() )
                .add( "email", email )
                .toString();
    }
}
