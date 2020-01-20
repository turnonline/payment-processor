package biz.turnonline.ecosystem.payment.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    @Override
    protected void doGet( HttpServletRequest request, HttpServletResponse response )
    {
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

        response.setStatus( HttpServletResponse.SC_OK );
    }
}
