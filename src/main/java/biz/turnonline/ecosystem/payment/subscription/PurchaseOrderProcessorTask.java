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

package biz.turnonline.ecosystem.payment.subscription;

import biz.turnonline.ecosystem.billing.model.PurchaseOrder;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import com.googlecode.objectify.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * The asynchronous task to process purchase order.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
class PurchaseOrderProcessorTask
        extends JsonTask<PurchaseOrder>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( PurchaseOrderProcessorTask.class );

    private static final long serialVersionUID = 2155479255175862359L;

    PurchaseOrderProcessorTask( @Nonnull Key<LocalAccount> accountKey, @Nonnull String json, boolean delete )
    {
        super( accountKey, json, delete );
    }

    @Override
    protected void execute( @Nonnull LocalAccount debtor, @Nonnull PurchaseOrder order )
    {

    }

    @Override
    protected Class<PurchaseOrder> type()
    {
        return PurchaseOrder.class;
    }
}
