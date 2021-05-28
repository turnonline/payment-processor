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
import biz.turnonline.ecosystem.steward.model.Account;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import org.testng.annotations.Test;

import java.time.OffsetDateTime;

/**
 * {@link InvoiceTransactionDeletionTask} unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class InvoiceTransactionDeletionTaskTest
{
    @Injectable
    private final LocalAccount account = new LocalAccount( new Account()
            .setId( 585628009L )
            .setEmail( "my.account@turnonline.biz" )
            .setIdentityId( "85PKlVi" )
            .setAudience( "b2b" ) );

    @Injectable
    private final long orderId = 13;

    @Injectable
    private final long invoiceId = 1314;

    @Tested
    private InvoiceTransactionDeletionTask tested;

    @Injectable
    private PaymentConfig config;

    @Test
    public void processed()
    {
        TransactionInvoice transaction = new TransactionInvoice( orderId, invoiceId );
        transaction.status( CommonTransaction.State.CREATED ).completedAt( null );

        new Expectations( transaction )
        {
            {
                config.countTransactionInvoice( orderId, invoiceId );
                result = 1;

                config.initGetTransactionDraft( orderId, invoiceId );
                result = transaction;

                transaction.delete();
            }
        };

        tested.execute();
    }

    @Test
    public void ignored_NotFound()
    {
        new Expectations()
        {
            {
                config.countTransactionInvoice( orderId, invoiceId );
                result = 0;
            }
        };

        tested.execute();

        new Verifications()
        {
            {
                config.initGetTransactionDraft( anyLong, anyLong );
                times = 0;
            }
        };
    }

    @Test
    public void ignored_Transaction_COMPLETED()
    {
        TransactionInvoice transaction = new TransactionInvoice( orderId, invoiceId );
        transaction.status( CommonTransaction.State.COMPLETED );

        new Expectations( transaction )
        {
            {
                config.countTransactionInvoice( orderId, invoiceId );
                result = 1;

                config.initGetTransactionDraft( orderId, invoiceId );
                result = transaction;

                transaction.delete();
                times = 0;
            }
        };

        tested.execute();
    }

    @Test
    public void ignored_Transaction_CompletedAt()
    {
        TransactionInvoice transaction = new TransactionInvoice( orderId, invoiceId );
        transaction.completedAt( OffsetDateTime.now() );

        new Expectations( transaction )
        {
            {
                config.countTransactionInvoice( orderId, invoiceId );
                result = 1;

                config.initGetTransactionDraft( orderId, invoiceId );
                result = transaction;

                transaction.delete();
                times = 0;
            }
        };

        tested.execute();
    }
}