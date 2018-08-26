package biz.turnonline.ecosystem.payment.api;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiNamespace;

import static biz.turnonline.ecosystem.payment.api.PaymentsApiProfile.CURRENT_VERSION;

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
        documentationLink = "https://ecosystem.turnonline.biz/docs",
        namespace = @ApiNamespace( ownerDomain = "ecosystem.turnonline.biz", ownerName = "Comvai, s.r.o." )
)
class PaymentsApiProfile
{
    static final String CURRENT_VERSION = "v1";

    static final String API_NAME = "payment";
}