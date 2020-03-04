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
 * Transaction that represents a payment for an invoice.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Subclass( name = "Invoice", index = true )
public class TransactionInvoice
        extends Transaction
{
    private static final long serialVersionUID = 7396725695386439571L;

    @Index
    private Long orderId;

    @Index
    private Long invoiceId;

    public TransactionInvoice( @Nonnull Long orderId, @Nonnull Long invoiceId )
    {
        this.orderId = checkNotNull( orderId, "Incoming invoice's order ID can't be null" );
        this.invoiceId = checkNotNull( invoiceId, "Incoming invoice ID can't be null" );
    }

    /**
     * Returns the unique identification of the order.
     *
     * @return the order Id
     */
    public Long getOrderId()
    {
        return orderId;
    }

    /**
     * Returns the identification of the invoice, unique only for associated order.
     *
     * @return the invoice Id
     */
    public Long getInvoiceId()
    {
        return invoiceId;
    }
}
