/*
 * Copyright (c) 2020 TurnOnline.biz s.r.o. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

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
     * Returns the local lightweight account entity instance identified by email.
     *
     * @param email the login email address of the account
     * @return the local lightweight account instance
     */
    LocalAccount get( @Nonnull String email );

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
        private Long accountId;

        private String identityId;

        private String email;

        /**
         * Returns the account unique identification within TurnOnline.biz Ecosystem.
         */
        public Long getAccountId()
        {
            return accountId;
        }

        /**
         * Returns the user account unique identification within login provider system.
         */
        public String getIdentityId()
        {
            return identityId;
        }

        /**
         * Returns the login email address of the account.
         */
        public String getEmail()
        {
            return email;
        }

        /**
         * Sets the account unique identification within TurnOnline.biz Ecosystem.
         */
        public Builder accountId( @Nonnull Long accountId )
        {
            this.accountId = accountId;
            return this;
        }

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

        @Override
        public String toString()
        {
            MoreObjects.ToStringHelper string = MoreObjects.toStringHelper( "Builder" );
            string.add( "Account ID", accountId )
                    .add( "email", email )
                    .add( "identityId", identityId );

            return string.toString();
        }
    }
}
