package biz.turnonline.ecosystem.payment.subscription;

import biz.turnonline.ecosystem.billing.model.IncomingInvoice;
import biz.turnonline.ecosystem.billing.model.PurchaseOrder;
import biz.turnonline.ecosystem.payment.service.LocalAccountProvider;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.payment.service.model.Timestamp;
import com.google.api.client.util.DateTime;
import com.google.api.services.pubsub.model.PubsubMessage;
import com.google.common.io.ByteStreams;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.ctoolkit.restapi.client.pubsub.TopicMessage;
import org.ctoolkit.services.task.Task;
import org.ctoolkit.services.task.TaskExecutor;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_AUDIENCE;
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
@SuppressWarnings( {"unchecked", "ConstantConditions", "rawtypes"} )
public class ProductBillingChangesSubscriptionTest
{
    private static final String EMAIL = "debtor.account@turnonline.biz";

    private static final long ACCOUNT_ID = 565728947417L;

    private static final String IDENTITY_ID = "CvbTgHmdkl23h5";

    private static final long PURCHASE_ORDER_ID = 565941286574L;

    private static final long INCOMING_INVOICE_ID = 571714185671L;

    @Tested
    private ProductBillingChangesSubscription tested;

    @Injectable
    private TaskExecutor executor;

    @Injectable
    private LocalAccountProvider lap;

    @Mocked
    private Timestamp timestamp;

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
    public void onMessage_ProcessIncomingInvoice() throws Exception
    {
        PubsubMessage message = invoicePubsubMessage( false );

        tested.onMessage( message, "billing.changes" );

        new Verifications()
        {
            {
                Task<IncomingInvoice> task;
                executor.schedule( task = withCapture() );
                times = 1;

                assertWithMessage( "Number of scheduled tasks" )
                        .that( task.countTasks() )
                        .isEqualTo( 1 );

                assertThat( task ).isInstanceOf( IncomingInvoiceProcessorTask.class );

                assertWithMessage( "Entity scheduled to be deleted" )
                        .that( ( ( IncomingInvoiceProcessorTask ) task ).isDelete() ).isFalse();

                timestamp.done();
            }
        };
    }

    @Test
    public void onMessage_ProcessIncomingInvoiceDeletion() throws Exception
    {
        PubsubMessage message = invoicePubsubMessage( true );
        String publishTime = "2019-03-25T16:00:00.999Z";
        message.setPublishTime( publishTime );

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
                        .isEqualTo( 1 );

                assertThat( task ).isInstanceOf( IncomingInvoiceProcessorTask.class );

                assertWithMessage( "Entity scheduled to be deleted" )
                        .that( ( ( IncomingInvoiceProcessorTask ) task ).isDelete() ).isTrue();

                timestamp.done();
            }
        };
    }

    @Test
    public void onMessage_ProcessIncomingInvoiceIgnoreObsoleteChanges() throws Exception
    {
        PubsubMessage message = invoicePubsubMessage( false );
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

    private PubsubMessage invoicePubsubMessage( boolean deletion )
            throws IOException
    {
        TopicMessage.Builder builder = validPubsubMessage( "incoming-invoice.pubsub.json",
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
                .addAttribute( ACCOUNT_AUDIENCE, "turn-online-2b" )
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