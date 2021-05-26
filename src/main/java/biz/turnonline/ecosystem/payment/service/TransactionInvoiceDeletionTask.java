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

package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.payment.service.model.TransactionInvoice;
import com.googlecode.objectify.Key;
import org.ctoolkit.services.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import static biz.turnonline.ecosystem.payment.service.model.CommonTransaction.State;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Async deletion of an unfinished {@link TransactionInvoice} if exist.
 * At least one condition must meet the criteria:
 * <ul>
 *     <li>Transaction cannot be {@link State#COMPLETED}</li>
 *     <li>{@link TransactionInvoice#getCompletedAt()} returns {@code null}</li>
 * </ul>
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class TransactionInvoiceDeletionTask
        extends Task<LocalAccount>
{
    private static final long serialVersionUID = 8997518182085550497L;

    private static final Logger LOGGER = LoggerFactory.getLogger( TransactionInvoiceDeletionTask.class );

    private final long orderId;

    private final long invoiceId;

    @Inject
    transient private PaymentConfig config;

    /**
     * Constructor.
     *
     * @param creditor the key of a local account as a creditor of the invoice
     */
    public TransactionInvoiceDeletionTask( @Nonnull Key<LocalAccount> creditor,
                                           @Nonnull Long orderId,
                                           @Nonnull Long invoiceId )
    {
        super( "Revolut-Invoice-Processing" );
        setEntityKey( creditor );
        this.orderId = checkNotNull( orderId, "Order ID can't be null" );
        this.invoiceId = checkNotNull( invoiceId, "Invoice ID can't be null" );
    }

    @Override
    protected void execute()
    {
        int numberOf = config.countTransactionInvoice( orderId, invoiceId );
        if ( numberOf == 0 )
        {
            LOGGER.info( "No transaction found for Order ID: " + orderId + ", Invoice ID: " + invoiceId );
            return;
        }
        else
        {
            LOGGER.info( "Number of transaction records for Order ID: "
                    + orderId
                    + ", Invoice ID: "
                    + invoiceId
                    + ", is "
                    + numberOf );
        }

        CommonTransaction transaction = config.initGetTransactionDraft( orderId, invoiceId );
        if ( transaction.getCompletedAt() != null
                || State.COMPLETED.equals( transaction.getStatus() ) )
        {
            LOGGER.info( transaction.entityKey() + " already completed" );
            return;
        }

        transaction.delete();
        LOGGER.info( "Transaction identified by Order ID: "
                + orderId
                + ", Invoice ID: "
                + invoiceId
                + " has been deleted" );
    }
}
