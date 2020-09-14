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

package biz.turnonline.ecosystem.payment.service.model;

import biz.turnonline.ecosystem.payment.service.LocalAccountProvider;
import biz.turnonline.ecosystem.steward.facade.Domicile;
import biz.turnonline.ecosystem.steward.model.Account;
import biz.turnonline.ecosystem.steward.model.DeputyAccount;
import com.google.api.client.util.DateTime;
import com.google.api.services.pubsub.model.PubsubMessage;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.apache.commons.lang3.LocaleUtils;
import org.ctoolkit.restapi.client.pubsub.PubsubCommand;
import org.ctoolkit.restapi.client.pubsub.PubsubMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_EMAIL;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_IDENTITY_ID;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_UNIQUE_ID;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.DATA_TYPE;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ENCODED_UNIQUE_KEY;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.fromString;

/**
 * The 'account.changes' subscription listener implementation.
 * Updates following property values from {@link Account} if any of those values
 * has changed comparing to {@link LocalAccount}.
 * <ul>
 *     <li>{@link LocalAccount#setEmail(String)}</li>
 *     <li>{@link LocalAccount#setZoneId(String)}</li>
 *     <li>{@link LocalAccount#setLocale(String)}</li>
 *     <li>{@link LocalAccount#setDomicile(String)}</li>
 * </ul>
 * Updates following property values from {@link DeputyAccount} if any of those values
 * has changed comparing to {@link LocalDeputyAccount}.
 * If matching entry not found a new {@link LocalDeputyAccount} will be created.
 * <ul>
 *     <li>{@link LocalDeputyAccount#setRole(String)}</li>
 *     <li>{@link LocalDeputyAccount#setLocale(String)}</li>
 * </ul>
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
public class AccountStewardChangesSubscription
        implements PubsubMessageListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger( AccountStewardChangesSubscription.class );

    private static final long serialVersionUID = -3406437765037822097L;

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
                ACCOUNT_IDENTITY_ID
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
        boolean delete = command.isDelete();
        List<String> uniqueKey = command.getUniqueKey();
        String data = message.getData();

        LOGGER.info( "[" + subscription + "] " + dataType + " has been received at publish time "
                + message.getPublishTime()
                + " with length: "
                + data.length() + " and unique key: '" + uniqueKey + "'" + ( delete ? " to be deleted" : "" ) );

        Account account = command.fromData( Account.class );
        LocalAccount associatedAccount = lap.check( command );
        if ( associatedAccount == null )
        {
            return;
        }

        DateTime publishDateTime = command.getPublishDateTime();
        DateTime last = delete && publishDateTime != null
                ? publishDateTime : account.getModificationDate();

        if ( Account.class.getSimpleName().equals( dataType ) )
        {
            if ( !Objects.equals( associatedAccount.getId(), account.getId() ) )
            {
                LOGGER.info( "Uninterested account identified by ID '" + account.getId() + "'" );
                LOGGER.info( "Associated account ID '" + associatedAccount.getId() + "'" );
                return;
            }

            Timestamp timestamp = Timestamp.of( dataType, uniqueKey, associatedAccount, last );
            if ( timestamp.isObsolete() )
            {
                LOGGER.info( "Incoming account changes are obsolete, nothing to do " + timestamp.getName() );
                return;
            }

            process( associatedAccount, account, timestamp );
        }
        else if ( DeputyAccount.class.getSimpleName().equals( dataType ) )
        {
            DeputyAccount sub = fromString( data, DeputyAccount.class );
            if ( Strings.isNullOrEmpty( sub.getEmail() ) )
            {
                LOGGER.warn( "Deputy account is missing email, nothing to do " + sub );
                return;
            }

            Timestamp timestamp = Timestamp.of( dataType, uniqueKey, associatedAccount, publishDateTime );
            if ( timestamp.isObsolete() )
            {
                LOGGER.info( "Incoming deputy account changes are obsolete, nothing to do " + timestamp.getName() );
                return;
            }

            LocalDeputyAccount deputy = lap.get( sub.getEmail() );
            if ( deputy == null )
            {
                deputy = new LocalDeputyAccount( sub.getEmail() );
                deputy.setOwner( associatedAccount );
            }

            process( deputy, sub, timestamp );
        }
    }

    private void process( @Nonnull LocalAccount la,
                          @Nonnull Account account,
                          @Nonnull Timestamp timestamp )
    {
        boolean updateAccount = false;

        // Current, the most up to date Zone ID, taken from the remote account
        ZoneId remoteZoneId = Strings.isNullOrEmpty( account.getZoneId() ) ? null : ZoneId.of( account.getZoneId() );
        ZoneId zoneId = la.getZoneId();

        if ( remoteZoneId != null && !remoteZoneId.equals( zoneId ) )
        {
            LOGGER.info( "Zone ID has changed from '" + la.getZoneId() + "' to '" + remoteZoneId + "'" );
            la.setZoneId( remoteZoneId.getId() );
            updateAccount = true;
        }

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

        // Current, the most up to date domicile, taken from the remote account
        String remoteDomicile = account.getBusiness() == null ? null : account.getBusiness().getDomicile();
        Domicile domicile = null;

        if ( !Strings.isNullOrEmpty( remoteDomicile ) )
        {
            try
            {
                domicile = Domicile.valueOf( remoteDomicile );
            }
            catch ( IllegalArgumentException e )
            {
                LOGGER.warn( "Unsupported incoming domicile '"
                        + remoteDomicile
                        + "' of the business account, ignored." );
            }
        }

        if ( domicile != null && ( la.isDomicileNull() || !domicile.equals( la.getDomicile() ) ) )
        {
            LOGGER.info( "Account domicile has changed from '"
                    + la.getDomicile()
                    + "' to '"
                    + domicile
                    + "'" );

            // set only supported domicile
            la.setDomicile( domicile.name() );
            updateAccount = true;
        }

        if ( updateAccount )
        {
            updateAccount( la, timestamp );
        }
    }

    private void process( @Nonnull LocalDeputyAccount deputy,
                          @Nonnull DeputyAccount remote,
                          @Nonnull Timestamp timestamp )
    {
        boolean updateAccount = false;

        Locale remoteLocale = remote.getLocale() == null ? null : LocaleUtils.toLocale( remote.getLocale() );
        if ( remoteLocale != null && !remoteLocale.equals( deputy.getLocale() ) )
        {
            LOGGER.info( "Deputy account locale has changed from '"
                    + deputy.getLocale()
                    + "' to '"
                    + remoteLocale
                    + "'" );

            deputy.setLocale( remote.getLocale() );
            updateAccount = true;
        }

        String remoteRole = remote.getRole();
        if ( remoteRole != null && !remoteRole.equals( deputy.getRole() ) )
        {
            LOGGER.info( "Deputy role has changed from '"
                    + deputy.getRole()
                    + "' to '"
                    + remoteRole
                    + "'" );

            deputy.setRole( remoteRole );
            updateAccount = true;
        }

        if ( updateAccount )
        {
            updateDeputy( deputy, timestamp );
        }
    }

    @VisibleForTesting
    void updateDeputy( LocalDeputyAccount deputy, Timestamp timestamp )
    {
        ofy().transact( () -> {
            deputy.save();
            timestamp.done();
        } );
    }

    @VisibleForTesting
    void updateAccount( LocalAccount la, Timestamp timestamp )
    {
        ofy().transact( () -> {
            la.save();
            timestamp.done();
        } );
    }
}
