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

package biz.turnonline.ecosystem.payment.webhook;

import org.ctoolkit.services.task.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.stream.Collectors;

/**
 * Revolut web-hook mechanism that allows you to receive updates about business account.
 * Currently the following events are supported:
 * <ul>
 *     <li>Transaction Creation (TransactionCreated)</li>
 *     <li>Transaction State Change (TransactionStateChanged)</li>
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

    @Inject
    public RevolutWebhookSubscription( TaskExecutor executor )
    {
        this.executor = executor;
    }

    @Override
    protected void doPost( HttpServletRequest request, HttpServletResponse response )
            throws IOException
    {
        process( request );
    }

    @Override
    protected void doPut( HttpServletRequest request, HttpServletResponse response )
            throws IOException
    {
        process( request );
    }

    private void process( HttpServletRequest request ) throws IOException
    {
        String referer = request.getHeader( "referer" );
        LOGGER.info( "Referer " + referer );
        LOGGER.info( "RemoteAddr " + request.getRemoteAddr() );
        LOGGER.info( "LocalAddr " + request.getLocalAddr() );

        Enumeration<String> parameterNames = request.getParameterNames();
        while ( parameterNames.hasMoreElements() )
        {
            String param = parameterNames.nextElement();
            LOGGER.info( "Request parameter [" + param + " - " + request.getParameter( param ) + "]" );
        }

        Enumeration<String> headerNames = request.getHeaderNames();
        String name;
        String value;
        while ( headerNames.hasMoreElements() )
        {
            name = headerNames.nextElement();
            value = request.getHeader( name );
            LOGGER.info( "Request header [" + name + " - " + value + "]" );
        }

        ServletInputStream stream = request.getInputStream();
        String body = new BufferedReader( new InputStreamReader( stream ) )
                .lines()
                .collect( Collectors.joining( "\n" ) );
        LOGGER.info( body );
    }
}
