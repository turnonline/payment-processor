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

import biz.turnonline.ecosystem.payment.api.model.Transaction;
import biz.turnonline.ecosystem.payment.api.model.TransactionBank;
import biz.turnonline.ecosystem.payment.service.BackendServiceTestCase;
import ma.glasnost.orika.MapperFacade;
import org.ctoolkit.agent.service.impl.ImportTask;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Date;

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
        Transaction transaction = mapper.map( new TransactionReceipt( TRANSACTION_EXT_ID ), Transaction.class );
        validateEmpty( transaction );
    }

    @Test
    public void convert_TransactionBill()
    {
        // import test bank accounts and transactions
        ImportTask task = new ImportTask( "/testdataset/changeset_transactions.xml" );
        task.run();

        TransactionReceipt backend = ofy().load().type( TransactionReceipt.class ).id( 680L ).now();

        // test call
        Transaction transaction = mapper.map( backend, Transaction.class );
        validateCommon( transaction );

        assertWithMessage( "Transaction receipt Id" )
                .that( transaction.getBill().getReceipt() )
                .isNotNull();

        assertWithMessage( "Transaction order Id" )
                .that( transaction.getBill().getOrder() )
                .isNull();

        assertWithMessage( "Transaction invoice Id" )
                .that( transaction.getBill().getInvoice() )
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

        double transactionAmount = 0.9;
        String currency = "EUR";
        double transactionBillAmount = 1.0;
        String billCurrency = "USD";

        // exchange rate test date
        ExchangeRate rate = new ExchangeRate();
        rate.from( new ExchangeAmount().amount( transactionBillAmount ).currency( billCurrency ) );
        rate.to( new ExchangeAmount().amount( transactionAmount ).currency( currency ) );
        rate.fee( new ExchangeAmount().amount( 0.0 ).currency( "EUR" ) );
        rate.rate( 0.9 );
        rate.rateDate( new Date() );

        String counterpartyIban = "SK2711001936954420095443";
        String counterpartyBic = "TATRSKBX";
        String counterpartyName = "One Academy, s.r.o.";

        CounterpartyBankAccount counterparty = new CounterpartyBankAccount();
        counterparty.setIban( counterpartyIban );
        counterparty.setBic( counterpartyBic );
        counterparty.setName( counterpartyName );

        TransactionInvoice backend = ofy().load().type( TransactionInvoice.class ).id( 681L ).now();
        backend.setCounterparty( counterparty );
        backend.exchangeRate( rate );
        backend.save();

        // test call
        Transaction transaction = mapper.map( backend, Transaction.class );
        validateCommon( transaction );

        assertWithMessage( "Transaction receipt Id" )
                .that( transaction.getBill().getReceipt() )
                .isNull();

        assertWithMessage( "Transaction order Id" )
                .that( transaction.getBill().getOrder() )
                .isNotNull();

        assertWithMessage( "Transaction invoice Id" )
                .that( transaction.getBill().getInvoice() )
                .isNotNull();

        assertWithMessage( "Transaction type" )
                .that( transaction.getType() )
                .isEqualTo( TRANSFER.name() );

        assertWithMessage( "Transaction exchange rate" )
                .that( transaction.getExchangeRate() )
                .isNotNull();

        assertWithMessage( "Transaction exchange rate value" )
                .that( transaction.getExchangeRate().getRate() )
                .isEqualTo( 0.9 );

        assertWithMessage( "Transaction exchange rate 'from' amount" )
                .that( transaction.getExchangeRate().getFrom().getAmount() )
                .isEqualTo( transactionBillAmount );

        assertWithMessage( "Transaction exchange rate 'from' currency" )
                .that( transaction.getExchangeRate().getFrom().getCurrency() )
                .isEqualTo( billCurrency );

        assertWithMessage( "Transaction exchange rate 'to' amount" )
                .that( transaction.getExchangeRate().getTo().getAmount() )
                .isEqualTo( transactionAmount );

        assertWithMessage( "Transaction exchange rate 'to' currency" )
                .that( transaction.getExchangeRate().getTo().getCurrency() )
                .isEqualTo( currency );

        assertWithMessage( "Transaction exchange rate 'fee' amount" )
                .that( transaction.getExchangeRate().getFee().getAmount() )
                .isEqualTo( 0.0 );

        assertWithMessage( "Transaction exchange rate 'fee' currency" )
                .that( transaction.getExchangeRate().getFee().getCurrency() )
                .isEqualTo( currency );

        assertWithMessage( "Transaction exchange rate date" )
                .that( transaction.getExchangeRate().getRateDate() )
                .isNotNull();

        assertWithMessage( "Transaction counterparty" )
                .that( transaction.getCounterparty() )
                .isNotNull();

        assertWithMessage( "Transaction counterparty IBAN" )
                .that( transaction.getCounterparty().getIban() )
                .isEqualTo( counterpartyIban );

        assertWithMessage( "Transaction counterparty BIC" )
                .that( transaction.getCounterparty().getBic() )
                .isEqualTo( counterpartyBic );

        assertWithMessage( "Transaction counterparty name" )
                .that( transaction.getCounterparty().getName() )
                .isEqualTo( counterpartyName );
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

        assertWithMessage( "Transaction ID" )
                .that( transaction.getTransactionId() )
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

        TransactionBank bank = transaction.getBankAccount();
        assertWithMessage( "Transaction bank code" )
                .that( bank == null ? null : bank.getCode() )
                .isEqualTo( "REVO" );

        assertWithMessage( "Transaction IBAN" )
                .that( transaction.getBankAccount().getIban() )
                .isNotNull();
    }
}