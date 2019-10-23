package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import com.google.common.base.MoreObjects;
import org.ctoolkit.restapi.client.NotFoundException;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The dedicated provider to handle local and remote account.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public interface LocalAccountProvider
{
    /**
     * Returns the local lightweight account entity instance identified by email and its audience.
     *
     * @param email    the login email address of the account
     * @param audience the account audience unique identification
     * @return the local lightweight account instance
     */
    LocalAccount get( @Nonnull String email, @Nonnull String audience );

    /**
     * Returns the associated local lightweight account entity instance. It might act as an owner of an entities.
     * <p>
     * {@link LocalAccount} instance accessed for the first time, then it will be stored in datastore
     * with updated values taken from the remote account.
     *
     * @param builder all builder properties are mandatory
     * @return the local lightweight account
     * @throws NotFoundException if remote account has not been found
     */
    LocalAccount initGet( @Nonnull Builder builder );

    class Builder
    {
        public String identityId;

        public String email;

        public String audience;

        /**
         * Sets the user account unique identification within login provider system.
         */
        public Builder identityId( @Nonnull String identityId )
        {
            this.identityId = checkNotNull( identityId, "Identity ID is mandatory" );
            return this;
        }

        /**
         * Sets the login email address of the account
         */
        public Builder email( @Nonnull String email )
        {
            this.email = checkNotNull( email, "Email is mandatory" );
            return this;
        }

        /**
         * Sets the account audience unique identification.
         */
        public Builder audience( @Nonnull String audience )
        {
            this.audience = checkNotNull( audience, "Audience is mandatory" );
            return this;
        }

        @Override
        public String toString()
        {
            MoreObjects.ToStringHelper string = MoreObjects.toStringHelper( "Builder" );
            string.add( "email", email )
                    .add( "identityId", identityId )
                    .add( "audience", audience );

            return string.toString();
        }
    }
}
