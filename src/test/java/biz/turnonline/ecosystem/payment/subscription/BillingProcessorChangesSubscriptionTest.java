package biz.turnonline.ecosystem.payment.subscription;

import biz.turnonline.ecosystem.bill.model.Bill;
import biz.turnonline.ecosystem.payment.service.LocalAccountProvider;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.TransactionNotFound;
import biz.turnonline.ecosystem.payment.service.model.TransactionInvoice;
import biz.turnonline.ecosystem.payment.service.model.TransactionReceipt;
import com.google.api.services.pubsub.model.PubsubMessage;
import com.google.common.io.ByteStreams;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.ctoolkit.restapi.client.NotFoundException;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.restapi.client.pubsub.TopicMessage;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.DATA_TYPE;

/**
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public class BillingProcessorChangesSubscriptionTest
{

    @Tested
    private BillingProcessorChangesSubscription tested;

    @Injectable
    private RestFacade facade;

    @Injectable
    private PaymentConfig config;

    @Injectable
    private LocalAccountProvider lap;

    @Mocked
    private TransactionReceipt transactionReceipt;

    @Test
    public void onMessage_ProductBillingTransaction_NotExists() throws Exception
    {
        PubsubMessage message = validPubsubMessage();

        new Expectations()
        {
            {
                facade.get( biz.turnonline.ecosystem.billing.model.Transaction.class )
                        .identifiedBy( 4831426297987072L )
                        .finish();
                result = new NotFoundException();
            }
        };

        tested.onMessage( message, "bill.changes" );

        new Verifications()
        {
            {
                transactionReceipt.save();
                times = 0;
            }
        };
    }

    @Test
    public void onMessage_PaymentProcessorTransaction_NotExists() throws Exception
    {
        PubsubMessage message = validPubsubMessage();
        biz.turnonline.ecosystem.billing.model.Transaction productBillingTransaction = new biz.turnonline.ecosystem.billing.model.Transaction();
        productBillingTransaction.setTransactionId( 1L );

        new Expectations()
        {
            {
                facade.get( biz.turnonline.ecosystem.billing.model.Transaction.class )
                        .identifiedBy( 4831426297987072L )
                        .finish();
                result = productBillingTransaction;

                config.getTransaction( 1L );
                result = new TransactionNotFound("Transaction not found");
            }
        };

        tested.onMessage( message, "bill.changes" );

        new Verifications()
        {
            {
                transactionReceipt.save();
                times = 0;
            }
        };
    }

    @Test
    public void onMessage_PaymentProcessorTransaction_Exists_WrongType() throws Exception
    {
        PubsubMessage message = validPubsubMessage();
        biz.turnonline.ecosystem.billing.model.Transaction productBillingTransaction = new biz.turnonline.ecosystem.billing.model.Transaction();
        productBillingTransaction.setTransactionId( 1L );

        TransactionInvoice transaction = new TransactionInvoice( 10L, 100L );

        new Expectations()
        {
            {
                facade.get( biz.turnonline.ecosystem.billing.model.Transaction.class )
                        .identifiedBy( 4831426297987072L )
                        .finish();
                result = productBillingTransaction;

                config.getTransaction( 1L );
                result = transaction;
            }
        };

        tested.onMessage( message, "bill.changes" );

        new Verifications()
        {
            {
                transactionReceipt.save();
                times = 0;
            }
        };
    }

    @Test
    public void onMessage_PaymentProcessorTransaction_Exists_CorrectType() throws Exception
    {
        PubsubMessage message = validPubsubMessage();
        biz.turnonline.ecosystem.billing.model.Transaction productBillingTransaction = new biz.turnonline.ecosystem.billing.model.Transaction();
        productBillingTransaction.setTransactionId( 1L );

        new Expectations()
        {
            {
                facade.get( biz.turnonline.ecosystem.billing.model.Transaction.class )
                        .identifiedBy( 4831426297987072L )
                        .finish();
                result = productBillingTransaction;

                config.getTransaction( 1L );
                result = transactionReceipt;
            }
        };

        tested.onMessage( message, "bill.changes" );

        new Verifications()
        {
            {
                transactionReceipt.setReceipt( 5168421444517888L );
                times = 1;

                transactionReceipt.save();
                times = 1;
            }
        };
    }

    private PubsubMessage validPubsubMessage() throws IOException
    {
        TopicMessage.Builder builder = TopicMessage.newBuilder();
        builder.setProjectId( "projectId" )
                .setTopicId( "a-topic" )
                .addMessage( bytes() )
                .addAttribute( DATA_TYPE, Bill.class.getSimpleName() );

        return builder.build().getMessages().get( 0 );
    }

    private byte[] bytes() throws IOException
    {
        InputStream stream = Bill.class.getResourceAsStream( "bill-change.pubsub.json" );
        return ByteStreams.toByteArray( stream );
    }
}