package biz.turnonline.ecosystem.payment.service.model;

import biz.turnonline.ecosystem.payment.service.LocalAccountProvider;
import biz.turnonline.ecosystem.steward.model.Account;
import com.google.api.client.util.DateTime;
import com.google.api.services.pubsub.model.PubsubMessage;
import org.apache.commons.lang3.LocaleUtils;
import org.ctoolkit.restapi.client.pubsub.PubsubCommand;
import org.ctoolkit.restapi.client.pubsub.PubsubMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_AUDIENCE;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_EMAIL;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_IDENTITY_ID;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_UNIQUE_ID;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.DATA_TYPE;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ENCODED_UNIQUE_KEY;

/**
 * The 'account.changes' subscription listener implementation.
 * Updates following property values from {@link Account} if any of those values
 * has changed comparing to {@link LocalAccount}.
 * <ul>
 *     <li>{@link LocalAccount#setEmail(String)}</li>
 *     <li>{@link LocalAccount#setLocale(String)}</li>
 * </ul>
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
public class AccountStewardChangesSubscription
        implements PubsubMessageListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger( AccountStewardChangesSubscription.class );

    private static final long serialVersionUID = -8409940111786126948L;

    private final LocalAccountProvider lap;

    @Inject
    AccountStewardChangesSubscription( LocalAccountProvider lap )
    {
        this.lap = lap;
    }

    @Override
    public void onMessage( @Nonnull PubsubMessage message, @Nonnull String subscription ) throws Exception
    {
        PubsubCommand command = new PubsubCommand( message );

        String[] mandatory = {
                DATA_TYPE,
                ENCODED_UNIQUE_KEY,
                ACCOUNT_UNIQUE_ID,
                ACCOUNT_EMAIL,
                ACCOUNT_IDENTITY_ID,
                ACCOUNT_AUDIENCE
        };

        if ( !command.validate( mandatory ) )
        {
            LOGGER.error( "Some of the mandatory attributes "
                    + Arrays.toString( mandatory )
                    + " are missing, incoming attributes: "
                    + message.getAttributes() );
            return;
        }

        String dataType = command.getDataType();
        if ( !Account.class.getSimpleName().equals( dataType ) )
        {
            LOGGER.info( "Uninterested data type '" + dataType + "'" );
            return;
        }

        boolean delete = command.isDelete();
        List<String> uniqueKey = command.getUniqueKey();
        String data = message.getData();

        LOGGER.info( "[" + subscription + "] " + dataType + " has been received at publish time "
                + message.getPublishTime()
                + " with length: "
                + data.length() + " and unique key: '" + uniqueKey + "'" + ( delete ? " to be deleted" : "" ) );

        Account account = fromString( data, Account.class );
        LocalAccount localAccount = lap.initGet( new LocalAccountProvider.Builder()
                .accountId( command.getAccountId() )
                .email( command.getAccountEmail() )
                .identityId( command.getAccountIdentityId() )
                .audience( command.getAccountAudience() ) );

        DateTime publishDateTime = command.getPublishDateTime();
        DateTime last = delete && publishDateTime != null
                ? publishDateTime : account.getModificationDate();

        Timestamp timestamp = Timestamp.of( dataType, uniqueKey, localAccount, last );
        if ( timestamp.isObsolete() )
        {
            LOGGER.info( "Incoming account changes are obsolete, nothing to do " + timestamp.getName() );
            return;
        }

        if ( Account.class.getSimpleName().equals( dataType ) )
        {
            process( localAccount, account, timestamp );
        }
    }

    private void process( @Nonnull LocalAccount la,
                          @Nonnull Account account,
                          @Nonnull Timestamp timestamp )
    {
        boolean updateAccount = false;

        // Current, the most up to date login email, taken from the remote account
        String remoteLoginEmail = account.getEmail();
        if ( !remoteLoginEmail.equalsIgnoreCase( la.getEmail() ) )
        {
            LOGGER.info( "Login Email has changed from '" + la.getEmail() + "' to '" + remoteLoginEmail + "'" );
            la.setEmail( remoteLoginEmail );
            updateAccount = true;
        }

        // Current, the most up to date locale, taken from the remote account
        Locale remoteLocale = account.getLocale() == null ? null : LocaleUtils.toLocale( account.getLocale() );
        if ( remoteLocale != null && !remoteLocale.equals( la.getLocale() ) )
        {
            LOGGER.info( "Account locale has changed from '" + la.getLocale() + "' to '" + remoteLocale + "'" );
            la.setLocale( account.getLocale() );
            updateAccount = true;
        }

        if ( updateAccount )
        {
            updateAccount( la, timestamp );
        }
    }

    void updateAccount( LocalAccount la, Timestamp timestamp )
    {
        ofy().transact( () -> {
            la.save();
            timestamp.done();
        } );
    }
}
