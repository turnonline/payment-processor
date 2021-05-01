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
import biz.turnonline.ecosystem.payment.service.CategoryService;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.FormOfPayment;
import biz.turnonline.ecosystem.payment.service.model.TransactionReceipt;
import biz.turnonline.ecosystem.payment.subscription.MockedInputStream;
import biz.turnonline.ecosystem.revolut.business.account.model.AccountBankDetailsItem;
import biz.turnonline.ecosystem.revolut.business.counterparty.model.Counterparty;
import biz.turnonline.ecosystem.revolut.business.counterparty.model.CounterpartyAccount;
import biz.turnonline.ecosystem.revolut.business.transaction.model.Transaction;
import biz.turnonline.ecosystem.revolut.business.transaction.model.TransactionState;
import biz.turnonline.ecosystem.revolut.business.transaction.model.TransactionType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import org.ctoolkit.agent.service.impl.ImportTask;
import org.ctoolkit.restapi.client.ClientErrorException;
import org.ctoolkit.restapi.client.NotFoundException;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.restapi.client.UnauthorizedException;
import org.ctoolkit.services.task.Task;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

import static biz.turnonline.ecosystem.payment.service.PaymentConfig.REVOLUT_BANK_EU_CODE;
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

    public static final String TRANSACTION_CURRENCY = "EUR";

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

    @Injectable
    @Inject
    private CategoryService categoryService;

    @Mocked
    private Task<?> task;

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
        created.setCategoryService( categoryService );

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

        assertWithMessage( "Transaction bill amount" )
                .that( transaction.getBillAmount() )
                .isNull();

        assertWithMessage( "Transaction bill currency" )
                .that( transaction.getBillCurrency() )
                .isNull();

        assertWithMessage( "Transaction related account Id" )
                .that( transaction.getBankAccountKey() )
                .isNull();

        assertWithMessage( "Balance after transaction" )
                .that( transaction.getBalance() )
                .isNull();

        assertWithMessage( "Transaction currency" )
                .that( transaction.getCurrency() )
                .isEqualTo( TRANSACTION_CURRENCY );

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

        assertWithMessage( "Transaction type (Receipt)" )
                .that( transaction )
                .isInstanceOf( TransactionReceipt.class );

        assertWithMessage( "Transaction merchant name" )
                .that( ( ( TransactionReceipt ) transaction ).getMerchantName() )
                .isEqualTo( "Best, Ltd." );

        assertWithMessage( "Transaction merchant category" )
                .that( ( ( TransactionReceipt ) transaction ).getCategory() )
                .isEqualTo( "7523" );

        assertWithMessage( "Transaction merchant city" )
                .that( ( ( TransactionReceipt ) transaction ).getCity() )
                .isEqualTo( "Bratislava" );

        Date modificationDate = transaction.getModificationDate();

        stateChanged.execute();
        ofy().clear();

        int count = ofy().load().type( TransactionReceipt.class ).count();
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
        created.setCategoryService( categoryService );

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
        created.setCategoryService( categoryService );

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

        assertWithMessage( "Transaction bill amount" )
                .that( transaction.getBillAmount() )
                .isEqualTo( 1.0 );

        assertWithMessage( "Transaction bill currency" )
                .that( transaction.getBillCurrency() )
                .isEqualTo( "USD" );

        assertWithMessage( "Transaction related account Id" )
                .that( transaction.getBankAccountKey() )
                .isNull();

        assertWithMessage( "Balance after transaction" )
                .that( transaction.getBalance() )
                .isNull();

        assertWithMessage( "Transaction currency" )
                .that( transaction.getCurrency() )
                .isEqualTo( TRANSACTION_CURRENCY );

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

        assertWithMessage( "Transaction type (Receipt)" )
                .that( transaction )
                .isInstanceOf( TransactionReceipt.class );

        assertWithMessage( "Transaction merchant name" )
                .that( ( ( TransactionReceipt ) transaction ).getMerchantName() )
                .isEqualTo( "Pty Ltd" );

        assertWithMessage( "Transaction merchant category" )
                .that( ( ( TransactionReceipt ) transaction ).getCategory() )
                .isEqualTo( "7399" );

        assertWithMessage( "Transaction merchant city" )
                .that( ( ( TransactionReceipt ) transaction ).getCity() )
                .isEqualTo( "Killara" );
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
        created.setCategoryService( categoryService );

        // mocking of the transaction from remote bank system
        Transaction t = mapper.readValue( json, Transaction.class );
        Transaction afterStateChanged = mapper.readValue( json, Transaction.class );
        afterStateChanged.setState( TransactionState.fromValue( stateChanged.workWith().getData().getNewState() ) );

        AccountBankDetailsItem accountBankDetailsItem = new AccountBankDetailsItem();
        accountBankDetailsItem.setIban( "SK1234567890" );
        accountBankDetailsItem.setBic( "SLSPSK" );

        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( TRANSACTION_EXT_ID ).finish();
                result = t;
                result = afterStateChanged;

                // FIXME
