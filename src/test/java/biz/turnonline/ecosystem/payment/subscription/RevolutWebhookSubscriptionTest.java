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

import biz.turnonline.ecosystem.payment.service.revolut.webhook.TransactionCreatedTask;
import biz.turnonline.ecosystem.payment.service.revolut.webhook.TransactionStateChanged;
import biz.turnonline.ecosystem.payment.service.revolut.webhook.TransactionStateChangedTask;
import biz.turnonline.ecosystem.revolut.business.transaction.model.Transaction;
import com.google.appengine.api.taskqueue.TaskOptions;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.ctoolkit.services.task.Task;
import org.ctoolkit.services.task.TaskExecutor;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static biz.turnonline.ecosystem.payment.service.revolut.webhook.TransactionCreatedFlowTest.TRANSACTION_EXT_ID;
import static biz.turnonline.ecosystem.revolut.business.transaction.model.TransactionType.CARD_PAYMENT;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * {@link RevolutWebhookSubscription} unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@SuppressWarnings( "ConstantConditions" )
public class RevolutWebhookSubscriptionTest
{
    @Tested
    private RevolutWebhookSubscription tested;

    @Injectable
    private TaskExecutor executor;

    @Mocked
    private HttpServletRequest request;

    @Mocked
    private HttpServletResponse response;

    @Test
    public void transactionCreated_Scheduled() throws IOException
    {
        new Expectations()
        {
            {
                request.getInputStream();
                result = new MockedInputStream( "transaction-created-card_payment.json" );
            }
        };

        tested.doPut( request, response );

        new Verifications()
        {
            {
                Task<?> task;
                executor.schedule( task = withCapture() );

                assertWithMessage( "Event task" )
                        .that( task )
                        .isNotNull();

                assertWithMessage( "Number of scheduled tasks" )
                        .that( task.countTasks() )
                        .isEqualTo( 2 );

                assertWithMessage( "Type of the event task" )
                        .that( task )
                        .isInstanceOf( TransactionCreatedTask.class );

                assertWithMessage( "Publisher task" )
                        .that( task.next() )
                        .isInstanceOf( TransactionPublisherTask.class );

                TransactionCreatedTask tt = ( TransactionCreatedTask ) task;
                Transaction event = tt.workWith();

                assertWithMessage( "Task transaction payload" )
                        .that( event )
                        .isNotNull();

                assertWithMessage( "Transaction ID" )
                        .that( event.getId() )
                        .isEqualTo( TRANSACTION_EXT_ID );

                assertWithMessage( "Transaction type" )
                        .that( event.getType() )
                        .isEqualTo( CARD_PAYMENT );

                assertWithMessage( "Transaction merchant" )
                        .that( event.getMerchant() )
                        .isNotNull();

                assertWithMessage( "Transaction merchant name" )
                        .that( event.getMerchant().getName() )
                        .isEqualTo( "Best, Ltd." );

                assertWithMessage( "Transaction city" )
                        .that( event.getMerchant().getCity() )
                        .isEqualTo( "Bratislava" );

                assertWithMessage( "Transaction merchant category code" )
                        .that( event.getMerchant().getCategoryCode() )
                        .isEqualTo( "7523" );

                assertWithMessage( "Transaction merchant country" )
                        .that( event.getMerchant().getCountry() )
                        .isEqualTo( "SVK" );

                assertWithMessage( "Transaction legs" )
                        .that( event.getLegs() )
                        .hasSize( 1 );

                assertWithMessage( "Transaction card" )
                        .that( event.getCard() )
                        .isNotNull();

                assertWithMessage( "Transaction card number" )
                        .that( event.getCard().getCardNumber() )
                        .isEqualTo( "442581******7123" );

                assertWithMessage( "Transaction card first name" )
                        .that( event.getCard().getFirstName() )
                        .isEqualTo( "John" );

                assertWithMessage( "Transaction card last name" )
                        .that( event.getCard().getLastName() )
                        .isEqualTo( "Foo" );

                assertWithMessage( "Transaction card phone" )
                        .that( event.getCard().getPhone() )
                        .isEqualTo( "+421905905905" );
            }
        };
    }

