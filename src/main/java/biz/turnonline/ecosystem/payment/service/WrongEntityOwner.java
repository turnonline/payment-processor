package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import com.google.common.annotations.VisibleForTesting;
import org.ctoolkit.services.storage.HasOwner;
import org.ctoolkit.services.storage.appengine.objectify.BaseEntityIdentity;

import javax.annotation.Nonnull;

/**
 * Thrown to indicate that authenticated account tries to manipulate with an entity
 * that's being owned by another account.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class WrongEntityOwner
        extends IllegalArgumentException
{
    private static final long serialVersionUID = -9041146219305052477L;

    private LocalAccount owner;

    @SuppressWarnings( "NonSerializableFieldInSerializableClass" )
    private HasOwner<LocalAccount> entity;

    @VisibleForTesting
    public WrongEntityOwner()
    {
    }

    WrongEntityOwner( @Nonnull LocalAccount account, @Nonnull HasOwner<LocalAccount> entity )
    {
        super( "The authenticated account with email '"
                + account.getEmail()
                + "' ("
                + account.getIdentityId()
                + ")"
                + " is trying to work with "
                + ( ( entity instanceof BaseEntityIdentity )
                ? ( ( BaseEntityIdentity ) entity ).entityKey()
                : entity.getClass().getSimpleName() )
                + " entity that has a different owner '"
                + entity.getOwner().getEmail()
                + "' ("
                + entity.getOwner().getIdentityId()
                + ")." );

        this.owner = account;
        this.entity = entity;
    }

    /**
     * Returns the current authenticated account.
     *
     * @return the authenticated account
     */
    public LocalAccount getOwner()
    {
        return owner;
    }

    /**
     * Returns the current authenticated account email.
     *
     * @return the authenticated account email
     */
    public String getEmail()
    {
        return owner.getEmail();
    }

    /**
     * The entity tried to be manipulated with wrong owner.
     *
     * @return the entity
     */
    public HasOwner<LocalAccount> getEntity()
    {
        return entity;
    }

    /**
     * The entity (readable key identification) tried to be manipulated with wrong owner.
     *
     * @return the entity readable key identification
     */
    public String getEntityKey()
    {
        if ( entity instanceof BaseEntityIdentity )
        {
            return ( ( BaseEntityIdentity ) entity ).entityKey().toString();
        }
        else
        {
            return entity.getClass().getSimpleName();
        }
    }
}
