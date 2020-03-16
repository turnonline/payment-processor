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

import biz.turnonline.ecosystem.payment.api.model.Transaction;
import biz.turnonline.ecosystem.payment.service.LocalAccountProvider;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.pubsub.model.PublishResponse;
import com.google.cloud.ServiceOptions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.googlecode.objectify.Key;
import ma.glasnost.orika.MapperFacade;
import org.ctoolkit.restapi.client.PubSub;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.restapi.client.pubsub.TopicMessage;
import org.ctoolkit.services.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import static biz.turnonline.ecosystem.payment.service.PaymentConfig.TRANSACTION_TOPIC;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_EMAIL;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_IDENTITY_ID;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_UNIQUE_ID;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.DATA_TYPE;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ENTITY_ID;

/**
 * Dedicated task to publish {@link Transaction} changes via Pub/Sub topic {@link PaymentConfig#TRANSACTION_TOPIC}.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
class TransactionPublisherTask
        extends Task<CommonTransaction>
{
    private static final long serialVersionUID = 8492365572681403771L;

    private static final String PUB_DATA_TYPE = "Transaction";

    private static final Logger LOGGER = LoggerFactory.getLogger( TransactionPublisherTask.class );

    @Inject
    private transient RestFacade facade;

    @Inject
    @PubSub
    private transient ObjectMapper jsonMapper;

    @Inject
    private transient MapperFacade mapper;

    @Inject
    private transient LocalAccountProvider lap;

    TransactionPublisherTask( @Nonnull Key<CommonTransaction> entityKey )
    {
        super( "PubSub-Transaction" );
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

        if ( lAccount.getId() == null )
        {
            LOGGER.error( "Invalid account has been found (missing ID) " + lAccount );
            return;
        }

        if ( Strings.isNullOrEmpty( lAccount.getEmail() ) )
        {
            LOGGER.error( "Invalid account has been found (missing Email) " + lAccount );
            return;
        }

        if ( Strings.isNullOrEmpty( lAccount.getIdentityId() ) )
        {
            LOGGER.error( "Invalid account has been found (missing Identity ID) " + lAccount );
            return;
        }

        Transaction api = mapper.map( transaction, Transaction.class );
        byte[] jsonContent;

        try
        {
            jsonContent = jsonMapper.writeValueAsBytes( api );
        }
        catch ( JsonProcessingException e )
        {
            // Re-try does not make sense, this type of error looks like a development error.
            LOGGER.error( "JSON processing has failed.", e );
            LOGGER.error( String.valueOf( api ) );
            return;
        }

        Long uniqueId = checkNotNull( transaction.getId(), "Transaction expected to be already persisted" );
        String dataType = PUB_DATA_TYPE;
        String projectId = ServiceOptions.getDefaultProjectId();

        TopicMessage.Builder builder = TopicMessage.newBuilder()
                .setProjectId( projectId )
                .setTopicId( TRANSACTION_TOPIC )
                .addMessage( jsonContent, ENTITY_ID, String.valueOf( uniqueId ) )
                .addAttribute( DATA_TYPE, dataType )
                .addAttribute( ACCOUNT_EMAIL, lAccount.getEmail() )
                .addAttribute( ACCOUNT_UNIQUE_ID, String.valueOf( lAccount.getId() ) )
                .addAttribute( ACCOUNT_IDENTITY_ID, lAccount.getIdentityId() );

        TopicMessage message = builder.build();

        // Once published, returns a server-assigned message id (unique within the topic)
        PublishResponse response = facade.insert( message ).answerBy( PublishResponse.class ).finish();
        LOGGER.info( dataType + " has been published via topic '" + message.getTopic()
                + "' with message ID: " + response.getMessageIds() );

        stopwatch.stop();
        LOGGER.info( getTaskName() + " final duration " + stopwatch );
    }
}
