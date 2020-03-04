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

import biz.turnonline.ecosystem.billing.model.IncomingInvoice;
import biz.turnonline.ecosystem.billing.model.InvoicePayment;
import biz.turnonline.ecosystem.billing.model.PurchaseOrder;
import biz.turnonline.ecosystem.payment.service.LocalAccountProvider;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.payment.service.model.Timestamp;
import biz.turnonline.ecosystem.payment.service.model.Transaction;
import com.google.api.client.util.DateTime;
import com.google.api.services.pubsub.model.PubsubMessage;
import com.google.common.base.Strings;
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
import static biz.turnonline.ecosystem.payment.service.model.FormOfPayment.TRANSFER;
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
 * Payment (bank transfer) will be scheduled only if one of the condition is being matched:
 * <ul>
 *     <li>{@link InvoicePayment#getMethod()} is TRANSFER</li>
 *     <li>{@link InvoicePayment#getMethod()} is {@code null}, meaning not configured at all,
 *     but rest of the payment properties are valid for bank transfer</li>
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
                IncomingInvoice invoice = command.fromData( IncomingInvoice.class );
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

                String method = payment.getMethod();
                if ( !Strings.isNullOrEmpty( method ) && !TRANSFER.name().equals( method ) )
                {
                    LOGGER.warn( "Incoming invoice identified by '"
                            + uniqueKey
                            + "' is not eligible to make a payment for payment method: "
                            + method );
                    return;
                }

                switch ( debtorBank.getBankCode() )
                {
                    case REVOLUT_BANK_CODE:
                    {
                        // prepares an empty transaction to be completed later (idempotent call)
                        Transaction tDraft = config.createTransactionDraft( account, invoice );

                        // incoming invoice has been successfully de-serialized, schedule processing
                        Key<LocalAccount> accountKey = account.entityKey();
                        Key<CompanyBankAccount> debtorBankKey = debtorBank.entityKey();

                        Task<IncomingInvoice> tasks = new RevolutBeneficiarySyncTask( accountKey, data, debtorBankKey );
                        tasks.addNext( new RevolutIncomingInvoiceProcessorTask( accountKey, data, delete, debtorBankKey, tDraft ) );

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
                PurchaseOrder order = command.fromData( PurchaseOrder.class );
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
