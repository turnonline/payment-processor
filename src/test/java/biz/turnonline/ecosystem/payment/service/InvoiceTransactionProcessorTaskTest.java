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

import biz.turnonline.ecosystem.billing.model.Invoice;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.payment.service.model.TransactionInvoice;
import biz.turnonline.ecosystem.steward.model.Account;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import org.testng.annotations.Test;

import static biz.turnonline.ecosystem.payment.service.BackendServiceTestCase.genericJsonFromFile;
import static biz.turnonline.ecosystem.payment.service.model.CommonTransaction.State.CREATED;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * {@link InvoiceTransactionProcessorTask} unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class InvoiceTransactionProcessorTaskTest
{
    @Injectable
    private final LocalAccount account = new LocalAccount( new Account()
            .setId( 7828249L )
            .setEmail( "my.account@turnonline.biz" )
            .setIdentityId( "87zT6vTTxyvYi" )
            .setAudience( "b2m" ) );

    @Injectable
    private final String json = "{}";

    @Injectable
    private final long orderId = 12;

    @Injectable
    private final long invoiceId = 1213;

    @Injectable
    private PaymentConfig config;

    @Tested
    private InvoiceTransactionProcessorTask tested;

    @Test
    public void processed_InvoiceSent_VS()
    {
        TransactionInvoice transaction = new TransactionInvoice( orderId, invoiceId );

        new Expectations( transaction )
        {
            {
                config.initGetTransactionDraft( orderId, invoiceId );
                result = transaction;

                transaction.save();
            }
        };

        Invoice invoice = genericJsonFromFile( "invoice-sent-pubsub.json", Invoice.class );
        tested.execute( account, invoice );

        assertWithMessage( "Transaction status" )
                .that( transaction.getStatus() )
                .isEqualTo( CREATED );

        assertWithMessage( "Transaction failure" )
                .that( transaction.isFailure() )
                .isFalse();

        assertWithMessage( "Transaction key" )
                .that( transaction.getKey() )
                .isEqualTo( "100342021" );

        assertWithMessage( "Transaction reference (invoice number)" )
                .that( transaction.getReference() )
                .isEqualTo( "VF1-0034/2021" );

        assertWithMessage( "Transaction counterparty" )
                .that( transaction.getCounterparty() )
                .isNotNull();

        assertWithMessage( "Transaction counterparty business name" )
                .that( transaction.getCounterparty().getName() )
                .isEqualTo( "External Ltd." );
    }

    @Test
    public void processed_InvoiceSent_Key()
    {
        TransactionInvoice transaction = new TransactionInvoice( orderId, invoiceId );

        new Expectations( transaction )
        {
            {
                config.initGetTransactionDraft( orderId, invoiceId );
                result = transaction;

                transaction.save();
            }
        };

        Invoice invoice = genericJsonFromFile( "invoice-sent-pubsub.json", Invoice.class );
        // reset VS, payment key will be used instead
        invoice.getPayment().setVariableSymbol( null );

        tested.execute( account, invoice );

        assertWithMessage( "Transaction key" )
                .that( transaction.getKey() )
                .isEqualTo( "key-100342021" );
    }

    @Test
    public void ignored_InvoicePaid()
    {
        Invoice invoice = genericJsonFromFile( "invoice-paid-pubsub.json", Invoice.class );
        tested.execute( account, invoice );

        new Verifications()
        {
            {
                config.initGetTransactionDraft( anyLong, anyLong );
                times = 0;
            }
        };
    }

    @Test
    public void ignored_MissingCustomer()
    {
        Invoice invoice = genericJsonFromFile( "invoice-sent-pubsub.json", Invoice.class );
        invoice.setCustomer( null );

        tested.execute( account, invoice );

        new Verifications()
        {
            {
                config.initGetTransactionDraft( anyLong, anyLong );
                times = 0;
            }
        };
    }

    @Test
    public void ignored_MissingPayment()
    {
        Invoice invoice = genericJsonFromFile( "invoice-sent-pubsub.json", Invoice.class );
        invoice.setPayment( null );

        tested.execute( account, invoice );

        new Verifications()
        {
            {
                config.initGetTransactionDraft( anyLong, anyLong );
                times = 0;
            }
        };
    }

    @Test
    public void ignored_MissingPaymentKey()
    {
        Invoice invoice = genericJsonFromFile( "invoice-sent-pubsub.json", Invoice.class );
        invoice.getPayment().setVariableSymbol( null );
        invoice.getPayment().setKey( null );

        tested.execute( account, invoice );

        new Verifications()
        {
            {
                config.initGetTransactionDraft( anyLong, anyLong );
                times = 0;
            }
        };
    }
}