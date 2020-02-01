package biz.turnonline.ecosystem.payment.subscription;

import biz.turnonline.ecosystem.billing.model.PurchaseOrder;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import com.googlecode.objectify.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * The asynchronous task to process purchase order.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
class PurchaseOrderProcessorTask
        extends JsonTask<PurchaseOrder>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( PurchaseOrderProcessorTask.class );

    private static final long serialVersionUID = 2155479255175862359L;

    PurchaseOrderProcessorTask( @Nonnull Key<LocalAccount> accountKey, @Nonnull String json, boolean delete )
    {
        super( accountKey, json, delete );
    }

    @Override
    protected void execute( @Nonnull LocalAccount debtor, @Nonnull PurchaseOrder order )
    {

    }

    @Override
    protected Class<PurchaseOrder> type()
    {
        return PurchaseOrder.class;
    }
}
