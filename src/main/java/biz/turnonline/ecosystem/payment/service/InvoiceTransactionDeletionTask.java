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
import org.ctoolkit.services.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import static biz.turnonline.ecosystem.payment.service.model.CommonTransaction.State;

/**
 * Async deletion of an unfinished {@link TransactionInvoice} if exist.
 * At least one condition must meet the criteria:
 * <ul>
 *     <li>Transaction is not {@link State#COMPLETED}</li>
 *     <li>{@link TransactionInvoice#getCompletedAt()} returns {@code null}</li>
 * </ul>
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class InvoiceTransactionDeletionTask
        extends Task<LocalAccount>
{
    private static final long serialVersionUID = 6506707575391770728L;

    private static final Logger LOGGER = LoggerFactory.getLogger( InvoiceTransactionDeletionTask.class );

    private final long orderId;

    private final long invoiceId;

    @Inject
    transient private PaymentConfig config;

    /**
     * Constructor.
     *
     * @param creditor  the key of a local account as a creditor of the invoice
     * @param orderId   the invoice's parent order identification
     * @param invoiceId the invoice identification
     */
    public InvoiceTransactionDeletionTask( @Nonnull LocalAccount creditor,
                                           long orderId,
                                           long invoiceId )
    {
        super( "Invoice-Transaction-Deletion" );
        setEntityKey( creditor.entityKey() );
        this.orderId = orderId;
        this.invoiceId = invoiceId;
    }

    @Override
    protected void execute()
    {
        int numberOf = config.countTransactionInvoice( orderId, invoiceId );
        if ( numberOf == 0 )
        {
            LOGGER.info( "No transaction found for " + invoiceId( orderId, invoiceId ) );
            return;
        }
        else
        {
            LOGGER.info( "Number of transaction records for "
                    + invoiceId( orderId, invoiceId )
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
        LOGGER.info( "Placeholder transaction identified by "
                + invoiceId( orderId, invoiceId )
                + " has been deleted" );
    }

    private String invoiceId( long orderId, long invoiceId )
    {
        return "Order:" + orderId + "::Invoice:" + invoiceId;
    }
}
