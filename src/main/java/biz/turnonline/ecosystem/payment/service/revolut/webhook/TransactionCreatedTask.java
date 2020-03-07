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

package biz.turnonline.ecosystem.payment.service.revolut.webhook;

import biz.turnonline.ecosystem.payment.subscription.JsonTask;

import javax.annotation.Nonnull;

/**
 * Async {@link TransactionCreated} event processor.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class TransactionCreatedTask
        extends JsonTask<TransactionCreated>
{
    /**
     * Constructor.
     *
     * @param json the event JSON payload
     */
    public TransactionCreatedTask( @Nonnull String json )
    {
        super( json, "Revolut-Webhook-TransactionCreated" );
    }

    @Override
    protected void execute( @Nonnull TransactionCreated resource )
    {

    }

    @Override
    protected Class<TransactionCreated> type()
    {
        return TransactionCreated.class;
    }
}
