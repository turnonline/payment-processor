package biz.turnonline.ecosystem.payment.service.model;

import biz.turnonline.ecosystem.payment.service.LocalAccountProvider;
import biz.turnonline.ecosystem.steward.model.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * The {@link LocalAccountProvider} implementation.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
public class LocalAccountProviderImpl
        implements LocalAccountProvider
{
    private static final Logger logger = LoggerFactory.getLogger( LocalAccountProviderImpl.class );

    @Override
    public LocalAccount getAssociatedLightAccount( @Nonnull Account account )
    {
        checkNotNull( account, "{0} cannot be null", Account.class.getSimpleName() );

        LocalAccount localAccount;
        localAccount = ofy().load().type( LocalAccount.class ).id( account.getId() ).now();

        if ( localAccount == null )
        {
            localAccount = new LocalAccount( account );
            localAccount.save();
            logger.info( "Local account just has been created: " + localAccount );
        }

        if ( !account.getId().equals( localAccount.getId() ) )
        {
            logger.error( "IdentityId mismatch. Current " + LocalAccount.class.getSimpleName() + " '"
                    + localAccount.getId() + "' does not match to the authenticated account: " + account );
            throw new IllegalArgumentException( "Identity mismatch." );
        }

        return localAccount;
    }
}
