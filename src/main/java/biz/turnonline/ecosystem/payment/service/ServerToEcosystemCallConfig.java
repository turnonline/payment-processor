package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import org.ctoolkit.restapi.client.AuthRequest;
import org.ctoolkit.restapi.client.provider.TokenProvider;

import javax.annotation.Nullable;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

import static org.ctoolkit.services.endpoints.ThirdPartyToServerAuthenticator.ON_BEHALF_OF_EMAIL;
import static org.ctoolkit.services.endpoints.ThirdPartyToServerAuthenticator.ON_BEHALF_OF_USER_ID;

/**
 * Server to TurnOnline.biz Ecosystem call configuration that adds 'on behalf of' headers to the calls.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
public
class ServerToEcosystemCallConfig
        implements TokenProvider<LocalAccount>
{
    @Override
    public String token( @Nullable AuthRequest.AuthScheme scheme, LocalAccount user )
    {
        // returns null in order to retain default App Engine OAuth2 server to server authentication
        return null;
    }

    @Override
    public Map<String, String> headers( LocalAccount onBehalfOf )
    {
        Map<String, String> headers = new HashMap<>();
        headers.put( ON_BEHALF_OF_EMAIL, onBehalfOf.getEmail() );
        headers.put( ON_BEHALF_OF_USER_ID, onBehalfOf.getIdentityId() );

        return headers;
    }
}
