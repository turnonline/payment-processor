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

import biz.turnonline.ecosystem.payment.service.BackendServiceTestCase;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.FormOfPayment;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.payment.service.model.TransactionBill;
import biz.turnonline.ecosystem.payment.subscription.MockedInputStream;
import biz.turnonline.ecosystem.revolut.business.transaction.model.Transaction;
import biz.turnonline.ecosystem.revolut.business.transaction.model.TransactionState;
import biz.turnonline.ecosystem.revolut.business.transaction.model.TransactionType;
import biz.turnonline.ecosystem.steward.model.Account;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import org.ctoolkit.agent.service.impl.ImportTask;
import org.ctoolkit.restapi.client.ClientErrorException;
import org.ctoolkit.restapi.client.NotFoundException;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.restapi.client.UnauthorizedException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.stream.Collectors;

import static biz.turnonline.ecosystem.payment.service.PaymentConfig.REVOLUT_BANK_CODE;
import static biz.turnonline.ecosystem.payment.service.model.CommonTransaction.State.COMPLETED;
import static biz.turnonline.ecosystem.payment.service.model.CommonTransaction.State.FAILED;
import static biz.turnonline.ecosystem.payment.service.model.CommonTransaction.State.PENDING;
import static biz.turnonline.ecosystem.revolut.business.transaction.model.TransactionType.CARD_PAYMENT;
import static biz.turnonline.ecosystem.revolut.business.transaction.model.TransactionType.REFUND;
import static biz.turnonline.ecosystem.revolut.business.transaction.model.TransactionType.TRANSFER;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * {@link TransactionCreatedTask} and {@link TransactionStateChangedTask} unit testing against emulated datastore.
 * <p>
 * Use case, where the transaction is being created first then its state will be changed to completed.
 * </p>
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class TransactionCreatedFlowTest
        extends BackendServiceTestCase
{
    public static final String TRANSACTION_EXT_ID = "0dfaec58-6043-11ea-bc55-0242ac130003";

    static final String BANK_ACCOUNT_EXT_ID = "bdab1c20-8d8c-430d-b967-87ac01af060c";

    static ObjectMapper mapper = new ObjectMapper()
            .disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES )
            .registerModule( new JavaTimeModule() );

    private TransactionCreatedTask created;

    @Tested
    private TransactionStateChangedTask stateChanged;

    @Injectable
    private String json;

    @Injectable
    @Inject
    private PaymentConfig config;

    @Injectable
    private RestFacade facade;

    static String toJson( String fileName )
    {
        InputStream stream = MockedInputStream.class.getResourceAsStream( fileName );
        return new BufferedReader( new InputStreamReader( stream ) )
                .lines()
                .collect( Collectors.joining( System.lineSeparator() ) );
    }

    @BeforeMethod
    public void before()
    {
        stateChanged = new TransactionStateChangedTask( toJson( "transaction-state-changed.json" ) );
    }

    /**
     * Example of transaction for card payment.
     */
    @Test
    public void successful_CARD_PAYMENT() throws JsonProcessingException
    {
        // in the real use case transaction is being created by subscription webhook
        config.initGetTransaction( TRANSACTION_EXT_ID );

        String json = toJsonCreated( CARD_PAYMENT.getValue() );
        created = new TransactionCreatedTask( json );
        created.setConfig( config );
        created.setFacade( facade );

        // mocking of the transaction from remote bank system
        Transaction t = mapper.readValue( json, Transaction.class );
        Transaction afterStateChanged = mapper.readValue( json, Transaction.class );
        afterStateChanged.setState( TransactionState.fromValue( stateChanged.workWith().getData().getNewState() ) );

        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( TRANSACTION_EXT_ID ).finish();
                result = t;
                result = afterStateChanged;
            }
        };

        // test call
        created.execute();

        CommonTransaction transaction = ofy().load().type( CommonTransaction.class ).first().now();
        verifyTransactionBasics( transaction );

        assertWithMessage( "Transaction reference" )
                .that( transaction.getReference() )
                .isNull();

        assertWithMessage( "Transaction amount" )
                .that( transaction.getAmount() )
                .isEqualTo( 2.0 );

        assertWithMessage( "Transaction related account Id" )
                .that( transaction.getBankAccountKey() )
                .isNull();

        assertWithMessage( "Balance after transaction" )
                .that( transaction.getBalance() )
                .isNull();

        assertWithMessage( "Transaction pending" )
                .that( transaction.getCompletedAt() )
                .isNull();

        assertWithMessage( "Transaction credit" )
                .that( transaction.isCredit() )
                .isFalse();

        assertWithMessage( "Transaction status" )
                .that( transaction.getStatus() )
                .isEqualTo( PENDING );

        assertWithMessage( "Transaction type" )
                .that( transaction.getType() )
                .isEqualTo( FormOfPayment.CARD_PAYMENT );

        assertWithMessage( "Transaction failure" )
                .that( transaction.isFailure() )
                .isFalse();

        Date modificationDate = transaction.getModificationDate();

        stateChanged.execute();
        ofy().clear();

        int count = ofy().load().type( TransactionBill.class ).count();
        assertWithMessage( "Final number of transactions" )
                .that( count )
                .isEqualTo( 1 );

        transaction = ofy().load().type( CommonTransaction.class ).first().now();
        assertWithMessage( "Transaction status changed, completed at" )
                .that( transaction.getCompletedAt() )
                .isNotNull();

        assertWithMessage( "Transaction status changed" )
                .that( transaction.getStatus() )
                .isEqualTo( COMPLETED );

        assertWithMessage( "Transaction status changed, origins" )
                .that( transaction.getOrigins() )
                .hasSize( 2 );

        assertWithMessage( "Transaction modification date (modification expected)" )
                .that( transaction.getModificationDate() )
                .isGreaterThan( modificationDate );
    }

    /**
     * Example of transaction for card payment, the incoming. Some kind of hack, data payload is different (except id)
     * comparing the transaction from the bank.
     * The {@link Transaction} from the bank takes precedence, only the ID is being used from the incoming transaction.
     */
    @Test
    public void successful_CARD_PAYMENT_OverTRANSFER() throws JsonProcessingException
    {
        // in the real use case transaction is being created by subscription webhook
        config.initGetTransaction( TRANSACTION_EXT_ID );

        String json = toJsonCreated( TRANSFER.getValue() );
        created = new TransactionCreatedTask( json );
        created.setConfig( config );
        created.setFacade( facade );

        // mocking of the transaction from remote bank system
        Transaction t = mapper.readValue( toJsonCreated( CARD_PAYMENT.getValue() ), Transaction.class );
        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( TRANSACTION_EXT_ID ).finish();
                result = t;
            }
        };

        // test call
        created.execute();

        CommonTransaction transaction = ofy().load().type( CommonTransaction.class ).first().now();
        verifyTransactionBasics( transaction );

        assertWithMessage( "Transaction type" )
                .that( transaction.getType() )
                .isEqualTo( FormOfPayment.CARD_PAYMENT );
    }

    /**
     * Example of transaction for card payment with cross currency.
     * The {@link Transaction} from the bank takes precedence, only the ID is being used from the incoming transaction.
     */
    @Test
    public void successful_CARD_PAYMENT_CrossCurrency() throws JsonProcessingException
    {
        // in the real use case transaction is being created by subscription webhook
        config.initGetTransaction( TRANSACTION_EXT_ID );

        String json = toJsonCreated( CARD_PAYMENT.getValue() + "-cross-currency" );
        created = new TransactionCreatedTask( json );
        created.setConfig( config );
        created.setFacade( facade );

        // mocking of the transaction from remote bank system
        Transaction t = mapper.readValue( json, Transaction.class );

        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( TRANSACTION_EXT_ID ).finish();
                result = t;
            }
        };

        // test call
        created.execute();

        CommonTransaction transaction = ofy().load().type( CommonTransaction.class ).first().now();
        verifyTransactionBasics( transaction );

        assertWithMessage( "Transaction reference" )
                .that( transaction.getReference() )
                .isNull();

        assertWithMessage( "Transaction amount" )
                .that( transaction.getAmount() )
                .isEqualTo( 0.9 );

        assertWithMessage( "Transaction related account Id" )
                .that( transaction.getBankAccountKey() )
                .isNull();

        assertWithMessage( "Balance after transaction" )
                .that( transaction.getBalance() )
                .isNull();

        assertWithMessage( "Transaction pending" )
                .that( transaction.getCompletedAt() )
                .isNull();

        assertWithMessage( "Transaction credit" )
                .that( transaction.isCredit() )
                .isFalse();

        assertWithMessage( "Transaction status" )
                .that( transaction.getStatus() )
                .isEqualTo( PENDING );

        assertWithMessage( "Transaction type" )
                .that( transaction.getType() )
                .isEqualTo( FormOfPayment.CARD_PAYMENT );

        assertWithMessage( "Transaction failure" )
                .that( transaction.isFailure() )
                .isFalse();
    }

    /**
     * Example of transaction for internal transfer between your accounts:
     */
    @Test
    public void successful_TRANSFER_Internal() throws JsonProcessingException
    {
        // in the real use case transaction is being created by subscription webhook
        config.initGetTransaction( TRANSACTION_EXT_ID );

        String json = toJsonCreated( TRANSFER.getValue() + "-internal" );
        created = new TransactionCreatedTask( json );
        created.setConfig( config );
        created.setFacade( facade );

        // mocking of the transaction from remote bank system
        Transaction t = mapper.readValue( json, Transaction.class );
        Transaction afterStateChanged = mapper.readValue( json, Transaction.class );
        afterStateChanged.setState( TransactionState.fromValue( stateChanged.workWith().getData().getNewState() ) );

        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( TRANSACTION_EXT_ID ).finish();
                result = t;
                result = afterStateChanged;
            }
        };

        // test call
        created.execute();

        CommonTransaction transaction = ofy().load().type( CommonTransaction.class ).first().now();
        verifyTransactionBasics( transaction );

        assertWithMessage( "Transaction reference" )
                .that( transaction.getReference() )
                .isEqualTo( "Expenses funding" );

        assertWithMessage( "Transaction amount" )
                .that( transaction.getAmount() )
                .isEqualTo( 123.11 );

        assertWithMessage( "Transaction related account Id" )
                .that( transaction.getBankAccountKey() )
                .isNull();

        assertWithMessage( "Balance after transaction" )
                .that( transaction.getBalance() )
                .isEqualTo( 0.0 );

        assertWithMessage( "Transaction completed at" )
                .that( transaction.getCompletedAt() )
                .isNotNull();

        assertWithMessage( "Transaction credit" )
                .that( transaction.isCredit() )
                .isFalse();

        assertWithMessage( "Transaction status" )
                .that( transaction.getStatus() )
                .isEqualTo( COMPLETED );

        assertWithMessage( "Transaction type" )
                .that( transaction.getType() )
                .isEqualTo( FormOfPayment.TRANSFER );

        assertWithMessage( "Transaction failure" )
                .that( transaction.isFailure() )
                .isFalse();

        Date modificationDate = transaction.getModificationDate();

        stateChanged.execute();
        ofy().clear();

        transaction = ofy().load().type( CommonTransaction.class ).first().now();
        int count = ofy().load().type( TransactionBill.class ).count();

        assertWithMessage( "Final number of transactions" )
                .that( count )
                .isEqualTo( 1 );

        assertWithMessage( "Transaction status changed" )
                .that( transaction.getStatus() )
                .isEqualTo( COMPLETED );

        assertWithMessage( "Transaction status changed, origins" )
                .that( transaction.getOrigins() )
                .hasSize( 1 );

        assertWithMessage( "Transaction modification date (modification not expected)" )
                .that( transaction.getModificationDate() )
                .isEquivalentAccordingToCompareTo( modificationDate );
    }

    /**
     * Example of transaction for a payment to another Revolut business/user.
     */
    @Test
    public void successful_TRANSFER_External() throws JsonProcessingException
    {
        // in the real use case transaction is being created by subscription webhook
        config.initGetTransaction( TRANSACTION_EXT_ID );

        String json = toJsonCreated( TRANSFER.getValue() );
        created = new TransactionCreatedTask( json );
        created.setConfig( config );
        created.setFacade( facade );

        // mocking of the transaction from remote bank system
        Transaction t = mapper.readValue( json, Transaction.class );
        Transaction afterStateChanged = mapper.readValue( json, Transaction.class );
        afterStateChanged.setState( TransactionState.fromValue( stateChanged.workWith().getData().getNewState() ) );

        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( TRANSACTION_EXT_ID ).finish();
                result = t;
                result = afterStateChanged;
            }
        };

        // test call
        created.execute();

        CommonTransaction transaction = ofy().load().type( CommonTransaction.class ).first().now();
        verifyTransactionBasics( transaction );

        assertWithMessage( "Transaction reference" )
                .that( transaction.getReference() )
                .isEqualTo( "Payment for Blows & Wistles Co." );

        assertWithMessage( "Transaction amount" )
                .that( transaction.getAmount() )
                .isEqualTo( 0.0 );

        assertWithMessage( "Transaction related account Id" )
                .that( transaction.getBankAccountKey() )
                .isNull();

        assertWithMessage( "Balance after transaction" )
                .that( transaction.getBalance() )
                .isEqualTo( 100 );

        assertWithMessage( "Transaction completed at" )
                .that( transaction.getCompletedAt() )
                .isNotNull();

        assertWithMessage( "Transaction credit" )
                .that( transaction.isCredit() )
                .isTrue();

        assertWithMessage( "Transaction status" )
                .that( transaction.getStatus() )
                .isEqualTo( COMPLETED );

        assertWithMessage( "Transaction type" )
                .that( transaction.getType() )
                .isEqualTo( FormOfPayment.TRANSFER );

        assertWithMessage( "Transaction failure" )
                .that( transaction.isFailure() )
                .isFalse();

        Date modificationDate = transaction.getModificationDate();

        stateChanged.execute();
        ofy().clear();

        transaction = ofy().load().type( CommonTransaction.class ).first().now();
        int count = ofy().load().type( TransactionBill.class ).count();

        assertWithMessage( "Final number of transactions" )
                .that( count )
                .isEqualTo( 1 );

        assertWithMessage( "Transaction status changed" )
                .that( transaction.getStatus() )
                .isEqualTo( COMPLETED );

        assertWithMessage( "Transaction status changed, origins" )
                .that( transaction.getOrigins() )
                .hasSize( 1 );

        assertWithMessage( "Transaction modification date (modification not expected)" )
                .that( transaction.getModificationDate() )
                .isEquivalentAccordingToCompareTo( modificationDate );
    }

    /**
     * Example of transaction for a cross-currency payment to another Revolut business/user.
     */
    @Test
    public void successful_TRANSFER_ExternalCrossCurrency() throws JsonProcessingException
    {
        // in the real use case transaction is being created by subscription webhook
        config.initGetTransaction( TRANSACTION_EXT_ID );

        String json = toJsonCreated( TRANSFER.getValue() + "-cross-currency" );
        created = new TransactionCreatedTask( json );
        created.setConfig( config );
        created.setFacade( facade );

        // mocking of the transaction from remote bank system
        Transaction t = mapper.readValue( json, Transaction.class );
        Transaction afterStateChanged = mapper.readValue( json, Transaction.class );
        afterStateChanged.setState( TransactionState.fromValue( stateChanged.workWith().getData().getNewState() ) );

        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( TRANSACTION_EXT_ID ).finish();
                result = t;
                result = afterStateChanged;
            }
        };

        // test call
        created.execute();

        CommonTransaction transaction = ofy().load().type( CommonTransaction.class ).first().now();
        verifyTransactionBasics( transaction );

        assertWithMessage( "Transaction reference" )
                .that( transaction.getReference() )
                .isEqualTo( "Payment for Blows & Wistles Co." );

        assertWithMessage( "Transaction amount" )
                .that( transaction.getAmount() )
                .isEqualTo( 123.11 );

        assertWithMessage( "Transaction related account Id" )
                .that( transaction.getBankAccountKey() )
                .isNull();

        assertWithMessage( "Balance after transaction" )
                .that( transaction.getBalance() )
                .isEqualTo( 22.5 );

        assertWithMessage( "Transaction completed at" )
                .that( transaction.getCompletedAt() )
                .isNotNull();

        assertWithMessage( "Transaction credit" )
                .that( transaction.isCredit() )
                .isFalse();

        assertWithMessage( "Transaction status" )
                .that( transaction.getStatus() )
                .isEqualTo( COMPLETED );

        assertWithMessage( "Transaction type" )
                .that( transaction.getType() )
                .isEqualTo( FormOfPayment.TRANSFER );

        assertWithMessage( "Transaction failure" )
                .that( transaction.isFailure() )
                .isFalse();

        Date modificationDate = transaction.getModificationDate();

        stateChanged.execute();
        ofy().clear();

        transaction = ofy().load().type( CommonTransaction.class ).first().now();
        int count = ofy().load().type( TransactionBill.class ).count();

        assertWithMessage( "Final number of transactions" )
                .that( count )
                .isEqualTo( 1 );

        assertWithMessage( "Transaction status changed" )
                .that( transaction.getStatus() )
                .isEqualTo( COMPLETED );

        assertWithMessage( "Transaction status changed, origins" )
                .that( transaction.getOrigins() )
                .hasSize( 1 );

        assertWithMessage( "Transaction modification date (modification not expected)" )
                .that( transaction.getModificationDate() )
                .isEquivalentAccordingToCompareTo( modificationDate );
    }

    /**
     * Example of transaction for a payment to external counterparty.
     */
    @Test
    public void successful_TRANSFER_ExternalNonRevolut() throws JsonProcessingException
    {
        LocalAccount account = new LocalAccount( genericJsonFromFile( "account.json", Account.class ) );

        // import test bank accounts
        ImportTask task = new ImportTask( "/testdataset/changeset_00001.xml" );
        task.run();

        CompanyBankAccount primaryBankAccount = config.getPrimaryBankAccount( account, null );
        // set bank account external Id taken from transaction-created-transfer-non-revolut.json
        primaryBankAccount.setExternalId( BANK_ACCOUNT_EXT_ID );
        primaryBankAccount.save();

        // in the real use case transaction is being created by subscription webhook
        config.initGetTransaction( TRANSACTION_EXT_ID );

        String json = toJsonCreated( TRANSFER.getValue() + "-non-revolut" );
        created = new TransactionCreatedTask( json );
        created.setConfig( config );
        created.setFacade( facade );

        // mocking of the transaction from remote bank system
        Transaction t = mapper.readValue( json, Transaction.class );
        Transaction afterStateChanged = mapper.readValue( json, Transaction.class );
        afterStateChanged.setState( TransactionState.fromValue( stateChanged.workWith().getData().getNewState() ) );

        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( TRANSACTION_EXT_ID ).finish();
                result = t;
                result = afterStateChanged;
            }
        };

        // test call
        created.execute();

        CommonTransaction transaction = ofy().load().type( CommonTransaction.class ).first().now();
        verifyTransactionBasics( transaction );

        assertWithMessage( "Transaction reference" )
                .that( transaction.getReference() )
                .isEqualTo( "Payment for Blows & Wistles Co." );

        assertWithMessage( "Transaction related account Id" )
                .that( transaction.getBankAccountKey() )
                .isEqualTo( primaryBankAccount.entityKey() );

        assertWithMessage( "Transaction amount" )
                .that( transaction.getAmount() )
                .isEqualTo( 119.19 );

        assertWithMessage( "Balance after transaction" )
                .that( transaction.getBalance() )
                .isEqualTo( 10.0 );

        assertWithMessage( "Transaction pending" )
                .that( transaction.getCompletedAt() )
                .isNull();

        assertWithMessage( "Transaction credit" )
                .that( transaction.isCredit() )
                .isFalse();

        assertWithMessage( "Transaction status" )
                .that( transaction.getStatus() )
                .isEqualTo( PENDING );

        assertWithMessage( "Transaction type" )
                .that( transaction.getType() )
                .isEqualTo( FormOfPayment.TRANSFER );

        assertWithMessage( "Transaction failure" )
                .that( transaction.isFailure() )
                .isFalse();

        Date modificationDate = transaction.getModificationDate();

        stateChanged.execute();
        ofy().clear();

        transaction = ofy().load().type( CommonTransaction.class ).first().now();
        int count = ofy().load().type( TransactionBill.class ).count();

        assertWithMessage( "Final number of transactions" )
                .that( count )
                .isEqualTo( 1 );

        assertWithMessage( "Transaction status changed" )
                .that( transaction.getStatus() )
                .isEqualTo( COMPLETED );

        assertWithMessage( "Transaction status changed, origins" )
                .that( transaction.getOrigins() )
                .hasSize( 2 );

        assertWithMessage( "Transaction modification date (modification expected)" )
                .that( transaction.getModificationDate() )
                .isGreaterThan( modificationDate );
    }

    /**
     * Example of transaction for a failed transfer.
     * Testing whether status change to 'completed' will be ignored.
     */
    @Test
    public void successful_TRANSFER_Failed() throws JsonProcessingException
    {
        // in the real use case transaction is being created by subscription webhook
        config.initGetTransaction( TRANSACTION_EXT_ID );

        String json = toJsonCreated( TRANSFER.getValue() + "-failed" );
        created = new TransactionCreatedTask( json );
        created.setConfig( config );
        created.setFacade( facade );

        // mocking of the transaction from remote bank system
        Transaction t = mapper.readValue( json, Transaction.class );
        Transaction afterStateChanged = mapper.readValue( json, Transaction.class );
        afterStateChanged.setState( TransactionState.fromValue( stateChanged.workWith().getData().getNewState() ) );

        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( TRANSACTION_EXT_ID ).finish();
                result = t;
                result = afterStateChanged;
            }
        };

        // test call
        created.execute();

        CommonTransaction transaction = ofy().load().type( CommonTransaction.class ).first().now();
        verifyTransactionBasics( transaction );

        assertWithMessage( "Transaction reference" )
                .that( transaction.getReference() )
                .isEqualTo( "Payment for Blows & Wistles Co." );

        assertWithMessage( "Transaction amount" )
                .that( transaction.getAmount() )
                .isEqualTo( 99.22 );

        assertWithMessage( "Transaction related account Id" )
                .that( transaction.getBankAccountKey() )
                .isNull();

        assertWithMessage( "Balance after transaction" )
                .that( transaction.getBalance() )
                .isNull();

        assertWithMessage( "Transaction pending" )
                .that( transaction.getCompletedAt() )
                .isNull();

        assertWithMessage( "Transaction credit" )
                .that( transaction.isCredit() )
                .isFalse();

        assertWithMessage( "Transaction status" )
                .that( transaction.getStatus() )
                .isEqualTo( FAILED );

        assertWithMessage( "Transaction type" )
                .that( transaction.getType() )
                .isEqualTo( FormOfPayment.TRANSFER );

        assertWithMessage( "Transaction failure" )
                .that( transaction.isFailure() )
                .isTrue();

        Date modificationDate = transaction.getModificationDate();

        stateChanged.execute();
        ofy().clear();

        transaction = ofy().load().type( CommonTransaction.class ).first().now();
        int count = ofy().load().type( TransactionBill.class ).count();

        assertWithMessage( "Final number of transactions" )
                .that( count )
                .isEqualTo( 1 );

        assertWithMessage( "Transaction status" )
                .that( transaction.getStatus() )
                .isEqualTo( FAILED );

        assertWithMessage( "Transaction status changed, origins" )
                .that( transaction.getOrigins() )
                .hasSize( 1 );

        assertWithMessage( "Transaction modification date (modification not expected)" )
                .that( transaction.getModificationDate() )
                .isEquivalentAccordingToCompareTo( modificationDate );
    }

    /**
     * Example of transaction for a failed transfer.
     * Testing whether status change to 'completed' will be ignored.
     */
    @Test
    public void successful_REFUND() throws JsonProcessingException
    {
        // in the real use case transaction is being created by subscription webhook
        config.initGetTransaction( TRANSACTION_EXT_ID );

        String json = toJsonCreated( REFUND.getValue() );
        created = new TransactionCreatedTask( json );
        created.setConfig( config );
        created.setFacade( facade );

        // mocking of the transaction from remote bank system
        Transaction t = mapper.readValue( json, Transaction.class );
        Transaction afterStateChanged = mapper.readValue( json, Transaction.class );
        afterStateChanged.setState( TransactionState.fromValue( stateChanged.workWith().getData().getNewState() ) );

        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( TRANSACTION_EXT_ID ).finish();
                result = t;
                result = afterStateChanged;
            }
        };

        // test call
        created.execute();

        CommonTransaction transaction = ofy().load().type( CommonTransaction.class ).first().now();
        verifyTransactionBasics( transaction );

        assertWithMessage( "Transaction reference" )
                .that( transaction.getReference() )
                .isNull();

        assertWithMessage( "Transaction amount" )
                .that( transaction.getAmount() )
                .isEqualTo( 15.00 );

        assertWithMessage( "Transaction related account Id" )
                .that( transaction.getBankAccountKey() )
                .isNull();

        assertWithMessage( "Balance after transaction" )
                .that( transaction.getBalance() )
                .isEqualTo( 30.00 );

        assertWithMessage( "Transaction completed at" )
                .that( transaction.getCompletedAt() )
                .isNotNull();

        assertWithMessage( "Transaction credit" )
                .that( transaction.isCredit() )
                .isTrue();

        assertWithMessage( "Transaction status" )
                .that( transaction.getStatus() )
                .isEqualTo( COMPLETED );

        assertWithMessage( "Transaction type" )
                .that( transaction.getType() )
                .isEqualTo( FormOfPayment.REFUND );

        assertWithMessage( "Transaction failure" )
                .that( transaction.isFailure() )
                .isFalse();

        Date modificationDate = transaction.getModificationDate();

        stateChanged.execute();
        ofy().clear();

        transaction = ofy().load().type( CommonTransaction.class ).first().now();
        int count = ofy().load().type( TransactionBill.class ).count();

        assertWithMessage( "Final number of transactions" )
                .that( count )
                .isEqualTo( 1 );

        assertWithMessage( "Transaction status changed" )
                .that( transaction.getStatus() )
                .isEqualTo( COMPLETED );

        assertWithMessage( "Transaction status changed, origins" )
                .that( transaction.getOrigins() )
                .hasSize( 1 );

        assertWithMessage( "Transaction modification date (modification not expected)" )
                .that( transaction.getModificationDate() )
                .isEquivalentAccordingToCompareTo( modificationDate );
    }

    @Test
    public void unsuccessful_TransactionNotFound()
    {
        created = new TransactionCreatedTask( toJsonCreated( TRANSFER.getValue() ) );
        created.setConfig( config );
        created.setFacade( facade );

        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( anyString ).finish();
                result = new NotFoundException();
            }
        };

        // test call
        created.execute();

        int count = ofy().load().type( TransactionBill.class ).count();
        assertWithMessage( "Final number of transactions" )
                .that( count )
                .isEqualTo( 0 );
    }

    @Test
    public void unsuccessful_RevolutClientError()
    {
        created = new TransactionCreatedTask( toJsonCreated( TRANSFER.getValue() ) );
        created.setConfig( config );
        created.setFacade( facade );

        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( TRANSACTION_EXT_ID ).finish();
                result = new ClientErrorException();
            }
        };

        // test call
        created.execute();

        int count = ofy().load().type( TransactionBill.class ).count();
        assertWithMessage( "Final number of transactions" )
                .that( count )
                .isEqualTo( 0 );
    }

    @Test
    public void unsuccessful_RevolutUnauthorized()
    {
        created = new TransactionCreatedTask( toJsonCreated( TRANSFER.getValue() ) );
        created.setConfig( config );
        created.setFacade( facade );

        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( anyString ).finish();
                result = new UnauthorizedException();
            }
        };

        // test call
        created.execute();

        int count = ofy().load().type( TransactionBill.class ).count();
        assertWithMessage( "Final number of transactions" )
                .that( count )
                .isEqualTo( 0 );
    }

    @Test
    public void unsuccessful_MissingLegs() throws JsonProcessingException
    {
        String json = toJsonCreated( "missing-legs" );
        created = new TransactionCreatedTask( json );
        created.setConfig( config );
        created.setFacade( facade );

        // mocking of the transaction from remote bank system
        Transaction t = mapper.readValue( json, Transaction.class );
        Transaction afterStateChanged = mapper.readValue( json, Transaction.class );
        afterStateChanged.setState( TransactionState.fromValue( stateChanged.workWith().getData().getNewState() ) );

        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( TRANSACTION_EXT_ID ).finish();
                result = t;
                result = afterStateChanged;
            }
        };

        // test call
        created.execute();

        int count = ofy().load().type( TransactionBill.class ).count();
        assertWithMessage( "Final number of transactions" )
                .that( count )
                .isEqualTo( 0 );
    }

    /**
     * Verify those properties that are common.
     *
     * @param transaction to be verified
     */
    private void verifyTransactionBasics( CommonTransaction transaction )
    {
        assertWithMessage( "Transaction" )
                .that( transaction )
                .isNotNull();

        assertWithMessage( "Transaction external Id" )
                .that( transaction.getExternalId() )
                .isEqualTo( TRANSACTION_EXT_ID );

        assertWithMessage( "Transaction bank code" )
                .that( transaction.getBankCode() )
                .isEqualTo( REVOLUT_BANK_CODE );

        assertWithMessage( "Transaction currency" )
                .that( transaction.getCurrency() )
                .isEqualTo( "EUR" );

        assertWithMessage( "Transaction origins" )
                .that( transaction.getOrigins() )
                .hasSize( 1 );
    }

    /**
     * Picks up only data (transaction) part.
     *
     * @param paymentType the corresponding JSON file with expected type of the payment
     * @see TransactionType
     */
    private String toJsonCreated( String paymentType )
    {
        String fileName = "transaction-created-" + paymentType + ".json";
        InputStream stream = MockedInputStream.class.getResourceAsStream( fileName );
        JsonElement data = JsonParser.parseReader( new InputStreamReader( stream ) ).getAsJsonObject().get( "data" );
        return data.toString();
    }
}