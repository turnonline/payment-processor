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

package biz.turnonline.ecosystem.payment.subscription;

import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.TransactionNotFound;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.TransactionReceipt;
import com.google.api.services.pubsub.model.PubsubMessage;
import org.ctoolkit.restapi.client.NotFoundException;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.restapi.client.pubsub.PubsubCommand;
import org.ctoolkit.restapi.client.pubsub.PubsubMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Optional;

import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.DATA_TYPE;

/**
 * The 'bill.changes' subscription listener implementation.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
class BillingProcessorChangesSubscription
        implements PubsubMessageListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger( BillingProcessorChangesSubscription.class );

    private static final long serialVersionUID = 5590828228043735446L;

    private final RestFacade facade;

    private final PaymentConfig paymentConfig;

    @Inject
    BillingProcessorChangesSubscription( RestFacade facade, PaymentConfig paymentConfig )
    {
        this.facade = facade;
        this.paymentConfig = paymentConfig;
    }

    @Override
    public void onMessage( @Nonnull PubsubMessage message, @Nonnull String subscription ) throws Exception
    {
        PubsubCommand command = new PubsubCommand( message );

        String[] mandatory = {
                DATA_TYPE
        };
        if ( !command.validate( mandatory ) )
        {
            LOGGER.error( "Some of the mandatory attributes "
                    + Arrays.toString( mandatory )
                    + " are missing, incoming attributes: "
                    + message.getAttributes() );
            return;
        }

        String dataType = command.getDataType();
        LOGGER.info( "Received data type: " + dataType );
        LOGGER.info( command.getData() );

        if ( dataType.equals( "Bill" ) )
        {
            biz.turnonline.ecosystem.bill.model.Bill bill = command.fromData( biz.turnonline.ecosystem.bill.model.Bill.class );
            findProductBillingTransaction( bill )
                    .flatMap( this::findPaymentTransaction )
                    .ifPresent( transactionReceipt -> updateTransactionReceipt( transactionReceipt, bill ) );
        }
    }

    private void updateTransactionReceipt( TransactionReceipt transactionReceipt, biz.turnonline.ecosystem.bill.model.Bill bill )
    {
        transactionReceipt.setReceipt( bill.getId() );
        transactionReceipt.save();
    }

    private Optional<biz.turnonline.ecosystem.billing.model.Transaction> findProductBillingTransaction( biz.turnonline.ecosystem.bill.model.Bill bill )
    {
        Long productBillingTransactionId = bill.getTransactionId();

        try
        {
            return Optional.ofNullable(
                    facade.get( biz.turnonline.ecosystem.billing.model.Transaction.class )
                            .identifiedBy( productBillingTransactionId )
                            .finish()
            );
        }
        catch ( NotFoundException e )
        {
            LOGGER.info( "Transaction not found in product billing for id: " + productBillingTransactionId );
        }

        return Optional.empty();
    }

    private Optional<TransactionReceipt> findPaymentTransaction( biz.turnonline.ecosystem.billing.model.Transaction productBillingTransaction )
    {
        Long transactionId = productBillingTransaction.getTransactionId();

        try
        {
            CommonTransaction commonTransaction = paymentConfig.getTransaction( transactionId );
            if ( commonTransaction instanceof TransactionReceipt )
            {
                return Optional.of( ( TransactionReceipt ) commonTransaction );
            }

            LOGGER.info( "Transaction was found but it was not expected type: '{}', instead it was type of: '{}'",
                    TransactionReceipt.class.getName(), commonTransaction.getClass().getName() );
        }
        catch ( TransactionNotFound e )
        {
            LOGGER.info( "Transaction not found in payment processor for id: " + transactionId );
        }

        return Optional.empty();
    }
}