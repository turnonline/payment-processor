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
import biz.turnonline.ecosystem.payment.service.model.FormOfPayment;
import biz.turnonline.ecosystem.payment.subscription.JsonTask;
import biz.turnonline.ecosystem.revolut.business.transaction.model.Transaction;
import biz.turnonline.ecosystem.revolut.business.transaction.model.TransactionLeg;
import biz.turnonline.ecosystem.revolut.business.transaction.model.TransactionState;
import biz.turnonline.ecosystem.revolut.business.transaction.model.TransactionType;
import com.google.common.base.Strings;
import org.ctoolkit.restapi.client.ClientErrorException;
import org.ctoolkit.restapi.client.NotFoundException;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.restapi.client.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.List;

import static biz.turnonline.ecosystem.payment.service.PaymentConfig.REVOLUT_BANK_CODE;

/**
 * Async task to process Revolut {@link Transaction}.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class TransactionCreatedTask
        extends JsonTask<Transaction>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( TransactionCreatedTask.class );

    private static final long serialVersionUID = 8438319760944126297L;

    transient private PaymentConfig config;

    transient private RestFacade facade;

    /**
     * Constructor.
     *
     * @param json the event JSON payload of type {@link Transaction}
     */
    public TransactionCreatedTask( @Nonnull String json )
    {
        super( json, "Revolut-Transaction" );
    }

    @Override
    protected void execute( @Nonnull Transaction incoming )
    {
        String id = Strings.isNullOrEmpty( incoming.getId() ) ? "" : incoming.getId();

        try
        {
            facade.get( Transaction.class ).identifiedBy( id ).finish();
            LOGGER.info( "Incoming transaction (via webhook) found in bank system too" );
        }
        catch ( ClientErrorException | NotFoundException | UnauthorizedException e )
        {
            LOGGER.error( "Unknown incoming transaction identified by transaction Id: " + id, e );
            return;
        }

        List<TransactionLeg> legs = incoming.getLegs();
        if ( legs == null || legs.isEmpty() )
        {
            LOGGER.warn( "Invalid incoming transaction, it has leg; ID " + id );
            return;
        }

        TransactionLeg leg = legs.get( 0 );

        CommonTransaction transaction = config.createTransaction( id );
        transaction.bankCode( REVOLUT_BANK_CODE )
                .currency( leg.getCurrency() )
                .balance( leg.getBalance() )
                .reference( incoming.getReference() );

        TransactionState state = incoming.getState();
        if ( TransactionState.CREATED == state
                || TransactionState.PENDING == state
                || TransactionState.COMPLETED == state )
        {
            transaction.failure( false );
        }
        else
        {
            transaction.failure( true );
        }

        Double amount = leg.getAmount();
        if ( amount != null && amount > 0 )
        {
            transaction.amount( Math.abs( amount ) ).credit( true );
        }

        if ( amount != null && amount < 0 )
        {
            transaction.amount( Math.abs( amount ) ).credit( false );
        }

        if ( amount == null || amount == 0 )
        {
            transaction.amount( 0.0 ).credit( true );
        }

        if ( TransactionState.COMPLETED.equals( state ) )
        {
            transaction.completedAt( incoming.getUpdatedAt() );
        }

        if ( TransactionType.CARD_PAYMENT.equals( incoming.getType() ) )
        {
            transaction.type( FormOfPayment.CARD_PAYMENT );
        }
        else if ( TransactionType.TRANSFER.equals( incoming.getType() ) )
        {
            transaction.type( FormOfPayment.TRANSFER );
        }
        else if ( TransactionType.REFUND.equals( incoming.getType() ) )
        {
            transaction.type( FormOfPayment.REFUND );
        }

        transaction.addOrigin( incoming );
        transaction.save();
    }

    @Override
    protected Class<Transaction> type()
    {
        return Transaction.class;
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