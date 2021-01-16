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

package biz.turnonline.ecosystem.payment.subscription;

import biz.turnonline.ecosystem.payment.api.model.Transaction;
import biz.turnonline.ecosystem.payment.service.LocalAccountProvider;
import biz.turnonline.ecosystem.payment.service.MicroserviceModule;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.payment.service.model.TransactionCategory;
import biz.turnonline.ecosystem.steward.model.Account;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.objectify.Key;
import ma.glasnost.orika.MapperFacade;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mock;
import mockit.MockUp;
import mockit.Tested;
import mockit.Verifications;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.services.storage.PropertiesHashCode;
import org.ctoolkit.services.storage.PropertiesHasher;
import org.ctoolkit.services.task.Task;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

import static biz.turnonline.ecosystem.payment.service.BackendServiceTestCase.getFromFile;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * {@link TransactionPublisherTask} unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@SuppressWarnings( "ConstantConditions" )
public class TransactionPublisherTaskTest
{
    private static final Long TRANSACTION_ID = 2468L;

    private static final Long ACCOUNT_ID = 1735L;

    private static final String ACCOUNT_IDENTITY_ID = "64HGtr6ks";

    private static final String ACCOUNT_EMAIL = "my.account@turnonline.biz";

    @Tested
    private TransactionPublisherTask tested;

    @Injectable
    private Key<CommonTransaction> entityKey;

    @Injectable
    private RestFacade facade;

    @SuppressWarnings( "FieldCanBeLocal" )
    @Injectable
    private ObjectMapper jsonMapper;

    @Injectable
    private MapperFacade mapper;

    @Injectable
    private LocalAccountProvider lap;

    private LocalAccount account;

    private Transaction api;

    private CommonTransaction transaction;

    @BeforeMethod
    public void before()
    {
        jsonMapper = new MicroserviceModule().provideJsonObjectMapperPubSub();

        api = getFromFile( "transaction.json", Transaction.class );

        account = new LocalAccount( new Account()
                .setId( ACCOUNT_ID )
                .setEmail( ACCOUNT_EMAIL )
                .setIdentityId( ACCOUNT_IDENTITY_ID ) );

        transaction = new TransactionReceiptTest( "7fa8816a-1fe5-4fc7-9e86-fd659b753167", TRANSACTION_ID );
    }

    @Test
    public void successful()
    {
        expectationsTransaction();

        new Expectations()
        {
            {
                lap.get();
                result = account;

                mapper.map( transaction, Transaction.class );
                result = api;

                facade.insert( any ).onBehalfOf( account ).finish();
            }
        };

        tested.execute();

        new Verifications()
        {
            {
                biz.turnonline.ecosystem.billing.model.Transaction message;
                facade.insert( message = withCapture() );
                assertThat( message ).isNotNull();

                Map<String, Object> map = mapOf( message );
                Map<String, Object> properties = new Helper().flatMap( map, null );
                assertThat( properties ).hasSize( 21 );

                assertWithMessage( "Transaction amount" )
                        .that( properties.get( "amount" ) )
                        .isEqualTo( 35.0 );

                assertWithMessage( "Transaction bill amount" )
                        .that( properties.get( "billAmount" ) )
                        .isEqualTo( 31.29 );

                assertWithMessage( "Transaction bill currency" )
                        .that( properties.get( "billCurrency" ) )
                        .isEqualTo( "GBP" );

                assertWithMessage( "Transaction bank account IBAN" )
                        .that( properties.get( "bankAccount.iban" ) )
                        .isNotNull();

                assertWithMessage( "Transaction bill order Id" )
                        .that( properties.get( "bill.order" ) )
                        .isNotNull();

                assertWithMessage( "Transaction bill invoice Id" )
                        .that( properties.get( "bill.invoice" ) )
                        .isNotNull();

                assertWithMessage( "Transaction merchant name" )
                        .that( properties.get( "merchant.name" ) )
                        .isEqualTo( "Pty Ltd" );

                assertWithMessage( "Transaction status" )
                        .that( properties.get( "status" ) )
                        .isEqualTo( "COMPLETED" );

                assertWithMessage( "Transaction type" )
                        .that( properties.get( "type" ) )
                        .isEqualTo( "TRANSFER" );

                assertWithMessage( "Transaction bank account code" )
                        .that( properties.get( "bankAccount.code" ) )
                        .isEqualTo( "REVO" );

                assertWithMessage( "Transaction ID" )
                        .that( properties.get( "transactionId" ) )
                        .isEqualTo( 645568 );
            }
        };
    }

    @Test
    public void unsuccessful_TransactionNotFound()
    {
        new MockUp<Task<?>>()
        {
            @Mock
            public CommonTransaction workWith()
            {
                // not found, returns null
                return null;
            }
        };

        tested.execute();

        new Verifications()
        {
            {
                facade.insert( any );
                times = 0;
            }
        };
    }

    @Test
    public void unsuccessful_DoNotPropagate()
    {
        new MockUp<Task<?>>()
        {
            @Mock
            public CommonTransaction workWith()
            {
                // not found, returns null
                TransactionCategory category = new TransactionCategory();
                category.setPropagate( false );
                transaction.setCategories( Collections.singletonList( category ) );

                return transaction;
            }
        };

        tested.execute();

        new Verifications()
        {
            {
                facade.insert( any );
                times = 0;
            }
        };
    }

    @Test
    public void unsuccessful_LocalAccountNotFound()
    {
        expectationsTransaction();

        new Expectations()
        {
            {
                lap.get();
                result = null;
            }
        };

        tested.execute();

        new Verifications()
        {
            {
                facade.insert( any );
                times = 0;
            }
        };
    }

    /**
     * On exception task will retry
     */
    @Test( expectedExceptions = RuntimeException.class )
    public void unsuccessful_MapperFailure()
    {
        expectationsTransaction();

        new Expectations()
        {
            {
                mapper.map( transaction, Transaction.class );
                result = new RuntimeException( "temporal failure" );

                facade.insert( any );
                times = 0;
            }
        };

        tested.execute();
    }

    private void expectationsTransaction()
    {
        new MockUp<Task<?>>()
        {
            @Mock
            public CommonTransaction workWith()
            {
                return transaction;
            }
        };
    }

    private Map<String, Object> mapOf( biz.turnonline.ecosystem.billing.model.Transaction input )
    {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue( input, new PropertiesHasher.MapType() );
    }

    private static class Helper
            implements PropertiesHasher
    {
        @Override
        public String calcPropsHashCode( @Nonnull String name )
        {
            return null;
        }

        @Override
        public PropertiesHashCode getPropsHashCode()
        {
            return null;
        }
    }
}