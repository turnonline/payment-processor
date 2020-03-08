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

package biz.turnonline.ecosystem.payment.service.revolut.webhook;

import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.TransactionBill;
import biz.turnonline.ecosystem.revolut.business.transaction.model.Transaction;
import mockit.Expectations;
import mockit.Injectable;
import org.ctoolkit.restapi.client.ClientErrorException;
import org.ctoolkit.restapi.client.NotFoundException;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.restapi.client.UnauthorizedException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.OffsetDateTime;

import static biz.turnonline.ecosystem.payment.service.revolut.webhook.TransactionCreatedFlowTest.TRANSACTION_EXT_ID;
import static biz.turnonline.ecosystem.payment.service.revolut.webhook.TransactionCreatedFlowTest.toJson;

/**
 * {@link TransactionStateChangedTask} unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class TransactionStateChangedTaskTest
{
    @Injectable
    private PaymentConfig config;

    @Injectable
    private RestFacade facade;

    private CommonTransaction transaction;

    @BeforeMethod
    public void before()
    {
        transaction = new TransactionBill( TRANSACTION_EXT_ID );
    }

    @Test
    public void successful()
    {
        TransactionStateChangedTask tested;
        tested = new TransactionStateChangedTask( toJson( "transaction-state-changed.json" ) );
        tested.setConfig( config );
        tested.setFacade( facade );

        transaction.failure( false );

        new Expectations( transaction )
        {
            {
                transaction.save();
            }
        };

        tested.execute();
    }

    @Test
    public void unsuccessful_TransactionFailed()
    {
        TransactionStateChangedTask tested;
        tested = new TransactionStateChangedTask( toJson( "transaction-state-changed.json" ) );
        tested.setConfig( config );
        tested.setFacade( facade );

        transaction.failure( true );

        new Expectations( transaction )
        {
            {
                transaction.save();
                times = 0;
            }
        };

        tested.execute();
    }

    @Test
    public void unsuccessful_AlreadyCompleted()
    {
        TransactionStateChangedTask tested;
        tested = new TransactionStateChangedTask( toJson( "transaction-state-changed.json" ) );
        tested.setConfig( config );
        tested.setFacade( facade );

        transaction.completedAt( OffsetDateTime.now() );
        transaction.failure( false );

        new Expectations( transaction )
        {
            {
                transaction.save();
                times = 0;
            }
        };

        tested.execute();
    }

    @Test
    public void unsuccessful_InvalidStructureMissingData()
    {
        TransactionStateChangedTask tested;
        tested = new TransactionStateChangedTask( toJson( "transaction-state-changed-invalid-structure.json" ) );
        tested.setConfig( config );
        tested.setFacade( facade );

        transaction.failure( false );

        new Expectations( transaction )
        {
            {
                transaction.save();
                times = 0;
            }
        };

        tested.execute();
    }

    @Test
    public void unsuccessful_TransactionNotFound()
    {
        TransactionStateChangedTask tested;
        tested = new TransactionStateChangedTask( toJson( "transaction-state-changed.json" ) );
        tested.setConfig( config );
        tested.setFacade( facade );

        transaction.failure( false );

        new Expectations( transaction )
        {
            {
                facade.get( Transaction.class ).identifiedBy( anyString ).finish();
                result = new NotFoundException();

                transaction.save();
                times = 0;
            }
        };

        tested.execute();
    }

    @Test
    public void unsuccessful_RevolutClientError()
    {
        TransactionStateChangedTask tested;
        tested = new TransactionStateChangedTask( toJson( "transaction-state-changed.json" ) );
        tested.setConfig( config );
        tested.setFacade( facade );

        transaction.failure( false );

        new Expectations( transaction )
        {
            {
                facade.get( Transaction.class ).identifiedBy( anyString ).finish();
                result = new ClientErrorException();

                transaction.save();
                times = 0;
            }
        };

        tested.execute();
    }

    @Test
    public void unsuccessful_RevolutUnauthorized()
    {
        TransactionStateChangedTask tested;
        tested = new TransactionStateChangedTask( toJson( "transaction-state-changed.json" ) );
        tested.setConfig( config );
        tested.setFacade( facade );

        transaction.failure( false );

        new Expectations( transaction )
        {
            {
                facade.get( Transaction.class ).identifiedBy( anyString ).finish();
                result = new UnauthorizedException();

                transaction.save();
                times = 0;
            }
        };

        tested.execute();
    }

    @Test
    public void unsuccessful_MissingTransactionId()
    {
        TransactionStateChangedTask tested;
        tested = new TransactionStateChangedTask( toJson( "transaction-state-changed-no-id.json" ) );
        tested.setConfig( config );
        tested.setFacade( facade );

        transaction.failure( false );

        new Expectations( transaction )
        {
            {
                transaction.save();
                times = 0;
            }
        };

        tested.execute();
    }
}