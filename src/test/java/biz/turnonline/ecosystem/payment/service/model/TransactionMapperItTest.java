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

package biz.turnonline.ecosystem.payment.service.model;

import biz.turnonline.ecosystem.payment.api.model.Bank;
import biz.turnonline.ecosystem.payment.api.model.Transaction;
import biz.turnonline.ecosystem.payment.service.BackendServiceTestCase;
import ma.glasnost.orika.MapperFacade;
import org.ctoolkit.agent.service.impl.ImportTask;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static biz.turnonline.ecosystem.payment.service.model.CommonTransaction.State.COMPLETED;
import static biz.turnonline.ecosystem.payment.service.model.FormOfPayment.CARD_PAYMENT;
import static biz.turnonline.ecosystem.payment.service.model.FormOfPayment.TRANSFER;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * {@link TransactionMapper} unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class TransactionMapperItTest
        extends BackendServiceTestCase
{
    public static final String TRANSACTION_EXT_ID = "8a431f07-180b-4ce4-94ba-cd5df3548fb9";

    @Inject
    private MapperFacade mapper;

    @Test
    public void convert_EmptyTransactionBill()
    {
        Transaction transaction = mapper.map( new TransactionBill( TRANSACTION_EXT_ID ), Transaction.class );
        validateEmpty( transaction );
    }

    @Test
    public void convert_TransactionBill()
    {
        // import test bank accounts and transactions
        ImportTask task = new ImportTask( "/testdataset/changeset_transactions.xml" );
        task.run();

        TransactionBill backend = ofy().load().type( TransactionBill.class ).id( 680L ).now();

        // test call
        Transaction transaction = mapper.map( backend, Transaction.class );
        validateCommon( transaction );

        assertWithMessage( "Transaction bill Id" )
                .that( transaction.getBill().getId() )
                .isNotNull();

        assertWithMessage( "Transaction order Id" )
                .that( transaction.getBill().getOrderId() )
                .isNull();

        assertWithMessage( "Transaction invoice Id" )
                .that( transaction.getBill().getInvoiceId() )
                .isNull();

        assertWithMessage( "Transaction type" )
                .that( transaction.getType() )
                .isEqualTo( CARD_PAYMENT.name() );
    }

    @Test
    public void convert_EmptyTransactionInvoice()
    {
        Transaction transaction = mapper.map( new TransactionInvoice(), Transaction.class );
        validateEmpty( transaction );
    }

    @Test
    public void convert_TransactionInvoice()
    {
        // import test bank accounts and transactions
        ImportTask task = new ImportTask( "/testdataset/changeset_transactions.xml" );
        task.run();

        TransactionInvoice backend = ofy().load().type( TransactionInvoice.class ).id( 681L ).now();

        // test call
        Transaction transaction = mapper.map( backend, Transaction.class );
        validateCommon( transaction );

        assertWithMessage( "Transaction bill Id" )
                .that( transaction.getBill().getId() )
                .isNull();

        assertWithMessage( "Transaction order Id" )
                .that( transaction.getBill().getOrderId() )
                .isNotNull();

        assertWithMessage( "Transaction invoice Id" )
                .that( transaction.getBill().getInvoiceId() )
                .isNotNull();

        assertWithMessage( "Transaction type" )
                .that( transaction.getType() )
                .isEqualTo( TRANSFER.name() );
    }

    private void validateEmpty( Transaction transaction )
    {
        assertWithMessage( "Transaction" )
                .that( transaction )
                .isNotNull();

        assertWithMessage( "Transaction amount" )
                .that( transaction.getAmount() )
                .isNull();

        assertWithMessage( "Transaction balance" )
                .that( transaction.getBalance() )
                .isNull();

        assertWithMessage( "Transaction bankAccount" )
                .that( transaction.getBankAccount() )
                .isNull();

        assertWithMessage( "Transaction bill" )
                .that( transaction.getBill() )
                .isNull();

        assertWithMessage( "Transaction completedAt" )
                .that( transaction.getCompletedAt() )
                .isNull();

        assertWithMessage( "Transaction credit" )
                .that( transaction.isCredit() )
                .isFalse();

        assertWithMessage( "Transaction currency" )
                .that( transaction.getCurrency() )
                .isNull();

        assertWithMessage( "Transaction reference" )
                .that( transaction.getReference() )
                .isNull();

        assertWithMessage( "Transaction key" )
                .that( transaction.getKey() )
                .isNull();

        assertWithMessage( "Transaction status" )
                .that( transaction.getStatus() )
                .isNull();

        assertWithMessage( "Transaction type" )
                .that( transaction.getType() )
                .isNull();
    }

    private void validateCommon( Transaction transaction )
    {
        assertWithMessage( "Transaction" )
                .that( transaction )
                .isNotNull();

        assertWithMessage( "Transaction amount" )
                .that( transaction.getAmount() )
                .isNotNull();

        assertWithMessage( "Transaction balance" )
                .that( transaction.getBalance() )
                .isNotNull();

        assertWithMessage( "Transaction bill" )
                .that( transaction.getBill() )
                .isNotNull();

        assertWithMessage( "Transaction completedAt" )
                .that( transaction.getCompletedAt() )
                .isNotNull();

        assertWithMessage( "Transaction credit" )
                .that( transaction.isCredit() )
                .isFalse();

        assertWithMessage( "Transaction currency" )
                .that( transaction.getCurrency() )
                .isEqualTo( "EUR" );

        assertWithMessage( "Transaction reference" )
                .that( transaction.getReference() )
                .isNotNull();

        assertWithMessage( "Transaction key" )
                .that( transaction.getKey() )
                .isNotNull();

        assertWithMessage( "Transaction status" )
                .that( transaction.getStatus() )
                .isEqualTo( COMPLETED.name() );

        assertWithMessage( "Transaction bankAccount" )
                .that( transaction.getBankAccount() )
                .isNotNull();

        Bank bank = transaction.getBankAccount().getBank();
        assertWithMessage( "Transaction bank account, bank code" )
                .that( bank == null ? null : bank.getCode() )
                .isEqualTo( "REVO" );

        assertWithMessage( "Transaction bank account Id" )
                .that( transaction.getBankAccount().getId() )
                .isNotNull();
    }
}