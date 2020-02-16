package biz.turnonline.ecosystem.payment.oauth;

import biz.turnonline.ecosystem.revolut.business.account.model.Account;
import com.googlecode.objectify.Key;
import org.ctoolkit.restapi.client.ClientErrorException;
import org.ctoolkit.restapi.client.ForbiddenException;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.restapi.client.UnauthorizedException;
import org.ctoolkit.services.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Setting up access to business account.
 * Authorisation Code exchange will enable access to Revolut for Business API.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class RevolutExchangeAuthorisationCode
        extends Task<RevolutCertMetadata>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( RevolutExchangeAuthorisationCode.class );

    private static final long serialVersionUID = 7682871294183932878L;

    @Inject
    transient private RestFacade facade;

    public RevolutExchangeAuthorisationCode( @Nonnull Key<RevolutCertMetadata> key )
    {
        super( "Exchange-Authorisation-Code" );
        setEntityKey( checkNotNull( key, "Entity key can't be null" ) );
    }

    @Override
    protected void execute()
    {
        RevolutCertMetadata details = null;

        try
        {
            // Just call a GET operation to invoke a Revolut API, it causes authorisation code exchange.
            // Everything managed by RevolutCredential.
            facade.list( Account.class ).finish();

            details = workWith( null );
            LOGGER.warn( "Authorisation code has been exchanged to authorize access to Revolut for Business API: "
                    + details );

        }
        catch ( UnauthorizedException | ClientErrorException | ForbiddenException e )
        {
            LOGGER.warn( "Authorisation code seems to be invalid: " + details, e );
        }
    }
}
