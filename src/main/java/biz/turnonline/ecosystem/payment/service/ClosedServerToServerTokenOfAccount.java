package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import com.google.firebase.auth.FirebaseAuth;
import org.ctoolkit.services.endpoints.AudienceUser;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation to handle {@link LocalAccount}.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
class ClosedServerToServerTokenOfAccount
        extends ClosedServerToServerToken<LocalAccount>
{
    @Inject
    ClosedServerToServerTokenOfAccount( FirebaseAuth auth )
    {
        super( auth );
    }

    @Override
    protected AudienceUser convert( LocalAccount of )
    {
        return new AudienceUser.Builder()
                .userId( of.getIdentityId() )
                .email( of.getEmail() )
                .audience( of.getAudience() )
                .build();
    }
}
