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
import biz.turnonline.ecosystem.payment.service.model.LocalDeputyAccount;
import org.ctoolkit.restapi.client.pubsub.PubsubCommand;

import javax.annotation.Nonnull;

/**
 * The dedicated provider to handle local and remote account.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public interface LocalAccountProvider
{
    /**
     * Returns the account associated with this service.
     * <p>
     * It's a design concept. It's a single account (tenant) associated with the payment service.
     *
     * @return the account associated with the service
     */
    LocalAccount get();

    /**
     * Returns the associated deputy account entity instance or {@code null} if not found
     *
     * @param email the email address of the deputy account
     * @return the local deputy account
     */
    LocalDeputyAccount get( @Nonnull String email );

    /**
     * Returns the account associated with this service if matches the account coming via Pub/Sub.
     * If not found or does not match it will returns {@code null}.
     * <p>
     * A new local account will be created for incoming in case there is no local account record yet.
     * It's a design concept. It's a single account (tenant) associated with the payment service.
     *
     * @return the account associated with the service
     */
    LocalAccount check( @Nonnull PubsubCommand command );
}
