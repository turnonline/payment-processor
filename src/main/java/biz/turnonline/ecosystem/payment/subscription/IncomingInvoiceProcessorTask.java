package biz.turnonline.ecosystem.payment.subscription;

import biz.turnonline.ecosystem.billing.model.IncomingInvoice;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import com.googlecode.objectify.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * The asynchronous task to process incoming invoice.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
class IncomingInvoiceProcessorTask
        extends JsonTask<IncomingInvoice>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( IncomingInvoiceProcessorTask.class );

    IncomingInvoiceProcessorTask( @Nonnull Key<LocalAccount> accountKey, @Nonnull String json, boolean delete )
    {
        super( accountKey, json, delete );
    }

    @Override
    protected void execute( @Nonnull LocalAccount debtor, @Nonnull IncomingInvoice invoice )
    {
        if ( invoice.getPayment() != null )
        {
            String uniqueKey = invoice.getOrderId() + "/" + invoice.getId();
            LOGGER.warn( "Incoming Invoice identified by '"
                    + uniqueKey
                    + "' missing payment, nothing to do." );
            return;
        }
    }

    @Override
    protected Class<IncomingInvoice> type()
    {
        return IncomingInvoice.class;
    }
}
