package biz.turnonline.ecosystem.payment.service;

import com.google.common.base.Stopwatch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.ctoolkit.restapi.client.AuthRequest;
import org.ctoolkit.restapi.client.provider.TokenProvider;
import org.ctoolkit.services.endpoints.AudienceUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.ctoolkit.services.endpoints.ClosedServerToServerAuthenticator.INTERNAL_CALL;
import static org.ctoolkit.services.endpoints.ClosedServerToServerAuthenticator.ON_BEHALF_OF_AUDIENCE;
import static org.ctoolkit.services.endpoints.ThirdPartyToServerAuthenticator.ON_BEHALF_OF_EMAIL;
import static org.ctoolkit.services.endpoints.ThirdPartyToServerAuthenticator.ON_BEHALF_OF_USER_ID;

/**
 * The base implementation of {@link TokenProvider}.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
abstract class ClosedServerToServerToken<O>
        implements TokenProvider<O>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( ClosedServerToServerToken.class );

    private final FirebaseAuth auth;

    ClosedServerToServerToken( FirebaseAuth auth )
    {
        this.auth = checkNotNull( auth );
    }

    @Override
    public String token( @Nullable AuthRequest.AuthScheme scheme, O of )
    {
        Stopwatch stopwatch = Stopwatch.createStarted();

        AudienceUser user = convert( of );
        Map<String, Object> claims = new HashMap<>();
        claims.put( ON_BEHALF_OF_EMAIL, user.getEmail() );
        claims.put( ON_BEHALF_OF_USER_ID, user.getId() );
        claims.put( ON_BEHALF_OF_AUDIENCE, user.getAudience() );
        String token;

        try
        {
            token = auth.createCustomToken( user.getId(), claims );
            Stopwatch stopped = stopwatch.stop();
            if ( stopped.elapsed().toMillis() > 100 )
            {
                LOGGER.info( "Token creation took more than 100 ms ("
                        + stopped
                        + ") on behalf of (Identity ID): "
                        + user.getId() );
            }
            else
            {
                LOGGER.info( "Token creation took " + stopped + " on behalf of (Identity ID): " + user.getId() );
            }
        }
        catch ( FirebaseAuthException e )
        {
            LOGGER.warn( "Token failure took " + stopwatch.stop() + " on behalf of (Identity ID): " + user.getId() );
            throw new IllegalArgumentException( e );
        }

        return AuthRequest.AuthScheme.BEARER.getValue() + " " + token;
    }

    @Override
    public Map<String, String> headers( O of )
    {
        Map<String, String> headers = new HashMap<>();
        headers.put( INTERNAL_CALL, "true" );

        return headers;
    }

    protected abstract AudienceUser convert( O of );
}