    @Test
    public void transactionStateChanged_Scheduled() throws IOException
    {
        new Expectations()
        {
            {
                request.getInputStream();
                result = new MockedInputStream( "transaction-state-changed.json" );
            }
        };

        tested.doPost( request, response );

        new Verifications()
        {
            {
                Task<?> task;
                executor.schedule( task = withCapture() );

                assertWithMessage( "Event task" )
                        .that( task )
                        .isNotNull();

                assertWithMessage( "Number of scheduled tasks" )
                        .that( task.countTasks() )
                        .isEqualTo( 2 );

                assertWithMessage( "Type of the event task" )
                        .that( task )
                        .isInstanceOf( TransactionStateChangedTask.class );

                assertWithMessage( "Publisher task" )
                        .that( task.next() )
                        .isInstanceOf( TransactionPublisherTask.class );

                TransactionStateChangedTask tt = ( TransactionStateChangedTask ) task;
                TransactionStateChanged event = tt.workWith();

                assertWithMessage( "Event payload" )
                        .that( event )
                        .isNotNull();

                assertWithMessage( "Event type" )
                        .that( event.getEvent() )
                        .isEqualTo( "TransactionStateChanged" );

                assertWithMessage( "Event timestamp" )
                        .that( event.getTimestamp() )
                        .isNotNull();

                assertWithMessage( "Event data" )
                        .that( event.getData() )
                        .isNotNull();

                assertWithMessage( "Event data old state" )
                        .that( event.getData().getOldState() )
                        .isEqualTo( "pending" );

                assertWithMessage( "Event data new state" )
                        .that( event.getData().getNewState() )
                        .isEqualTo( "completed" );
            }
        };
    }

    @Test
    public void transactionStateChanged_UnknownEventType() throws IOException
    {
        new Expectations()
        {
            {
                request.getInputStream();
                result = new MockedInputStream( "transaction-type-unknown.json" );
            }
        };

        tested.doPost( request, response );

        new Verifications()
        {
            {
                executor.schedule( ( Task<?> ) any );
                times = 0;

                executor.schedule( ( Task<?> ) any, ( TaskOptions ) any );
                times = 0;
            }
        };
    }

    @Test
    public void transactionCreated_InvalidStructure() throws IOException
    {
        new Expectations()
        {
            {
                request.getInputStream();
                result = new MockedInputStream( "transaction-created-invalid-structure.json" );
            }
        };

        tested.doPost( request, response );

        new Verifications()
        {
            {
                executor.schedule( ( Task<?> ) any );
                times = 0;

                executor.schedule( ( Task<?> ) any, ( TaskOptions ) any );
                times = 0;
            }
        };
    }

    @Test
    public void transactionStateChanged_InvalidStructure() throws IOException
    {
        new Expectations()
        {
            {
                request.getInputStream();
                result = new MockedInputStream( "transaction-state-changed-invalid-structure.json" );
            }
        };

        tested.doPut( request, response );

        new Verifications()
        {
            {
                executor.schedule( ( Task<?> ) any );
                times = 0;

                executor.schedule( ( Task<?> ) any, ( TaskOptions ) any );
                times = 0;
            }
        };
    }

    @Test
    public void transactionEvent_MissingBody() throws IOException
    {
        new Expectations()
        {
            {
                request.getInputStream();
                result = null;
            }
        };

        tested.doPut( request, response );

        new Verifications()
        {
            {
                response.setStatus( HttpServletResponse.SC_BAD_REQUEST );

                executor.schedule( ( Task<?> ) any );
                times = 0;

                executor.schedule( ( Task<?> ) any, ( TaskOptions ) any );
                times = 0;
            }
        };
    }

    @Test
    public void transactionEvent_EmptyJsonBody() throws IOException
    {
        new Expectations()
        {
            {
                request.getInputStream();
                result = new MockedInputStream( "empty-json.json" );
            }
        };

        tested.doPut( request, response );

        new Verifications()
        {
            {
                executor.schedule( ( Task<?> ) any );
                times = 0;

                executor.schedule( ( Task<?> ) any, ( TaskOptions ) any );
                times = 0;
            }
        };
    }
}