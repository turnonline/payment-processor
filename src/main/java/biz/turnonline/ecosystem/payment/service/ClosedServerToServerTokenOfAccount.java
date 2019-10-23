package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import com.google.common.base.Stopwatch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.ctoolkit.restapi.client.AuthRequest;
import org.ctoolkit.restapi.client.provider.TokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.ctoolkit.services.endpoints.ClosedServerToServerAuthenticator.INTERNAL_CALL;
import static org.ctoolkit.services.endpoints.ClosedServerToServerAuthenticator.ON_BEHALF_OF_AUDIENCE;
import static org.ctoolkit.services.endpoints.ThirdPartyToServerAuthenticator.ON_BEHALF_OF_EMAIL;
import static org.ctoolkit.services.endpoints.ThirdPartyToServerAuthenticator.ON_BEHALF_OF_USER_ID;

/**
 * Implementation to handle {@link LocalAccount}.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
public class ClosedServerToServerTokenOfAccount
        implements TokenProvider<LocalAccount>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( ClosedServerToServerTokenOfAccount.class );

    private final FirebaseAuth auth;

    @Inject
    ClosedServerToServerTokenOfAccount( FirebaseAuth auth )
    {
        this.auth = checkNotNull( auth );
    }

    @Override
    public String token( @Nullable AuthRequest.AuthScheme scheme, LocalAccount user )
    {
        Stopwatch stopwatch = Stopwatch.createStarted();

        Map<String, Object> claims = new HashMap<>();
        claims.put( ON_BEHALF_OF_EMAIL, user.getEmail() );
        claims.put( ON_BEHALF_OF_USER_ID, user.getIdentityId() );
        claims.put( ON_BEHALF_OF_AUDIENCE, user.getAudience() );
        String token;

        try
        {
            token = auth.createCustomToken( user.getIdentityId(), claims );
            Stopwatch stopped = stopwatch.stop();
            if ( stopped.elapsed().toMillis() > 100 )
            {
                LOGGER.info( "Token creation took more than 100 ms ("
                        + stopped
                        + ") on behalf of (Account ID): "
                        + user.getId() );
            }
            else
            {
                LOGGER.info( "Token creation took " + stopped + " on behalf of (Account ID): " + user.getId() );
            }
        }
        catch ( FirebaseAuthException e )
        {
            LOGGER.warn( "Token failure took " + stopwatch.stop() + " on behalf of (Account ID): " + user.getId() );
            throw new IllegalArgumentException( e );
        }

        return AuthRequest.AuthScheme.BEARER.getValue() + " " + token;
    }

    @Override
    public Map<String, String> headers( LocalAccount of )
    {
        Map<String, String> headers = new HashMap<>();
        headers.put( INTERNAL_CALL, "true" );

        return headers;
    }
}
