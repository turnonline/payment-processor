package biz.turnonline.ecosystem.payment.subscription;

import biz.turnonline.ecosystem.billing.model.IncomingInvoice;
import biz.turnonline.ecosystem.billing.model.InvoicePayment;
import biz.turnonline.ecosystem.billing.model.PurchaseOrder;
import biz.turnonline.ecosystem.payment.service.LocalAccountProvider;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.payment.service.model.Timestamp;
import com.google.api.client.util.DateTime;
import com.google.api.services.pubsub.model.PubsubMessage;
import com.googlecode.objectify.Key;
import org.ctoolkit.restapi.client.NotFoundException;
import org.ctoolkit.restapi.client.pubsub.PubsubCommand;
import org.ctoolkit.restapi.client.pubsub.PubsubMessageListener;
import org.ctoolkit.services.task.Task;
import org.ctoolkit.services.task.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

import static biz.turnonline.ecosystem.payment.service.PaymentConfig.REVOLUT_BANK_CODE;
import static biz.turnonline.ecosystem.payment.service.PaymentConfig.TRUST_PAY_BANK_CODE;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_AUDIENCE;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_EMAIL;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_IDENTITY_ID;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_UNIQUE_ID;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.DATA_TYPE;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ENCODED_UNIQUE_KEY;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ENTITY_ID;

/**
 * The 'billing.changes' subscription listener implementation.
 * Processing following resources:
 * <ul>
 * <li>{@link PurchaseOrder}</li>
 * <li>{@link IncomingInvoice}</li>
 * </ul>
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
class ProductBillingChangesSubscription
        implements PubsubMessageListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger( ProductBillingChangesSubscription.class );

    private static final long serialVersionUID = 2547878701538798649L;

    private final TaskExecutor executor;

    private final LocalAccountProvider lap;

    private final PaymentConfig config;

    @Inject
    ProductBillingChangesSubscription( TaskExecutor executor,
                                       LocalAccountProvider lap,
                                       PaymentConfig config )
    {
        this.executor = executor;
        this.lap = lap;
        this.config = config;
    }

    @Override
    public void onMessage( @Nonnull PubsubMessage message, @Nonnull String subscription ) throws Exception
    {
        PubsubCommand command = new PubsubCommand( message );

        String[] mandatory = {
                ENTITY_ID,
                DATA_TYPE,
                ENCODED_UNIQUE_KEY,
                ACCOUNT_IDENTITY_ID,
                ACCOUNT_UNIQUE_ID,
                ACCOUNT_EMAIL,
                ACCOUNT_AUDIENCE
        };
        if ( !command.validate( mandatory ) )
        {
            LOGGER.error( "Some of the mandatory attributes "
                    + Arrays.toString( mandatory )
                    + " are missing, incoming attributes: "
                    + message.getAttributes() );
            return;
        }

        List<String> uniqueKey = command.getUniqueKey();
        String dataType = command.getDataType();
        boolean delete = command.isDelete();
        String accountEmail = command.getAccountEmail();
        String accountAudience = command.getAccountAudience();
        Long accountId = command.getAccountId();

        DateTime publishTime = command.getPublishDateTime();
        String data = message.getData();

        LOGGER.info( "[" + subscription + "] " + dataType + " has been received at publish time "
                + publishTime
                + " with length: "
                + data.length() + " and unique key: '" + uniqueKey + "'" + ( delete ? " to be deleted" : "" ) );

        LocalAccount account;
        LocalAccountProvider.Builder builder = new LocalAccountProvider.Builder()
                .email( accountEmail )
                .identityId( command.getAccountIdentityId() )
                .audience( accountAudience )
                .accountId( accountId );

        try
        {
            account = lap.initGet( builder );
        }
        catch ( NotFoundException e )
        {
            LOGGER.warn( "Processing of the message has been retired for: " + builder );
            return;
        }

        switch ( dataType )
        {
            case "IncomingInvoice":
            {
                IncomingInvoice invoice = fromString( data, IncomingInvoice.class );
                DateTime last = delete && publishTime != null ? publishTime : invoice.getModificationDate();

                Timestamp timestamp = Timestamp.of( dataType, uniqueKey, account, last );
                if ( timestamp.isObsolete() )
                {
                    LOGGER.info( "Incoming Invoice changes are obsolete, nothing to do " + timestamp.getName() );
                    return;
                }

                InvoicePayment payment = invoice.getPayment();
                CompanyBankAccount debtorBank;
                if ( invoice.getPayment() != null )
                {
                    debtorBank = config.getDebtorBankAccount( account, payment );
                    if ( debtorBank == null || !debtorBank.isDebtorReady() )
                    {
                        LOGGER.warn( "Debtor '" + account.getId() + "' bank account is not ready yet to be debited" );
                        return;
                    }
                }
                else
                {
                    LOGGER.warn( "Incoming invoice identified by '" + uniqueKey + "' is missing payment" );
                    return;
                }

                switch ( debtorBank.getBankCode() )
                {
                    case REVOLUT_BANK_CODE:
                    {
                        // incoming invoice has been successfully de-serialized, schedule processing
                        Key<LocalAccount> accountKey = account.entityKey();
                        Key<CompanyBankAccount> debtorBankKey = debtorBank.entityKey();

                        Task<IncomingInvoice> tasks = new RevolutBeneficiarySyncTask( accountKey, data, debtorBankKey );
                        tasks.addNext( new RevolutIncomingInvoiceProcessorTask( accountKey, data, delete, debtorBankKey ) );

                        executor.schedule( tasks );
                        timestamp.done();
                        break;
                    }
                    case TRUST_PAY_BANK_CODE:
                    default:
                    {
                        LOGGER.info( "Unsupported bank to be debited via API '" + debtorBank.getBankCode() + "'" );
                        return;
                    }
                }
                break;
            }
            case "PurchaseOrder":
            {
                PurchaseOrder order = fromString( data, PurchaseOrder.class );
                DateTime last = delete && publishTime != null ? publishTime : order.getModificationDate();

                Timestamp timestamp = Timestamp.of( dataType, uniqueKey, account, last );
                if ( timestamp.isObsolete() )
                {
                    LOGGER.info( "Incoming Order changes are obsolete, nothing to do " + timestamp.getName() );
                    return;
                }

                // purchase order has been successfully de-serialized, schedule processing
                executor.schedule( new PurchaseOrderProcessorTask( account.entityKey(), data, delete ) );
                timestamp.done();
                break;
            }
            default:
            {
                LOGGER.info( "Uninterested data type '" + dataType + "'" );
            }
        }
    }
}
