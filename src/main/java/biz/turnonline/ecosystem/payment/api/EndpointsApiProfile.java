package biz.turnonline.ecosystem.payment.api;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiNamespace;

/**
 * The endpoint profile, the base class as a configuration of the REST API and generated client.
 * <p>
 * If you need more fine grained authentication, for example to white list your {@link #OAUTH_CLIENT_ID}
 * with Google Accounts, uncomment clientIds, and audiences (at this level the security schemes
 * applies to the entire API). Thu current implementation of {@link MessageEndpoint} allows
 * authenticated users to call the API with no additional restriction.
 *
 * @author <a href="mailto:aurel.medvegy@ctoolkit.org">Aurel Medvegy</a>
 * @see <a href="https://cloud.google.com/endpoints/docs/frameworks/java/authenticating-users">Authenticating Users</a>
 * @see <a href="https://cloud.google.com/endpoints/docs/openapi/when-why-api-key">Why and When to Use API Keys</a>
 */
@Api( name = "myApiName",
        canonicalName = "API Name",
        title = "Example REST API",
        version = "v1",
        description = "Example REST API",
        documentationLink = "https://ecosystem.turnonline.biz/docs",
        namespace = @ApiNamespace( ownerDomain = "ecosystem.turnonline.biz", ownerName = "Example, Ltd." )/*,
        clientIds = {OAUTH_CLIENT_ID},
        audiences = {OAUTH_CLIENT_ID}*/
)
class EndpointsApiProfile
{
    static final String OAUTH_CLIENT_ID = "YOUR_OAUTH_CLIENT_ID";
}
