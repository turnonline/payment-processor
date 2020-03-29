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

package biz.turnonline.ecosystem.payment.service.model;

import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Subclass;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Transaction that represents a payment for a bill (statement of charges from cash register etc.).
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Subclass( name = "Bill", index = true )
public class TransactionBill
        extends CommonTransaction
{
    private static final long serialVersionUID = -290298009607479569L;

    @Index
    private Long bill;

    /**
     * Needed if instantiated by objectify.
     */
    @SuppressWarnings( "unused" )
    TransactionBill()
    {
    }

    public TransactionBill( @Nonnull String extId )
    {
        super.externalId( checkNotNull( extId, "The bill external ID can't be null" ) );
    }

    /**
     * Returns the unique identification of the bill.
     *
     * @return the bill Id
     */
    public Long getBillId()
    {
        return bill;
    }

    /**
     * The bill identified by ID to be associated with this transaction.
     *
     * @param id the bill ID to be set
     */
    public void setBillId( Long id )
    {
        this.bill = id;
    }
}
