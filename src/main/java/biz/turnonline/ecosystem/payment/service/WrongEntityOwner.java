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
import com.google.common.annotations.VisibleForTesting;
import org.ctoolkit.services.datastore.objectify.BaseEntityIdentity;
import org.ctoolkit.services.storage.HasOwner;

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
