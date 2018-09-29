package biz.turnonline.ecosystem.payment.api;

import com.google.api.server.spi.auth.EspAuthenticator;
import com.google.api.server.spi.auth.GoogleOAuth2Authenticator;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiIssuerAudience;
import com.google.api.server.spi.config.ApiNamespace;
import org.ctoolkit.services.endpoints.ServerToServerAuthenticator;

import static biz.turnonline.ecosystem.payment.api.PaymentsApiProfile.CURRENT_VERSION;
import static biz.turnonline.ecosystem.payment.api.PaymentsApiProfile.PROJECT_ID;

/**
 * The endpoint profile, the base class as a configuration of the REST API and generated client.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 * @see <a href="https://cloud.google.com/endpoints/docs/frameworks/java/authenticating-users">Authenticating Users</a>
 * @see <a href="https://cloud.google.com/endpoints/docs/openapi/when-why-api-key">Why and When to Use API Keys</a>
 */
@Api( name = "payment",
        canonicalName = "Payment Processor",
        title = "TurnOnline.biz Payment Processor",
        version = CURRENT_VERSION,
        description = "TurnOnline.biz Ecosystem: Payment Processor REST API",
        documentationLink = "https://developers.turnonline.biz",
        namespace = @ApiNamespace( ownerDomain = "ecosystem.turnonline.biz", ownerName = "Comvai, s.r.o." ),
        // GoogleOAuth2Authenticator is here for now only for development purpose
        authenticators = {ServerToServerAuthenticator.class, GoogleOAuth2Authenticator.class, EspAuthenticator.class},
        issuers = {
                @ApiIssuer(
                        name = "firebase",
                        issuer = "https://securetoken.google.com/" + PROJECT_ID,
                        jwksUri = "https://www.googleapis.com/service_accounts/v1/metadata/x509/securetoken@system.gserviceaccount.com" )
        },
        issuerAudiences = {
                @ApiIssuerAudience( name = "firebase", audiences = PROJECT_ID )
        }
)
class PaymentsApiProfile
{
    static final String CURRENT_VERSION = "v1";

    static final String API_NAME = "payment";

    static final String PROJECT_ID = "turn-online-eu";
}
