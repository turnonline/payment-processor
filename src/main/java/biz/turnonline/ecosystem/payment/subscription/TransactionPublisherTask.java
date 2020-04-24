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

import biz.turnonline.ecosystem.payment.api.model.Bill;
import biz.turnonline.ecosystem.payment.api.model.Transaction;
import biz.turnonline.ecosystem.payment.api.model.TransactionBank;
import biz.turnonline.ecosystem.payment.service.LocalAccountProvider;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import com.google.api.client.util.DateTime;
import com.google.common.base.Stopwatch;
import com.googlecode.objectify.Key;
import ma.glasnost.orika.MapperFacade;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.services.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Dedicated task to push {@link biz.turnonline.ecosystem.billing.model.Transaction}
 * events to TurnOnline.biz Ecosystem Product Billing service.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
class TransactionPublisherTask
        extends Task<CommonTransaction>
{
    private static final long serialVersionUID = 5105482532174825094L;

    private static final Logger LOGGER = LoggerFactory.getLogger( TransactionPublisherTask.class );

    @Inject
    private transient RestFacade facade;

    @Inject
    private transient MapperFacade mapper;

    @Inject
    private transient LocalAccountProvider lap;

    TransactionPublisherTask( @Nonnull Key<CommonTransaction> entityKey )
    {
        super( "Push" );
        setEntityKey( checkNotNull( entityKey, "The transaction key can't be null" ) );
    }

    @Override
    public final void execute()
    {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Key<CommonTransaction> key = getEntityKey();

        CommonTransaction transaction = workWith();

        if ( transaction == null )
        {
            LOGGER.error( "Transaction has not found for specified key '" + key + "'" );
            return;
        }

        LocalAccount lAccount = lap.get();
        if ( lAccount == null )
        {
            LOGGER.error( "Local account has not been configured yet." );
            return;
        }

        Transaction api = mapper.map( transaction, Transaction.class );
        biz.turnonline.ecosystem.billing.model.Transaction pbt;
        pbt = new biz.turnonline.ecosystem.billing.model.Transaction();

        pbt.setAmount( api.getAmount() );
        pbt.setBalance( api.getBalance() );
        pbt.setCredit( api.isCredit() );
        pbt.setCurrency( api.getCurrency() );
        pbt.setReference( api.getReference() );
        pbt.setKey( api.getKey() );
        pbt.setStatus( api.getStatus() );
        pbt.setType( api.getType() );

        Date completedAt = api.getCompletedAt();
        pbt.setCompletedAt( completedAt == null ? null : new DateTime( completedAt ) );

        TransactionBank bankAccount = api.getBankAccount();
        if ( bankAccount != null )
        {
            biz.turnonline.ecosystem.billing.model.TransactionBank bank;
            bank = new biz.turnonline.ecosystem.billing.model.TransactionBank();
            bank.setCode( bankAccount.getCode() );
            bank.setIban( bankAccount.getIban() );

            pbt.setBankAccount( bank );
        }

        Bill apiBill = api.getBill();
        if ( apiBill != null )
        {
            biz.turnonline.ecosystem.billing.model.Bill bill;
            bill = new biz.turnonline.ecosystem.billing.model.Bill();
            bill.setId( apiBill.getId() );
            bill.setOrderId( apiBill.getOrderId() );
            bill.setInvoiceId( apiBill.getInvoiceId() );

            pbt.setBill( bill );
        }

        // transaction from product-billing service to be pushed
        facade.insert( pbt )
                .onBehalfOf( lAccount )
                .finish();

        stopwatch.stop();
        LOGGER.info( Transaction.class.getSimpleName()
                + " has been pushed to product-billing service. "
                + getTaskName()
                + " final duration "
                + stopwatch );
    }
}
