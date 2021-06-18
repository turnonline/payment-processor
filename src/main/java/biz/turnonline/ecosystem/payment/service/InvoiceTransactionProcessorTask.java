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

import biz.turnonline.ecosystem.billing.model.BillPayment;
import biz.turnonline.ecosystem.billing.model.Customer;
import biz.turnonline.ecosystem.billing.model.Invoice;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.CounterpartyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.payment.service.model.TransactionInvoice;
import biz.turnonline.ecosystem.payment.subscription.JsonAccountTask;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * Async creation of the {@link TransactionInvoice} that will act as a placeholder for payment transaction
 * that will settle provided invoice.
 * <p>
 * All these conditions are expected to be true:
 * <ul>
 *     <li>{@link Invoice#getStatus()} is SENT</li>
 *     <li>{@link Invoice#getCustomer()} is defined</li>
 *     <li>Payment instruction at invoice is defined and has one of the value defined,
 *     either {@link BillPayment#getVariableSymbol()} or {@link BillPayment#getKey()}.
 *     It will be used to match the incoming payment transaction.</li>
 * </ul>
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class InvoiceTransactionProcessorTask
        extends JsonAccountTask<Invoice>
{
    private static final long serialVersionUID = 1380384988027524233L;

    private static final Logger LOGGER = LoggerFactory.getLogger( InvoiceTransactionProcessorTask.class );

    private final long orderId;

    private final long invoiceId;

    @Inject
    transient private PaymentConfig config;

    /**
     * Constructor.
     *
     * @param creditor  the local account as a creditor of the invoice
     * @param json      the invoice JSON payload
     * @param orderId   the invoice's parent order identification
     * @param invoiceId the invoice identification
     */
    public InvoiceTransactionProcessorTask( @Nonnull LocalAccount creditor,
                                            @Nonnull String json,
                                            long orderId,
                                            long invoiceId )
    {
        super( creditor.entityKey(), json, false, "Invoice-Transaction-Processing" );
        this.orderId = orderId;
        this.invoiceId = invoiceId;
    }

    @Override
    protected void execute( @Nonnull LocalAccount account, @Nonnull Invoice invoice )
    {
        if ( !"SENT".equals( invoice.getStatus() ) )
        {
            LOGGER.info( "Only SENT Invoice will be processed (incoming "
                    + invoice.getStatus()
                    + "), Invoice identification "
                    + invoiceId( orderId, invoiceId ) );
            return;
        }

        Customer customer = invoice.getCustomer();
        if ( customer == null )
        {
            LOGGER.warn( "Incomplete invoice, missing customer. Invoice identification "
                    + invoiceId( orderId, invoiceId ) );
            return;
        }

        BillPayment payment = invoice.getPayment();
        String paymentKey;
        if ( payment != null )
        {
            Long vs = payment.getVariableSymbol();
            paymentKey = vs == null ? payment.getKey() : String.valueOf( vs );
        }
        else
        {
            paymentKey = null;
        }

        if ( Strings.isNullOrEmpty( paymentKey ) )
        {
            paymentKey = invoice.getInvoiceNumber();
        }

        if ( Strings.isNullOrEmpty( paymentKey ) )
        {
            LOGGER.warn( "Invoice identified by "
                    + invoiceId( orderId, invoiceId )
                    + " is missing payment key or variable symbol. It's mandatory in order to match with payment." );
            return;
        }

        CommonTransaction transaction = config.initGetTransactionDraft( orderId, invoiceId );

        if ( !Strings.isNullOrEmpty( customer.getBusinessName() ) )
        {
            CounterpartyBankAccount counterparty = new CounterpartyBankAccount();
            counterparty.setName( customer.getBusinessName() );
            transaction.setCounterparty( counterparty );
        }

        transaction.failure( false )
                .key( paymentKey )
                .status( CommonTransaction.State.CREATED );

        transaction.save();

        LOGGER.info( "Transaction placeholder for invoice identified by "
                + invoiceId( orderId, invoiceId )
                + " has done - "
                + transaction.entityKey() );
        LOGGER.info( " Number of transactions: " + config.countTransactionInvoice( orderId, invoiceId ) );
    }

    private String invoiceId( long orderId, long invoiceId )
    {
        return "Order:" + orderId + "::Invoice:" + invoiceId;
    }

    @Override
    protected Class<Invoice> type()
    {
        return Invoice.class;
    }
}
