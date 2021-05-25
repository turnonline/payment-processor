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

import biz.turnonline.ecosystem.billing.model.BillPayment;
import biz.turnonline.ecosystem.billing.model.IncomingInvoice;
import biz.turnonline.ecosystem.billing.model.Invoice;
import biz.turnonline.ecosystem.billing.model.PurchaseOrder;
import biz.turnonline.ecosystem.payment.service.LocalAccountProvider;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.payment.service.model.Timestamp;
import biz.turnonline.ecosystem.payment.service.revolut.RevolutBeneficiarySyncTask;
import biz.turnonline.ecosystem.payment.service.revolut.RevolutIncomingInvoiceProcessorTask;
import biz.turnonline.ecosystem.payment.service.revolut.RevolutInvoiceProcessorTask;
import com.google.api.client.util.DateTime;
import com.google.api.services.pubsub.model.PubsubMessage;
import com.google.common.io.ByteStreams;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.ctoolkit.restapi.client.ClientErrorException;
import org.ctoolkit.restapi.client.NotFoundException;
import org.ctoolkit.restapi.client.pubsub.PubsubCommand;
import org.ctoolkit.restapi.client.pubsub.TopicMessage;
import org.ctoolkit.services.task.Task;
import org.ctoolkit.services.task.TaskExecutor;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static biz.turnonline.ecosystem.payment.service.PaymentConfig.REVOLUT_BANK_CODE;
import static biz.turnonline.ecosystem.payment.service.PaymentConfig.REVOLUT_BANK_EU_CODE;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_EMAIL;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_IDENTITY_ID;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_UNIQUE_ID;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.DATA_TYPE;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ENCODED_UNIQUE_KEY;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ENTITY_DELETION;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ENTITY_ID;