//                facade.list(AccountBankDetailsItem.class, new Identifier( "544a8a74-1412-408e-b7db-51c6acac6e98" )).finish();
//                result = Collections.singletonList( accountBankDetailsItem );
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

        assertWithMessage( "Transaction bill amount" )
                .that( transaction.getBillAmount() )
                .isNull();

        assertWithMessage( "Transaction bill currency" )
                .that( transaction.getBillCurrency() )
                .isNull();

        assertWithMessage( "Transaction related account Id" )
                .that( transaction.getBankAccountKey() )
                .isNull();

        assertWithMessage( "Balance after transaction" )
                .that( transaction.getBalance() )
                .isEqualTo( 0.0 );

        assertWithMessage( "Transaction currency" )
                .that( transaction.getCurrency() )
                .isEqualTo( TRANSACTION_CURRENCY );

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

        assertWithMessage( "Transaction type (Receipt)" )
                .that( transaction )
                .isInstanceOf( TransactionReceipt.class );

        // FIXME
//        assertWithMessage( "Transaction counterparty (IBAN)" )
//                .that( transaction.getCounterparty().getIban() )
//                .isEqualTo( "SK1234567890" );
//
//        assertWithMessage( "Transaction counterparty (BIC)" )
//                .that( transaction.getCounterparty().getBic() )
//                .isEqualTo( "SLSPSK" );

        assertWithMessage( "Transaction name from description" )
                .that( ( ( TransactionReceipt ) transaction ).getMerchantName() )
                .isEqualTo( "From EUR source" );

        Date modificationDate = transaction.getModificationDate();

        stateChanged.execute();
        ofy().clear();

        transaction = ofy().load().type( CommonTransaction.class ).first().now();
        int count = ofy().load().type( TransactionReceipt.class ).count();

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
        created.setCategoryService( categoryService );

        // mocking of the transaction from remote bank system
        Transaction t = mapper.readValue( json, Transaction.class );
        Transaction afterStateChanged = mapper.readValue( json, Transaction.class );
        afterStateChanged.setState( TransactionState.fromValue( stateChanged.workWith().getData().getNewState() ) );

        Counterparty counterparty = new Counterparty();

        CounterpartyAccount counterpartyAccount = new CounterpartyAccount();
        counterpartyAccount.setIban( "SK1234567890" );
        counterpartyAccount.setBic( "SLSPSK" );
        counterparty.setAccounts( Collections.singletonList( counterpartyAccount ) );

        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( TRANSACTION_EXT_ID ).finish();
                result = t;
                result = afterStateChanged;

                facade.get( Counterparty.class ).identifiedBy( "4161edb6-7ba3-4501-951a-5825888307ff" ).finish();
                result = counterparty;
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

        assertWithMessage( "Transaction bill amount" )
                .that( transaction.getBillAmount() )
                .isNull();

        assertWithMessage( "Transaction bill currency" )
                .that( transaction.getBillCurrency() )
                .isNull();

        assertWithMessage( "Transaction related account Id" )
                .that( transaction.getBankAccountKey() )
                .isNull();

        assertWithMessage( "Balance after transaction" )
                .that( transaction.getBalance() )
                .isEqualTo( 100 );

        assertWithMessage( "Transaction currency" )
                .that( transaction.getCurrency() )
                .isEqualTo( TRANSACTION_CURRENCY );

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

        assertWithMessage( "Transaction type (Receipt)" )
                .that( transaction )
                .isInstanceOf( TransactionReceipt.class );

        assertWithMessage( "Transaction counterparty (IBAN)" )
                .that( transaction.getCounterparty().getIban() )
                .isEqualTo( "SK1234567890" );

        assertWithMessage( "Transaction counterparty (BIC)" )
                .that( transaction.getCounterparty().getBic() )
                .isEqualTo( "SLSPSK" );

        Date modificationDate = transaction.getModificationDate();

        stateChanged.execute();
        ofy().clear();

        transaction = ofy().load().type( CommonTransaction.class ).first().now();
        int count = ofy().load().type( TransactionReceipt.class ).count();

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
        created.setCategoryService( categoryService );

        // mocking of the transaction from remote bank system
        Transaction t = mapper.readValue( json, Transaction.class );
        Transaction afterStateChanged = mapper.readValue( json, Transaction.class );
        afterStateChanged.setState( TransactionState.fromValue( stateChanged.workWith().getData().getNewState() ) );

        Counterparty counterparty = new Counterparty();

        CounterpartyAccount counterpartyAccount = new CounterpartyAccount();
        counterpartyAccount.setIban( "SK1234567890" );
        counterpartyAccount.setBic( "SLSPSK" );
        counterparty.setAccounts( Collections.singletonList( counterpartyAccount ) );

        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( TRANSACTION_EXT_ID ).finish();
                result = t;
                result = afterStateChanged;

                facade.get( Counterparty.class ).identifiedBy( "5e3599aa-bd0d-45d0-9d0b-0686496a2156" ).finish();
                result = counterparty;
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

        assertWithMessage( "Transaction bill amount" )
                .that( transaction.getBillAmount() )
                .isEqualTo( 108.51 );

        assertWithMessage( "Transaction bill currency" )
                .that( transaction.getBillCurrency() )
                .isEqualTo( "GBP" );

        assertWithMessage( "Transaction related account Id" )
                .that( transaction.getBankAccountKey() )
                .isNull();

        assertWithMessage( "Balance after transaction" )
                .that( transaction.getBalance() )
                .isEqualTo( 22.5 );

        assertWithMessage( "Transaction currency" )
                .that( transaction.getCurrency() )
                .isEqualTo( TRANSACTION_CURRENCY );

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

        assertWithMessage( "Transaction type (Receipt)" )
                .that( transaction )
                .isInstanceOf( TransactionReceipt.class );

        assertWithMessage( "Transaction counterparty (IBAN)" )
                .that( transaction.getCounterparty().getIban() )
                .isEqualTo( "SK1234567890" );

        assertWithMessage( "Transaction counterparty (BIC)" )
                .that( transaction.getCounterparty().getBic() )
                .isEqualTo( "SLSPSK" );

        Date modificationDate = transaction.getModificationDate();

        stateChanged.execute();
        ofy().clear();

        transaction = ofy().load().type( CommonTransaction.class ).first().now();
        int count = ofy().load().type( TransactionReceipt.class ).count();

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
        // import test bank accounts
        ImportTask task = new ImportTask( "/testdataset/changeset_00001.xml" );
        task.run();

        task = new ImportTask( "/testdataset/changeset_local-account.xml" );
        task.run();

        CompanyBankAccount primaryBankAccount = config.getPrimaryBankAccount( null );
        // set bank account external Id taken from transaction-created-transfer-non-revolut.json
        primaryBankAccount.setExternalId( BANK_ACCOUNT_EXT_ID );
        primaryBankAccount.save();

        // in the real use case transaction is being created by subscription webhook
        config.initGetTransaction( TRANSACTION_EXT_ID );

        String json = toJsonCreated( TRANSFER.getValue() + "-non-revolut" );
        created = new TransactionCreatedTask( json );
        created.setConfig( config );
        created.setFacade( facade );
        created.setCategoryService( categoryService );

        // mocking of the transaction from remote bank system
        Transaction t = mapper.readValue( json, Transaction.class );
        Transaction afterStateChanged = mapper.readValue( json, Transaction.class );
        afterStateChanged.setState( TransactionState.fromValue( stateChanged.workWith().getData().getNewState() ) );

        Counterparty counterparty = new Counterparty();

        CounterpartyAccount counterpartyAccount = new CounterpartyAccount();
        counterpartyAccount.setIban( "SK1234567890" );
        counterpartyAccount.setBic( "SLSPSK" );
        counterparty.setAccounts( Collections.singletonList( counterpartyAccount ) );

        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( TRANSACTION_EXT_ID ).finish();
                result = t;
                result = afterStateChanged;

                facade.get( Counterparty.class ).identifiedBy( "a1dd617f-45b5-400b-8dd9-8970429d0a3c" ).finish();
                result = counterparty;
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

        assertWithMessage( "Transaction bill amount" )
                .that( transaction.getBillAmount() )
                .isNull();

        assertWithMessage( "Transaction bill currency" )
                .that( transaction.getBillCurrency() )
                .isNull();

        assertWithMessage( "Balance after transaction" )
                .that( transaction.getBalance() )
                .isEqualTo( 10.0 );

        assertWithMessage( "Transaction currency" )
                .that( transaction.getCurrency() )
                .isEqualTo( TRANSACTION_CURRENCY );

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

        assertWithMessage( "Transaction type (Receipt)" )
                .that( transaction )
                .isInstanceOf( TransactionReceipt.class );

        assertWithMessage( "Transaction counterparty (IBAN)" )
                .that( transaction.getCounterparty().getIban() )
                .isEqualTo( "SK1234567890" );

        assertWithMessage( "Transaction counterparty (BIC)" )
                .that( transaction.getCounterparty().getBic() )
                .isEqualTo( "SLSPSK" );

        Date modificationDate = transaction.getModificationDate();

        stateChanged.execute();
        ofy().clear();

        transaction = ofy().load().type( CommonTransaction.class ).first().now();
        int count = ofy().load().type( TransactionReceipt.class ).count();

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
        created.setCategoryService( categoryService );

        // mocking of the transaction from remote bank system
        Transaction t = mapper.readValue( json, Transaction.class );
        Transaction afterStateChanged = mapper.readValue( json, Transaction.class );
        afterStateChanged.setState( TransactionState.fromValue( stateChanged.workWith().getData().getNewState() ) );

        Counterparty counterparty = new Counterparty();

        CounterpartyAccount counterpartyAccount = new CounterpartyAccount();
        counterpartyAccount.setIban( "SK1234567890" );
        counterpartyAccount.setBic( "SLSPSK" );
        counterparty.setAccounts( Collections.singletonList( counterpartyAccount ) );

        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( TRANSACTION_EXT_ID ).finish();
                result = t;
                result = afterStateChanged;

                facade.get( Counterparty.class ).identifiedBy( "d197e887-7e8a-49e3-81e8-3a978140dfba" ).finish();
                result = counterparty;
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

        assertWithMessage( "Transaction bill amount" )
                .that( transaction.getBillAmount() )
                .isNull();

        assertWithMessage( "Transaction bill currency" )
                .that( transaction.getBillCurrency() )
                .isNull();

        assertWithMessage( "Transaction related account Id" )
                .that( transaction.getBankAccountKey() )
                .isNull();

        assertWithMessage( "Balance after transaction" )
                .that( transaction.getBalance() )
                .isNull();

        assertWithMessage( "Transaction currency" )
                .that( transaction.getCurrency() )
                .isEqualTo( TRANSACTION_CURRENCY );

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

        assertWithMessage( "Transaction type (Receipt)" )
                .that( transaction )
                .isInstanceOf( TransactionReceipt.class );

        assertWithMessage( "Transaction counterparty (IBAN)" )
                .that( transaction.getCounterparty().getIban() )
                .isEqualTo( "SK1234567890" );

        assertWithMessage( "Transaction counterparty (BIC)" )
                .that( transaction.getCounterparty().getBic() )
                .isEqualTo( "SLSPSK" );

        Date modificationDate = transaction.getModificationDate();

        stateChanged.execute();
        ofy().clear();

        transaction = ofy().load().type( CommonTransaction.class ).first().now();
        int count = ofy().load().type( TransactionReceipt.class ).count();

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
        created.setCategoryService( categoryService );

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

        assertWithMessage( "Transaction bill amount" )
                .that( transaction.getBillAmount() )
                .isNull();

        assertWithMessage( "Transaction bill currency" )
                .that( transaction.getBillCurrency() )
                .isNull();

        assertWithMessage( "Transaction related account Id" )
                .that( transaction.getBankAccountKey() )
                .isNull();

        assertWithMessage( "Balance after transaction" )
                .that( transaction.getBalance() )
                .isEqualTo( 30.00 );

        assertWithMessage( "Transaction currency" )
                .that( transaction.getCurrency() )
                .isEqualTo( TRANSACTION_CURRENCY );

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
        int count = ofy().load().type( TransactionReceipt.class ).count();

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

    @Test( expectedExceptions = NotFoundException.class )
    public void unsuccessful_TransactionNotFound()
    {
        created = new TransactionCreatedTask( toJsonCreated( TRANSFER.getValue() ) );
        // adding next task to test whether will be cleared
        created.addNext( task );
        created.setConfig( config );
        created.setFacade( facade );
        created.setCategoryService( categoryService );

        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( anyString ).finish();
                result = new NotFoundException();
            }
        };

        try
        {
            // test call
            created.execute();
        }
        catch ( NotFoundException e )
        {
            int count = ofy().load().type( TransactionReceipt.class ).count();
            assertWithMessage( "Final number of transactions" )
                    .that( count )
                    .isEqualTo( 0 );

            assertWithMessage( "Remaining number of tasks after failure" )
                    .that( created.countTasks() )
                    // 2 is the current one
                    .isEqualTo( 2 );

            throw e;
        }
    }

    @Test( expectedExceptions = ClientErrorException.class )
    public void unsuccessful_RevolutClientError()
    {
        created = new TransactionCreatedTask( toJsonCreated( TRANSFER.getValue() ) );
        // adding next task to test whether will be cleared
        created.addNext( task );
        created.setConfig( config );
        created.setFacade( facade );
        created.setCategoryService( categoryService );

        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( TRANSACTION_EXT_ID ).finish();
                result = new ClientErrorException();
            }
        };

        try
        {
            // test call
            created.execute();
        }
        catch ( ClientErrorException e )
        {
            int count = ofy().load().type( TransactionReceipt.class ).count();
            assertWithMessage( "Final number of transactions" )
                    .that( count )
                    .isEqualTo( 0 );

            assertWithMessage( "Remaining number of tasks after failure" )
                    .that( created.countTasks() )
                    // 2 is the current one
                    .isEqualTo( 2 );

            throw e;
        }
    }

    @Test( expectedExceptions = UnauthorizedException.class )
    public void unsuccessful_RevolutUnauthorized()
    {
        created = new TransactionCreatedTask( toJsonCreated( TRANSFER.getValue() ) );
        // adding next task to test whether will be cleared
        created.addNext( task );
        created.setConfig( config );
        created.setFacade( facade );
        created.setCategoryService( categoryService );

        new Expectations()
        {
            {
                facade.get( Transaction.class ).identifiedBy( anyString ).finish();
                result = new UnauthorizedException();
            }
        };

        try
        {
            // test call
            created.execute();
        }
        catch ( UnauthorizedException e )
        {
            int count = ofy().load().type( TransactionReceipt.class ).count();
            assertWithMessage( "Final number of transactions" )
                    .that( count )
                    .isEqualTo( 0 );

            assertWithMessage( "Remaining number of tasks after failure" )
                    .that( created.countTasks() )
                    // 2 is the current one
                    .isEqualTo( 2 );

            throw e;
        }
    }

    @Test
    public void unsuccessful_MissingLegs() throws JsonProcessingException
    {
        String json = toJsonCreated( "missing-legs" );
        created = new TransactionCreatedTask( json );
        // adding next task to test whether will be cleared
        created.addNext( task );
        created.setConfig( config );
        created.setFacade( facade );
        created.setCategoryService( categoryService );

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

        int count = ofy().load().type( TransactionReceipt.class ).count();
        assertWithMessage( "Final number of transactions" )
                .that( count )
                .isEqualTo( 0 );

        assertWithMessage( "Remaining number of tasks after failure" )
                .that( created.countTasks() )
                // 1 is the current one
                .isEqualTo( 1 );
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
                .isEqualTo( REVOLUT_BANK_EU_CODE );

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