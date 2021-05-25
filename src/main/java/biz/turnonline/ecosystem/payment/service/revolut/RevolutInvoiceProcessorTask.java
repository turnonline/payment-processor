/*
 * Copyright (c) 2021 TurnOnline.biz s.r.o. All Rights Reserved.
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

package biz.turnonline.ecosystem.payment.service.revolut;

import biz.turnonline.ecosystem.billing.model.Invoice;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.payment.subscription.JsonAccountTask;
import com.googlecode.objectify.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class RevolutInvoiceProcessorTask
        extends JsonAccountTask<Invoice>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( RevolutInvoiceProcessorTask.class );

    private final Key<CommonTransaction> transactionKey;

    /**
     * Constructor.
     *
     * @param accountKey the key of a local account as an owner of the payload
     * @param json       the JSON payload
     * @param t          the transaction draft
     */
    public RevolutInvoiceProcessorTask( @Nonnull Key<LocalAccount> accountKey,
                                        @Nonnull String json,
                                        @Nonnull CommonTransaction t )
    {
        super( accountKey, json, false, "Revolut-Invoice-Processing" );
        this.transactionKey = checkNotNull( t.entityKey(), "Transaction draft's key can't be null" );
    }

    @Override
    protected void execute( @Nonnull LocalAccount account, @Nonnull Invoice invoice )
    {
        if ( !"SENT".equalsIgnoreCase( invoice.getStatus() ) )
        {
            LOGGER.info( "Only SENT Invoice will be processed, Invoice.ID " + invoice.getId() );
            return;
        }

        CommonTransaction transaction = getTransactionDraft();
        if ( transaction == null )
        {
            LOGGER.warn( "Transaction draft not found for " + transactionKey );
            return;
        }
    }

    @Override
    protected Class<Invoice> type()
    {
        return Invoice.class;
    }

    CommonTransaction getTransactionDraft()
    {
        return ofy().load().key( transactionKey ).now();
    }
}
