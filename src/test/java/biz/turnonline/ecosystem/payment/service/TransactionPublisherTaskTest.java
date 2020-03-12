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

package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.payment.api.model.Transaction;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.steward.model.Account;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.pubsub.model.PublishResponse;
import com.google.api.services.pubsub.model.PubsubMessage;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import ma.glasnost.orika.MapperFacade;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.restapi.client.pubsub.TopicMessage;
import org.ctoolkit.services.storage.PropertiesHashCode;
import org.ctoolkit.services.storage.PropertiesHasher;
import org.ctoolkit.services.task.Task;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static biz.turnonline.ecosystem.payment.service.BackendServiceTestCase.getFromFile;
import static biz.turnonline.ecosystem.payment.service.PaymentConfig.TRANSACTION_TOPIC;
import static biz.turnonline.ecosystem.payment.service.model.FormOfPayment.CARD_PAYMENT;
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
    private static final String GOOGLE_CLOUD_PROJECT = "test-t2b";

    private static final Long TRANSACTION_ID = 2468L;

    private static final Long ACCOUNT_ID = 1735L;

    private static final String ACCOUNT_IDENTITY_ID = "64HGtr6ks";

    private static final String ACCOUNT_AUDIENCE = GOOGLE_CLOUD_PROJECT;

    private static final String EXPECTED_TOPIC = "projects/" + GOOGLE_CLOUD_PROJECT + "/topics/" + TRANSACTION_TOPIC;

    private static final String EXPECTED_ID = String.valueOf( TRANSACTION_ID );

    private static final String EXPECTED_ACCOUNT_ID = String.valueOf( ACCOUNT_ID );

    private static final String EXPECTED_DATA_TYPE = "Transaction";

    private static final String EXPECTED_EMAIL = "my.account@turnonline.biz";

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
    private PaymentConfig config;

    @Mocked
    private Key<CompanyBankAccount> accountKey;

    private LocalAccount account;

    private Transaction api;

    private CommonTransaction transaction;

    @BeforeMethod
    public void before()
    {
        System.setProperty( "GOOGLE_CLOUD_PROJECT", GOOGLE_CLOUD_PROJECT );
        jsonMapper = new MicroserviceModule().provideJsonObjectMapperPubSub();

        api = getFromFile( "transaction.json", Transaction.class );

        account = new LocalAccount( new Account()
                .setId( ACCOUNT_ID )
                .setEmail( EXPECTED_EMAIL )
                .setIdentityId( ACCOUNT_IDENTITY_ID )
                .setAudience( ACCOUNT_AUDIENCE ) );

        transaction = new TransactionBillTest( "7fa8816a-1fe5-4fc7-9e86-fd659b753167", TRANSACTION_ID )
                .bankAccountKey( accountKey )
                .bankCode( "REVO" )
                .completedAt( OffsetDateTime.now() )
                .amount( 59.0 )
                .currency( "EUR" )
                .credit( false )
                .failure( false )
                .type( CARD_PAYMENT )
                .status( CommonTransaction.State.COMPLETED )
                .reference( "Payment for online service" );

    }

    @Test
    public void successful() throws IOException
    {
        expectationsTransaction();

        new Expectations()
        {
            {
                config.getLocalAccount();
                result = account;

                mapper.map( transaction, Transaction.class );
                result = api;

                entityKey.getId();
                result = TRANSACTION_ID;

                entityKey.getKind();
                result = "PP_Transaction";

                facade.insert( any ).answerBy( PublishResponse.class ).finish();
                result = new PublishResponse().setMessageIds( Lists.newArrayList( "7531" ) );
            }
        };

        tested.execute();

        new Verifications()
        {
            {
                TopicMessage message;
                facade.insert( message = withCapture() );
                assertThat( message ).isNotNull();
                assertThat( message.getTopic() ).isEqualTo( EXPECTED_TOPIC );

                List<PubsubMessage> messages = message.getMessages();
                assertThat( messages ).hasSize( 1 );

                Map<String, String> attributes = messages.get( 0 ).getAttributes();
                assertThat( attributes ).hasSize( 6 );
                assertThat( attributes.get( "Entity_ID" ) ).isEqualTo( EXPECTED_ID );
                assertThat( attributes.get( "DataType" ) ).isEqualTo( EXPECTED_DATA_TYPE );
                assertThat( attributes.get( "AccountEmail" ) ).isEqualTo( EXPECTED_EMAIL );
                assertThat( attributes.get( "AccountUnique_ID" ) ).isEqualTo( EXPECTED_ACCOUNT_ID );
                assertThat( attributes.get( "AccountIdentity_ID" ) ).isEqualTo( ACCOUNT_IDENTITY_ID );
                assertThat( attributes.get( "AccountAudience" ) ).isEqualTo( ACCOUNT_AUDIENCE );

                PubsubMessage psb = messages.get( 0 );
                Map<String, Object> map = mapOf( psb.decodeData(), api.getClass() );
                Map<String, Object> properties = new Helper().flatMap( map, null );
                assertThat( properties ).hasSize( 22 );

                assertWithMessage( "Transaction amount" )
                        .that( properties.get( "amount" ) )
                        .isNotNull();

                assertWithMessage( "Transaction bank account Id" )
                        .that( properties.get( "bankAccount.id" ) )
                        .isNotNull();

                assertWithMessage( "Transaction bill invoice Id" )
                        .that( properties.get( "bill.invoiceId" ) )
                        .isNotNull();

                assertWithMessage( "Transaction status" )
                        .that( properties.get( "status" ) )
                        .isEqualTo( "COMPLETED" );

                assertWithMessage( "Transaction type" )
                        .that( properties.get( "type" ) )
                        .isEqualTo( "TRANSFER" );

                assertWithMessage( "Transaction type" )
                        .that( properties.get( "bankAccount.bank.code" ) )
                        .isEqualTo( "REVO" );
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
    public void unsuccessful_LocalAccountNotFound()
    {
        expectationsTransaction();

        new Expectations()
        {
            {
                config.getLocalAccount();
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

    @Test
    public void unsuccessful_AccountIdMissing()
    {
        expectationsTransaction();

        new Expectations( account )
        {
            {
                account.getId();
                result = null;

                config.getLocalAccount();
                result = account;
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
    public void unsuccessful_AccountEmailMissing()
    {
        expectationsTransaction();

        new Expectations()
        {
            {
                config.getLocalAccount();
                result = new LocalAccount( new Account()
                        .setId( ACCOUNT_ID )
                        .setEmail( "" )
                        .setIdentityId( ACCOUNT_IDENTITY_ID )
                        .setAudience( ACCOUNT_AUDIENCE ) );
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
    public void unsuccessful_AccountIdentityIdMissing()
    {
        expectationsTransaction();

        new Expectations()
        {
            {
                config.getLocalAccount();
                result = new LocalAccount( new Account()
                        .setId( ACCOUNT_ID )
                        .setEmail( EXPECTED_EMAIL )
                        .setIdentityId( "" )
                        .setAudience( ACCOUNT_AUDIENCE ) );
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
    public void unsuccessful_AccountAudienceMissing()
    {
        expectationsTransaction();

        new Expectations()
        {
            {
                config.getLocalAccount();
                result = new LocalAccount( new Account()
                        .setId( ACCOUNT_ID )
                        .setEmail( EXPECTED_EMAIL )
                        .setIdentityId( ACCOUNT_IDENTITY_ID )
                        .setAudience( "" ) );
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
                config.getLocalAccount();
                result = account;

                mapper.map( transaction, Transaction.class );
                result = new RuntimeException( "temporal failure" );

                facade.insert( any );
                times = 0;
            }
        };

        tested.execute();
    }

    /**
     * This JSON related exception represents a development error, do not re-try
     */
    @Test
    public void unsuccessful_JsonMapperFailure() throws JsonProcessingException
    {
        expectationsTransaction();

        new Expectations( jsonMapper )
        {
            {
                config.getLocalAccount();
                result = account;

                mapper.map( transaction, Transaction.class );
                result = api;

                jsonMapper.writeValueAsBytes( api );
                result = new MockedJsonProcessingException();
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

    private Map<String, Object> mapOf( byte[] data, Class<?> valueType )
            throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();

        Object input = mapper.readValue( data, valueType );
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