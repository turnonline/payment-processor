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

package biz.turnonline.ecosystem.payment.oauth;

import com.googlecode.objectify.Key;
import org.apache.http.entity.ContentType;
import org.ctoolkit.services.task.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

/**
 * OAuth authorisation details - redirect processing.
 * The servlet that reads {@code code}
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
public class RevolutOauth2AuthRedirect
        extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger( RevolutOauth2AuthRedirect.class );

    private final RevolutCredentialAdministration administration;

    private final TaskExecutor executor;

    @Inject
    public RevolutOauth2AuthRedirect( RevolutCredentialAdministration administration,
                                      TaskExecutor executor )
    {
        this.administration = administration;
        this.executor = executor;
    }

    @Override
    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        String referer = request.getHeader( "referer" );
        String allowed;

        if ( referer != null && !referer.endsWith( "/" ) )
        {
            allowed = referer + "/";
        }
        else
        {
            allowed = referer;
        }

        // basic check of allowed referer
        if ( !( "https://business.revolut.com/".equalsIgnoreCase( allowed )
                || "https://sandbox-b2b.revolut.com/".equalsIgnoreCase( allowed ) ) )
        {
            LOGGER.warn( "Not allowed referer " + referer );
            response.setStatus( HttpServletResponse.SC_UNAUTHORIZED );
            responseUnauthorized( response );
            return;
        }

        String[] path = request.getRequestURI().split( "/" );

        for ( int index = 1; index < path.length; index++ )
        {
            LOGGER.info( index + " = " + path[index] );
        }

        String code = request.getParameter( "code" );
        LOGGER.info( "Code: " + code );

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

        if ( code != null )
        {
            try
            {
                Key<RevolutCertMetadata> detailsKey = administration.storeCode( code );
                executor.schedule( new RevolutExchangeAuthorisationCode( detailsKey ) );

                response.setStatus( HttpServletResponse.SC_OK );
                responseOk( response );
            }
            catch ( Exception e )
            {
                LOGGER.error( "Authorisation Code processing has failed", e );
                response.setStatus( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
                responseFailure( response );
            }
        }
        else
        {
            response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
            responseInvalid( response );
        }

    }

    private void responseOk( HttpServletResponse response ) throws IOException
    {
        response.setContentType( ContentType.TEXT_HTML.getMimeType() );

        PrintWriter out = response.getWriter();
        String body = "<html>\n" +
                "<body>\n" +
                "<h1>Authorisation code processing has started</h1>\n" +
                "</body>\n" +
                "</html>";

        out.println( body );
        out.close();
    }

    private void responseUnauthorized( HttpServletResponse response ) throws IOException
    {
        response.setContentType( ContentType.TEXT_HTML.getMimeType() );

        PrintWriter out = response.getWriter();
        String body = "<html>\n" +
                "<body>\n" +
                "<h1>Unauthorized</h1>\n" +
                "</body>\n" +
                "</html>";

        out.println( body );
        out.close();
    }

    private void responseInvalid( HttpServletResponse response ) throws IOException
    {
        response.setContentType( ContentType.TEXT_HTML.getMimeType() );

        PrintWriter out = response.getWriter();
        String body = "<html>\n" +
                "<body>\n" +
                "<h1>Invalid request</h1>\n" +
                "</body>\n" +
                "</html>";

        out.println( body );
        out.close();
    }

    private void responseFailure( HttpServletResponse response ) throws IOException
    {
        response.setContentType( ContentType.TEXT_HTML.getMimeType() );

        PrintWriter out = response.getWriter();
        String body = "<html>\n" +
                "<body>\n" +
                "<h1>Something went wrong, try again please</h1>\n" +
                "</body>\n" +
                "</html>";

        out.println( body );
        out.close();
    }
}
