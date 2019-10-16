package biz.turnonline.ecosystem.payment.service;

import com.google.firebase.auth.FirebaseAuth;
import org.ctoolkit.services.endpoints.AudienceUser;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation to handle {@link AudienceUser}.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
class ClosedServerToServerTokenOfUser
        extends ClosedServerToServerToken<AudienceUser>
{
    @Inject
    ClosedServerToServerTokenOfUser( FirebaseAuth auth )
    {
        super( auth );
    }

    @Override
    protected AudienceUser convert( AudienceUser of )
    {
        return of;
    }
}