/**
 * {@link ProductBillingChangesSubscription} unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@SuppressWarnings( {"unchecked", "ConstantConditions", "rawtypes", "ResultOfMethodCallIgnored"} )
public class ProductBillingChangesSubscriptionTest
{
    private static final String EMAIL = "debtor.account@turnonline.biz";

    private static final long ACCOUNT_ID = 565728947417L;

    private static final String IDENTITY_ID = "CvbTgHmdkl23h5";

    private static final long PURCHASE_ORDER_ID = 565941286574L;

    private static final long INVOICE_ID = 54489855244444L;

    private static final long INCOMING_INVOICE_ID = 571714185671L;

    @Tested
    private ProductBillingChangesSubscription tested;

    @Injectable
    private TaskExecutor executor;

    @Injectable
    private LocalAccountProvider lap;

    @Injectable
    private PaymentConfig config;

    @Mocked
    private Timestamp timestamp;

    @Mocked
    private CompanyBankAccount debtorBank;

    @Test
    public void onMessage_Account_NoneAssociated() throws Exception
    {
        PubsubMessage message = iiPubsubMessage( false );
        new Expectations()
        {
            {
                lap.check( ( PubsubCommand ) any );
                result = null;
            }
        };

        tested.onMessage( message, "billing.changes" );

        new Verifications()
        {
            {
                executor.schedule( ( Task ) any );
                times = 0;

                timestamp.done();
                times = 0;
            }
        };
    }

    @Test
    public void onMessage_Account_NotFound() throws Exception
    {
        PubsubMessage message = iiPubsubMessage( false );
        new Expectations()
        {
            {
                lap.check( ( PubsubCommand ) any );
                result = new NotFoundException( "Account not found" );
            }
        };

        tested.onMessage( message, "billing.changes" );

        new Verifications()
        {
            {
                executor.schedule( ( Task ) any );
                times = 0;

                timestamp.done();
                times = 0;
            }
        };
    }

    @Test
    public void onMessage_Account_ClientError() throws Exception
    {
        PubsubMessage message = iiPubsubMessage( false );
        new Expectations()
        {
            {
                lap.check( ( PubsubCommand ) any );
                result = new ClientErrorException( "Client error" );
            }
        };

        tested.onMessage( message, "billing.changes" );

        new Verifications()
        {
            {
                executor.schedule( ( Task ) any );
                times = 0;

                timestamp.done();
                times = 0;
            }
        };
    }

    @Test
    public void onMessage_ProcessPurchaseOrder() throws Exception
    {
        PubsubMessage message = orderPubsubMessage( false );
        tested.onMessage( message, "billing.changes" );

        new Verifications()
        {
            {
                // order.getModificationDate() is null -> last == null
                Timestamp.of( anyString, ( List ) any, ( LocalAccount ) any, null );

                Task<PurchaseOrder> task;
                executor.schedule( task = withCapture() );
                times = 1;

                assertWithMessage( "Number of scheduled tasks" )
                        .that( task.countTasks() )
                        .isEqualTo( 1 );

                assertThat( task ).isInstanceOf( PurchaseOrderProcessorTask.class );

                assertWithMessage( "Entity scheduled to be deleted" )
                        .that( ( ( PurchaseOrderProcessorTask ) task ).isDelete() ).isFalse();

                timestamp.done();
            }
        };
    }

    @Test
    public void onMessage_ProcessPurchaseOrderDeletion() throws Exception
    {
        PubsubMessage message = orderPubsubMessage( true );
        String publishTime = "2019-03-25T16:00:00.999Z";
        message.setPublishTime( publishTime );

        tested.onMessage( message, "billing.changes" );

        new Verifications()
        {
            {
                Timestamp.of( anyString, ( List ) any, ( LocalAccount ) any,
                        withEqual( DateTime.parseRfc3339( publishTime ) ) );

                Task<PurchaseOrder> task;
                executor.schedule( task = withCapture() );
                times = 1;

                assertWithMessage( "Number of scheduled tasks" )
                        .that( task.countTasks() )
                        .isEqualTo( 1 );

                assertThat( task ).isInstanceOf( PurchaseOrderProcessorTask.class );

                assertWithMessage( "Entity scheduled to be deleted" )
                        .that( ( ( PurchaseOrderProcessorTask ) task ).isDelete() ).isTrue();

                timestamp.done();
            }
        };
    }

    @Test
    public void onMessage_ProcessPurchaseOrderIgnoreObsoleteChanges() throws Exception
    {
        PubsubMessage message = orderPubsubMessage( true );

        new Expectations()
        {
            {
                timestamp.isObsolete();
                result = true;
            }
        };

        tested.onMessage( message, "billing.changes" );

        new Verifications()
        {
            {
                executor.schedule( ( Task ) any );
                times = 0;

                timestamp.done();
                times = 0;
            }
        };
    }

    @Test
    public void onMessage_ProcessIncomingInvoiceByRevolut() throws Exception
    {
        PubsubMessage message = iiPubsubMessage( false );
        new Expectations()
        {
            {
                debtorBank.isDebtorReady();
                result = true;

                debtorBank.getBankCode();
                result = REVOLUT_BANK_CODE;
            }
        };

        tested.onMessage( message, "billing.changes" );

        new Verifications()
        {
            {
                Task<IncomingInvoice> task;
                executor.schedule( task = withCapture() );
                times = 1;

                assertWithMessage( "Number of scheduled tasks" )
                        .that( task.countTasks() )
                        .isEqualTo( 2 );

                assertThat( task ).isInstanceOf( RevolutBeneficiarySyncTask.class );
                assertThat( task.next() ).isInstanceOf( RevolutIncomingInvoiceProcessorTask.class );

                assertWithMessage( "Entity scheduled to be deleted" )
                        .that( ( ( RevolutIncomingInvoiceProcessorTask ) task.next() ).isDelete() ).isFalse();

                timestamp.done();
            }
        };
    }

    @Test
    public void onMessage_ProcessIncomingInvoiceByRevolutEU() throws Exception
    {
        PubsubMessage message = iiPubsubMessage( false );
        new Expectations()
        {
            {
                debtorBank.isDebtorReady();
                result = true;

                debtorBank.getBankCode();
                result = REVOLUT_BANK_EU_CODE;
            }
        };

        tested.onMessage( message, "billing.changes" );

        new Verifications()
        {
            {
                Task<IncomingInvoice> task;
                executor.schedule( task = withCapture() );
                times = 1;

                assertWithMessage( "Number of scheduled tasks" )
                        .that( task.countTasks() )
                        .isEqualTo( 2 );

                assertThat( task ).isInstanceOf( RevolutBeneficiarySyncTask.class );
                assertThat( task.next() ).isInstanceOf( RevolutIncomingInvoiceProcessorTask.class );

                assertWithMessage( "Entity scheduled to be deleted" )
                        .that( ( ( RevolutIncomingInvoiceProcessorTask ) task.next() ).isDelete() ).isFalse();

                timestamp.done();
            }
        };
    }

    @Test
    public void onMessage_ProcessIncomingInvoice_PaymentMissing() throws Exception
    {
        PubsubMessage message = iiPubsubMessage( "ii-payment-missing.pubsub.json", false );
        tested.onMessage( message, "billing.changes" );

        new Verifications()
        {
            {
                executor.schedule( ( Task ) any );
                times = 0;

                timestamp.done();
                times = 0;
            }
        };
    }

    @Test
    public void onMessage_ProcessIncomingInvoice_PaymentTotalAmountMissing() throws Exception
    {
        PubsubMessage message = iiPubsubMessage( "ii-total-amount-missing.pubsub.json", false );
        new Expectations()
        {
            {
                debtorBank.isDebtorReady();
                result = true;
                minTimes = 0;

                debtorBank.getBankCode();
                result = REVOLUT_BANK_CODE;
                minTimes = 0;
            }
        };

        tested.onMessage( message, "billing.changes" );

        new Verifications()
        {
            {
                executor.schedule( ( Task ) any );
                times = 0;

                timestamp.done();
                times = 0;
            }
        };
    }

    @Test
    public void onMessage_ProcessIncomingInvoice_AlreadyPaid() throws Exception
    {
        PubsubMessage message = iiPubsubMessage( "incoming-invoice-paid.pubsub.json", false );
        new Expectations()
        {
            {
                debtorBank.isDebtorReady();
                result = true;

                debtorBank.getBankCode();
                result = REVOLUT_BANK_CODE;
                minTimes = 0;
            }
        };

        tested.onMessage( message, "billing.changes" );

        new Verifications()
        {
            {
                executor.schedule( ( Task ) any );
                times = 0;

                timestamp.done();
                times = 0;
            }
        };
    }

    @Test
    public void onMessage_ProcessIncomingInvoice_PaymentMethodCASH() throws Exception
    {
        PubsubMessage message = iiPubsubMessage( "ii-cash-payment.pubsub.json", false );
        new Expectations()
        {
            {
                debtorBank.isDebtorReady();
                result = true;
                minTimes = 0;

                debtorBank.getBankCode();
                result = REVOLUT_BANK_CODE;
                minTimes = 0;
            }
        };

        tested.onMessage( message, "billing.changes" );

        new Verifications()
        {
            {
                executor.schedule( ( Task ) any );
                times = 0;

                timestamp.done();
                times = 0;
            }
        };
    }

    @Test
    public void onMessage_ProcessIncomingInvoice_DebtorBankAccountNotFound() throws Exception
    {
        PubsubMessage message = iiPubsubMessage( false );
        new Expectations()
        {
            {
                config.getDebtorBankAccount( ( BillPayment ) any );
                result = null;
            }
        };

        tested.onMessage( message, "billing.changes" );

        new Verifications()
        {
            {
                executor.schedule( ( Task ) any );
                times = 0;

                timestamp.done();
                times = 0;
            }
        };
    }

    @Test
    public void onMessage_ProcessIncomingInvoice_DebtorBankAccountIsNotReady() throws Exception
    {
        PubsubMessage message = iiPubsubMessage( false );
        new Expectations()
        {
            {
                debtorBank.isDebtorReady();
                result = false;
            }
        };

        tested.onMessage( message, "billing.changes" );

        new Verifications()
        {
            {
                executor.schedule( ( Task ) any );
                times = 0;

                timestamp.done();
                times = 0;
            }
        };
    }

    @Test
    public void onMessage_ProcessIncomingInvoice_UnsupportedBank() throws Exception
    {
        PubsubMessage message = iiPubsubMessage( false );
        new Expectations()
        {
            {
                debtorBank.isDebtorReady();
                result = true;

                debtorBank.getBankCode();
                result = "1100";
            }
        };

        tested.onMessage( message, "billing.changes" );

        new Verifications()
        {
            {
                executor.schedule( ( Task ) any );
                times = 0;

                timestamp.done();
                times = 0;
            }
        };
    }

    @Test
    public void onMessage_ProcessIncomingInvoiceDeletionByRevolut() throws Exception
    {
        PubsubMessage message = iiPubsubMessage( true );
        String publishTime = "2019-03-25T16:00:00.999Z";
        message.setPublishTime( publishTime );

        new Expectations()
        {
            {
                debtorBank.isDebtorReady();
                result = true;

                debtorBank.getBankCode();
                result = REVOLUT_BANK_CODE;
            }
        };

        tested.onMessage( message, "billing.changes" );

        new Verifications()
        {
            {
                Timestamp.of( anyString, ( List ) any, ( LocalAccount ) any,
                        withEqual( DateTime.parseRfc3339( publishTime ) ) );

                Task<IncomingInvoice> task;
                executor.schedule( task = withCapture() );
                times = 1;

                assertWithMessage( "Number of scheduled tasks" )
                        .that( task.countTasks() )
                        .isEqualTo( 2 );

                assertThat( task ).isInstanceOf( RevolutBeneficiarySyncTask.class );
                assertThat( task.next() ).isInstanceOf( RevolutIncomingInvoiceProcessorTask.class );

                assertWithMessage( "Entity scheduled to be deleted" )
                        .that( ( ( RevolutIncomingInvoiceProcessorTask ) task.next() ).isDelete() ).isTrue();

                timestamp.done();
            }
        };
    }

    @Test
    public void onMessage_ProcessIncomingInvoiceIgnoreObsoleteChanges() throws Exception
    {
        PubsubMessage message = iiPubsubMessage( false );
        new Expectations()
        {
            {
                timestamp.isObsolete();
                result = true;
            }
        };

        tested.onMessage( message, "billing.changes" );

        new Verifications()
        {
            {
                executor.schedule( ( Task ) any );
                times = 0;

                timestamp.done();
                times = 0;
            }
        };
    }

    @Test
    public void onMessage_ProcessInvoice() throws Exception
    {
        PubsubMessage message = invoicePubsubMessage();
        tested.onMessage( message, "billing.changes" );

        new Verifications()
        {
            {
                Task<Invoice> task;
                executor.schedule( task = withCapture() );
                times = 1;

                assertWithMessage( "Number of scheduled tasks" )
                        .that( task.countTasks() )
                        .isEqualTo( 1 );

                assertThat( task ).isInstanceOf( RevolutInvoiceProcessorTask.class );

                assertWithMessage( "Entity scheduled to be deleted" )
                        .that( ( ( RevolutInvoiceProcessorTask ) task ).isDelete() ).isFalse();

                timestamp.done();
            }
        };
    }

    private PubsubMessage orderPubsubMessage( boolean deletion )
            throws IOException
    {
        TopicMessage.Builder builder = validPubsubMessage( "purchase-order.pubsub.json",
                PurchaseOrder.class,
                PURCHASE_ORDER_ID,
                deletion );

        builder.addAttribute( ENCODED_UNIQUE_KEY, "/" + PURCHASE_ORDER_ID );
        return builder.build().getMessages().get( 0 );
    }

    private PubsubMessage invoicePubsubMessage()
            throws IOException
    {
        return invoicePubsubMessage( "invoice-sent-pubsub.json", false );
    }

    private PubsubMessage iiPubsubMessage( boolean deletion )
            throws IOException
    {
        return iiPubsubMessage( "incoming-invoice.pubsub.json", deletion );
    }

    private PubsubMessage invoicePubsubMessage( String fileName, boolean deletion )
            throws IOException
    {
        TopicMessage.Builder builder = validPubsubMessage( fileName,
                Invoice.class,
                INVOICE_ID,
                deletion );

        builder.addAttribute( ENCODED_UNIQUE_KEY, "/" + 5131678553331L + "/" + INVOICE_ID );
        return builder.build().getMessages().get( 0 );
    }

    private PubsubMessage iiPubsubMessage( String fileName, boolean deletion )
            throws IOException
    {
        TopicMessage.Builder builder = validPubsubMessage( fileName,
                IncomingInvoice.class,
                INCOMING_INVOICE_ID,
                deletion );

        builder.addAttribute( ENCODED_UNIQUE_KEY, "/" + PURCHASE_ORDER_ID + "/" + INCOMING_INVOICE_ID );
        return builder.build().getMessages().get( 0 );
    }

    private TopicMessage.Builder validPubsubMessage( String json,
                                                     Class<?> clazz,
                                                     long entityId,
                                                     boolean deletion )
            throws IOException
    {
        byte[] bytes = bytes( json, clazz );

        return validPubsubMessage( bytes, deletion )
                .addAttribute( DATA_TYPE, clazz.getSimpleName() )
                .addAttribute( ENTITY_ID, String.valueOf( entityId ) );
    }

    private TopicMessage.Builder validPubsubMessage( byte[] bytes, boolean deletion )
    {
        TopicMessage.Builder builder = TopicMessage.newBuilder();
        builder.setProjectId( "projectId" ).setTopicId( "a-topic" )
                .addMessage( bytes )
                .addAttribute( ACCOUNT_IDENTITY_ID, IDENTITY_ID )
                .addAttribute( ACCOUNT_EMAIL, EMAIL )
                .addAttribute( ACCOUNT_UNIQUE_ID, String.valueOf( ACCOUNT_ID ) )
                .addAttribute( ENTITY_DELETION, String.valueOf( deletion ) );

        return builder;
    }

    private byte[] bytes( String json, Class<?> clazz ) throws IOException
    {
        InputStream stream = clazz.getResourceAsStream( json );
        return ByteStreams.toByteArray( stream );
    }
}