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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Thrown if transaction not found.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class TransactionNotFound
        extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    private final String id;

    public TransactionNotFound( String id )
    {
        this.id = checkNotNull( id, "Id can't be null" );
    }

    /**
     * Returns the identification of the transaction that has not been found.
     *
     * @return the Id
     */
    public String getId()
    {
        return id;
    }
}
