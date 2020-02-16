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

package biz.turnonline.ecosystem.payment.api;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiIssuerAudience;
import com.google.api.server.spi.config.ApiNamespace;
import org.ctoolkit.services.endpoints.ClosedServerToServerAuthenticator;
import org.ctoolkit.services.endpoints.FirebaseJwtAuthenticator;
import org.ctoolkit.services.endpoints.ThirdPartyToServerAuthenticator;

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
        description = "TurnOnline.biz Ecosystem Payment Processor",
        documentationLink = "https://developers.turnonline.biz/docs/payment.turnon.cloud/1",
        namespace = @ApiNamespace( ownerDomain = "ecosystem.turnonline.biz", ownerName = "TurnOnline.biz, s.r.o." ),
        authenticators = {
                ClosedServerToServerAuthenticator.class,
                FirebaseJwtAuthenticator.class,
                ThirdPartyToServerAuthenticator.class
        },
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
