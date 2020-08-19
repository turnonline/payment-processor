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

import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.revolut.webhook.TransactionCreatedTask;
import biz.turnonline.ecosystem.payment.service.revolut.webhook.TransactionStateChanged;
import biz.turnonline.ecosystem.payment.service.revolut.webhook.TransactionStateChangedTask;
import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.ctoolkit.services.task.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;

/**
 * Revolut web-hook mechanism that allows you to receive updates about business account.
 * Once a valid event has been received a corresponding task will be scheduled to process the event.
 * Currently the following events are supported:
 * <ul>
 *     <li>Transaction created processed by {@link TransactionCreatedTask}</li>
 *     <li>{@link TransactionStateChanged} processed by {@link TransactionStateChangedTask}</li>
 * </ul>
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
public class RevolutWebhookSubscription
        extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger( RevolutWebhookSubscription.class );

    private final TaskExecutor executor;

    private final PaymentConfig config;

    @Inject
    public RevolutWebhookSubscription( TaskExecutor executor, PaymentConfig config )
    {
        this.executor = executor;
        this.config = config;
    }

    @Override
    protected void doPost( HttpServletRequest request, HttpServletResponse response )
            throws IOException
    {
        process( request, response );
    }

    @Override
    protected void doPut( HttpServletRequest request, HttpServletResponse response )
            throws IOException
    {
        process( request, response );
    }

    private void process( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        Enumeration<String> headerNames = request.getHeaderNames();
        String name;
        String value;
        while ( headerNames.hasMoreElements() )
        {
            name = headerNames.nextElement();
            value = request.getHeader( name );
            LOGGER.info( "Request header [" + name + " - " + value + "]" );
        }

        InputStream stream = request.getInputStream();
        if ( stream == null )
        {
            response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
            return;
        }

        // Parse the JSON event in order to read first the event type
        JsonElement element = JsonParser.parseReader( new InputStreamReader( stream ) );
        JsonObject jsonObject = element.getAsJsonObject();
        JsonElement jsonEvent = jsonObject.get( "event" );
        String event = jsonEvent == null ? "" : jsonEvent.getAsString();
        JsonElement dataElement = jsonObject.get( "data" );
        JsonElement idElement = dataElement == null ? null : dataElement.getAsJsonObject().get( "id" );
        String id = idElement == null ? null : idElement.getAsString();

        if ( Strings.isNullOrEmpty( id ) )
        {
            response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
            return;
        }

        CommonTransaction transaction = config.initGetTransaction( id );

        switch ( event )
        {
            case "TransactionCreated":
            {
                TransactionCreatedTask task = new TransactionCreatedTask( dataElement.toString() );
                task.addNext( new TransactionPublisherTask( transaction.entityKey() ) );
                executor.schedule( task );
                LOGGER.info( event + " task scheduled" );
                break;
            }
            case "TransactionStateChanged":
            {
                TransactionStateChangedTask task = new TransactionStateChangedTask( jsonObject.toString() );
                // TECO-238 ignore publishing of the transaction if it's incomplete yet (race condition issue)
                task.addNext( new TransactionPublisherTask( transaction.entityKey() ), CommonTransaction::isIncomplete );
                executor.schedule( task );
                LOGGER.info( event + " task scheduled" );
                break;
            }
            default:
            {
                LOGGER.warn( "Unknown Revolut web-hook event type: " + event );
                LOGGER.info( "Request body: " + jsonObject );
            }
        }

        // temporary logging of the body
        LOGGER.info( jsonObject.toString() );
    }
}
