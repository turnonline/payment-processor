package biz.turnonline.ecosystem.payment.api;

import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.response.UnauthorizedException;

import javax.inject.Singleton;

/**
 * The common services to handle REST API endpoints requests.
 *
 * @author <a href="mailto:aurel.medvegy@ctoolkit.org">Aurel Medvegy</a>
 */
@Singleton
class EndpointsCommon
{
    EndpointsCommon()
    {
    }

    /**
     * For unauthorized user throws {@link UnauthorizedException} otherwise does nothing.
     *
     * @param authUser the authorized user instance
     */
    void authorize( User authUser )
            throws UnauthorizedException
    {
        if ( authUser == null )
        {
            throw new UnauthorizedException( "User is unauthorized." );
        }
    }
}
