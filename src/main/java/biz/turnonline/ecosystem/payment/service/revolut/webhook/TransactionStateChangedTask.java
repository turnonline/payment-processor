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

import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.subscription.JsonTask;
import biz.turnonline.ecosystem.revolut.business.transaction.model.Transaction;
import biz.turnonline.ecosystem.revolut.business.transaction.model.TransactionState;
import org.ctoolkit.restapi.client.ClientErrorException;
import org.ctoolkit.restapi.client.NotFoundException;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.restapi.client.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.UUID;

import static biz.turnonline.ecosystem.payment.service.model.CommonTransaction.State.COMPLETED;

/**
 * Async {@link TransactionStateChanged} event processor.
 * <p>
 * Incoming transaction Id is being used to compare {@link Transaction#getState()} from the bank.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class TransactionStateChangedTask
        extends JsonTask<TransactionStateChanged>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( TransactionStateChangedTask.class );

    private static final long serialVersionUID = 8264148597336503259L;

    transient private PaymentConfig config;

    transient private RestFacade facade;

    /**
     * Constructor.
     *
     * @param json the event JSON payload of type {@link TransactionStateChanged}
     */
    public TransactionStateChangedTask( @Nonnull String json )
    {
        super( json, "Revolut-Webhook-StateChanged" );
    }

    @Override
    protected void execute( @Nonnull TransactionStateChanged resource )
    {
        TransactionStateChangedData incoming = resource.getData();
        if ( incoming == null )
        {
            LOGGER.warn( "Invalid incoming transaction status change, it has no data " + resource.getTimestamp() );
            return;
        }

        UUID id = incoming.getId();
        if ( id == null )
        {
            LOGGER.warn( "Invalid incoming transaction status change, it has no ID " + resource.getTimestamp() );
            return;
        }

        Transaction transactionFromBank;
        try
        {
            transactionFromBank = facade.get( Transaction.class ).identifiedBy( id.toString() ).finish();
            LOGGER.info( "Incoming transaction status change (via webhook) found in bank system too" );
        }
        catch ( ClientErrorException | NotFoundException | UnauthorizedException e )
        {
            LOGGER.error( "Unknown incoming transaction identified by transaction Id: " + id, e );
            return;
        }

        // if transaction not found an exception will be thrown in order to handle retry
        // (because of eventual consistency)
        CommonTransaction transaction = config.searchTransaction( id.toString() );
        TransactionState state = transactionFromBank.getState();

        if ( state == null || !state.getValue().equals( incoming.getNewState() ) )
        {
            LOGGER.error( "Mismatched state, incoming " + resource );
            return;
        }

        if ( TransactionState.COMPLETED.getValue().equals( incoming.getNewState() )
                && transaction.getCompletedAt() == null
                && !transaction.isFailure() )
        {
            // update only if transaction is not yet marked as completed
            transaction.completedAt( resource.getTimestamp() );
            transaction.status( COMPLETED );
            transaction.addOrigin( incoming );
            transaction.save();
        }
    }

    @Override
    protected Class<TransactionStateChanged> type()
    {
        return TransactionStateChanged.class;
    }

    @Inject
    void setConfig( PaymentConfig config )
    {
        this.config = config;
    }

    @Inject
    void setFacade( RestFacade facade )
    {
        this.facade = facade;
    }
}