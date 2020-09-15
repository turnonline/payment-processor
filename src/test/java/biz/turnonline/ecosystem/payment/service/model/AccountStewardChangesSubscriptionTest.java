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
import com.google.api.services.pubsub.model.PubsubMessage;
import com.google.common.io.ByteStreams;
import com.googlecode.objectify.Ref;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.ctoolkit.restapi.client.ClientErrorException;
import org.ctoolkit.restapi.client.NotFoundException;
import org.ctoolkit.restapi.client.pubsub.PubsubCommand;
import org.ctoolkit.restapi.client.pubsub.TopicMessage;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static biz.turnonline.ecosystem.payment.service.model.LocalAccount.DEFAULT_ZONE;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_EMAIL;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_IDENTITY_ID;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_UNIQUE_ID;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.DATA_TYPE;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ENCODED_UNIQUE_KEY;

/**
 * {@link AccountStewardChangesSubscription} unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@SuppressWarnings( "ConstantConditions" )
public class AccountStewardChangesSubscriptionTest
{
    private static final String EMAIL = "my.account@turnonline.biz";

    private static final String DEPUTY_EMAIL = "deputy.account@turnonline.biz";

    private static final String EMAIL_CHANGED = "another.account@turnonline.biz";

    private static final String IDENTITY_ID = "34ghW4jL9";

    private static final Long ACCOUNT_ID = 1233219L;

    @Tested
    private AccountStewardChangesSubscription tested;

    @Injectable
    private LocalAccountProvider lap;

    @Mocked
    private Timestamp timestamp;

    @Test
    public void validPubsubMessage_Account_NoChange() throws Exception
    {
        LocalAccount localAccount = initLocalAccount();

        localAccount.setLocale( "en" );
        localAccount.setDomicile( "SK" );
        localAccount.setZoneId( DEFAULT_ZONE );

        new Expectations( tested )
        {
            {
                lap.check( ( PubsubCommand ) any );
                result = localAccount;

                tested.updateAccount( localAccount, timestamp );
                times = 0;

                tested.updateDeputy( ( LocalDeputyAccount ) any, ( Timestamp ) any );
                times = 0;
            }
        };

        PubsubMessage message = validAccountPubsubMessage();
        tested.onMessage( message, "account.changes" );

        assertThat( localAccount.getId() ).isEqualTo( ACCOUNT_ID );
        assertThat( localAccount.getEmail() ).isEqualTo( EMAIL );
        assertThat( localAccount.getIdentityId() ).isEqualTo( IDENTITY_ID );
        assertThat( localAccount.getLocale() ).isEqualTo( Locale.ENGLISH );
        assertThat( localAccount.getDomicile().name() ).isEqualTo( "SK" );
    }

    @Test
    public void validPubsubMessage_Account_UninterestedAccount() throws Exception
    {
        // mock account ID to be different as coming via Pub/Sub
        LocalAccount localAccount = initLocalAccount();

        localAccount.setLocale( "en" );
        localAccount.setDomicile( "SK" );
        localAccount.setZoneId( DEFAULT_ZONE );

        new Expectations( tested )
        {
            {
                lap.check( ( PubsubCommand ) any );
                result = localAccount;

                tested.updateAccount( ( LocalAccount ) any, ( Timestamp ) any );
                times = 0;

                tested.updateDeputy( ( LocalDeputyAccount ) any, ( Timestamp ) any );
                times = 0;
            }
        };

        PubsubMessage message = uninterestedAccountChangedPubsubMessage();
        tested.onMessage( message, "account.changes" );

        assertThat( localAccount.getId() ).isEqualTo( ACCOUNT_ID );
        assertThat( localAccount.getEmail() ).isEqualTo( EMAIL );
        assertThat( localAccount.getIdentityId() ).isEqualTo( IDENTITY_ID );
        assertThat( localAccount.getLocale() ).isEqualTo( Locale.ENGLISH );
        assertThat( localAccount.getDomicile().name() ).isEqualTo( "SK" );
    }

    @Test
    public void validPubsubMessage_Account_EmailChanged() throws Exception
    {
        LocalAccount localAccount = initLocalAccount();
        localAccount.setZoneId( DEFAULT_ZONE );

        new Expectations( localAccount, tested )
        {
            {
                lap.check( ( PubsubCommand ) any );
                result = localAccount;

                tested.updateAccount( localAccount, timestamp );
                times = 1;

                tested.updateDeputy( ( LocalDeputyAccount ) any, ( Timestamp ) any );
                times = 0;

                timestamp.isObsolete();
                result = false;
            }
        };

        PubsubMessage message = emailChangedPubsubMessage();
        tested.onMessage( message, "account.changes" );

        assertThat( localAccount.getEmail() ).isEqualTo( EMAIL_CHANGED );
        assertThat( localAccount.getLocale() ).isEqualTo( Locale.ENGLISH );
    }

    @Test
    public void validPubsubMessage_Account_LocaleChanged() throws Exception
    {
        LocalAccount localAccount = initLocalAccount();
        localAccount.setLocale( "de" );
        localAccount.setZoneId( DEFAULT_ZONE );

        new Expectations( localAccount, tested )
        {
            {
                lap.check( ( PubsubCommand ) any );
                result = localAccount;

                tested.updateAccount( localAccount, timestamp );
                times = 1;

                tested.updateDeputy( ( LocalDeputyAccount ) any, ( Timestamp ) any );
                times = 0;

                timestamp.isObsolete();
                result = false;
            }
        };

        PubsubMessage message = validAccountPubsubMessage();
        tested.onMessage( message, "account.changes" );

        assertThat( localAccount.getEmail() ).isEqualTo( EMAIL );
        assertThat( localAccount.getLocale() ).isEqualTo( Locale.ENGLISH );
    }

    @Test
    public void validPubsubMessage_Account_ZoneIdChanged() throws Exception
    {
        LocalAccount localAccount = initLocalAccount();
        localAccount.setZoneId( DEFAULT_ZONE );

        new Expectations( localAccount, tested )
        {
            {
                lap.check( ( PubsubCommand ) any );
                result = localAccount;

                tested.updateAccount( localAccount, timestamp );
                times = 1;

                tested.updateDeputy( ( LocalDeputyAccount ) any, ( Timestamp ) any );
                times = 0;

                timestamp.isObsolete();
                result = false;
            }
        };

        PubsubMessage message = zoneChangedPubsubMessage();
        tested.onMessage( message, "account.changes" );

        assertThat( localAccount.getEmail() ).isEqualTo( EMAIL );
        assertThat( localAccount.getLocale() ).isEqualTo( Locale.ENGLISH );
        assertThat( localAccount.getZoneId().getId() ).isEqualTo( "America/Chicago" );
    }

    @Test
    public void validPubsubMessage_Account_DomicileChanged() throws Exception
    {
        LocalAccount localAccount = initLocalAccount();
        localAccount.setZoneId( "Europe/Paris" );
        localAccount.setLocale( "en" );
        localAccount.setDomicile( "CZ" );

        new Expectations( localAccount, tested )
        {
            {
                lap.check( ( PubsubCommand ) any );
                result = localAccount;

                tested.updateAccount( localAccount, timestamp );
                times = 1;

                tested.updateDeputy( ( LocalDeputyAccount ) any, ( Timestamp ) any );
                times = 0;

                timestamp.isObsolete();
                result = false;
            }
        };

        PubsubMessage message = validAccountPubsubMessage();
        tested.onMessage( message, "account.changes" );

        assertThat( localAccount.getEmail() ).isEqualTo( EMAIL );
        assertThat( localAccount.getZoneId() ).isEqualTo( ZoneId.of( "Europe/Paris" ) );
        assertThat( localAccount.getLocale() ).isEqualTo( Locale.ENGLISH );
        assertThat( localAccount.getDomicile() ).isEqualTo( Domicile.SK );
    }

    @Test
    public void validPubsubMessage_Account_UnsupportedDomicile() throws Exception
    {
        LocalAccount localAccount = initLocalAccount();
        localAccount.setZoneId( "Europe/Paris" );
        localAccount.setLocale( "en" );
        localAccount.setDomicile( "CZ" );

        new Expectations( localAccount, tested )
        {
            {
                lap.check( ( PubsubCommand ) any );
                result = localAccount;

                tested.updateAccount( localAccount, timestamp );
                times = 0;

                tested.updateDeputy( ( LocalDeputyAccount ) any, ( Timestamp ) any );
                times = 0;

                timestamp.isObsolete();
                result = false;
            }
        };

        PubsubMessage message = invalidDomicilePubsubMessage();
        tested.onMessage( message, "account.changes" );

        assertThat( localAccount.getDomicile() ).isEqualTo( Domicile.CZ );
    }

    @Test
    public void validPubsubMessage_Account_NoneAssociated() throws Exception
    {
        new Expectations( tested )
        {
            {
                lap.check( ( PubsubCommand ) any );
                result = null;

                tested.updateDeputy( ( LocalDeputyAccount ) any, ( Timestamp ) any );
                times = 0;

                tested.updateAccount( ( LocalAccount ) any, ( Timestamp ) any );
                times = 0;
            }
        };

        PubsubMessage message = validAccountPubsubMessage();
        tested.onMessage( message, "account.changes" );
    }

    @Test
    public void validPubsubMessage_Account_ObsoleteChange() throws Exception
    {
        LocalAccount localAccount = initLocalAccount();
        localAccount.setZoneId( "Europe/Paris" );

        new Expectations( tested )
        {
            {
                lap.check( ( PubsubCommand ) any );
                result = localAccount;

                tested.updateDeputy( ( LocalDeputyAccount ) any, ( Timestamp ) any );
                times = 0;

                tested.updateAccount( ( LocalAccount ) any, ( Timestamp ) any );
                times = 0;

                timestamp.isObsolete();
                result = true;
            }
        };

        PubsubMessage message = validAccountPubsubMessage();
        tested.onMessage( message, "account.changes" );
    }

    @Test
    public void validPubsubMessage_Account_NotFound() throws Exception
    {
        new Expectations( tested )
        {
            {
                lap.check( ( PubsubCommand ) any );
                result = new NotFoundException( "Account not found" );

                tested.updateAccount( ( LocalAccount ) any, ( Timestamp ) any );
                times = 0;

                tested.updateDeputy( ( LocalDeputyAccount ) any, ( Timestamp ) any );
                times = 0;
            }
        };

        PubsubMessage message = validAccountPubsubMessage();
        tested.onMessage( message, "account.changes" );
    }

    @Test
    public void validPubsubMessage_Account_ClientError() throws Exception
    {
        new Expectations( tested )
        {
            {
                lap.check( ( PubsubCommand ) any );
                result = new ClientErrorException( "Client error" );

                tested.updateAccount( ( LocalAccount ) any, ( Timestamp ) any );
                times = 0;

                tested.updateDeputy( ( LocalDeputyAccount ) any, ( Timestamp ) any );
                times = 0;
            }
        };

        PubsubMessage message = validAccountPubsubMessage();
        tested.onMessage( message, "account.changes" );
    }

    @Test
    public void validPubsubMessage_DeputyAccount_NoChange() throws Exception
    {
        LocalAccount localAccount = initLocalAccount();
        localAccount.setZoneId( "Europe/Paris" );

        LocalDeputyAccount deputy = new LocalDeputyAccount( DEPUTY_EMAIL );
        deputy.setLocale( "en" );
        deputy.setRole( "DEPUTY_SELLER" );

        new Expectations( tested )
        {
            {
                lap.check( ( PubsubCommand ) any );
                result = localAccount;

                lap.get( DEPUTY_EMAIL );
                result = deputy;

                tested.updateDeputy( ( LocalDeputyAccount ) any, ( Timestamp ) any );
                times = 0;

                tested.updateAccount( ( LocalAccount ) any, ( Timestamp ) any );
                times = 0;

                timestamp.isObsolete();
                result = false;
            }
        };

        PubsubMessage message = deputyAccountPubsubMessage();
        tested.onMessage( message, "account.changes" );

        assertWithMessage( "Deputy account email" )
                .that( deputy.getEmail() )
                .isEqualTo( DEPUTY_EMAIL );

        assertWithMessage( "Deputy account role" )
                .that( deputy.getRole() )
                .isEqualTo( "DEPUTY_SELLER" );

        assertWithMessage( "Deputy account locale" )
                .that( deputy.getLocale() )
                .isEqualTo( Locale.ENGLISH );
    }

    @Test
    public void validPubsubMessage_DeputyAccount_RoleChanged() throws Exception
    {
        LocalAccount localAccount = initLocalAccount();
        localAccount.setZoneId( "Europe/Paris" );

        LocalDeputyAccount deputy = new LocalDeputyAccount( DEPUTY_EMAIL );
        deputy.setLocale( "en" );
        deputy.setRole( "DEPUTY_STANDARD" );

        new Expectations( tested )
        {
            {
                lap.check( ( PubsubCommand ) any );
                result = localAccount;

                lap.get( DEPUTY_EMAIL );
                result = deputy;

                tested.updateDeputy( deputy, timestamp );
                times = 1;

                tested.updateAccount( ( LocalAccount ) any, ( Timestamp ) any );
                times = 0;

                timestamp.isObsolete();
                result = false;
            }
        };

        PubsubMessage message = deputyAccountPubsubMessage();
        tested.onMessage( message, "account.changes" );

        assertWithMessage( "Deputy account email" )
                .that( deputy.getEmail() )
                .isEqualTo( DEPUTY_EMAIL );

        assertWithMessage( "Deputy account role" )
                .that( deputy.getRole() )
                .isEqualTo( "DEPUTY_SELLER" );

        assertWithMessage( "Deputy account locale" )
                .that( deputy.getLocale() )
                .isEqualTo( Locale.ENGLISH );
    }

    @Test
    public void validPubsubMessage_DeputyAccount_LocaleChanged() throws Exception
    {
        LocalAccount localAccount = initLocalAccount();
        localAccount.setZoneId( "Europe/Paris" );

        LocalDeputyAccount deputy = new LocalDeputyAccount( DEPUTY_EMAIL );
        deputy.setLocale( "sk_SK" );
        deputy.setRole( "DEPUTY_SELLER" );

        new Expectations( tested )
        {
            {
                lap.check( ( PubsubCommand ) any );
                result = localAccount;

                lap.get( DEPUTY_EMAIL );
                result = deputy;

                tested.updateDeputy( deputy, timestamp );
                times = 1;

                tested.updateAccount( ( LocalAccount ) any, ( Timestamp ) any );
                times = 0;

                timestamp.isObsolete();
                result = false;
            }
        };

        PubsubMessage message = deputyAccountPubsubMessage();
        tested.onMessage( message, "account.changes" );

        assertWithMessage( "Deputy account email" )
                .that( deputy.getEmail() )
                .isEqualTo( DEPUTY_EMAIL );

        assertWithMessage( "Deputy account role" )
                .that( deputy.getRole() )
                .isEqualTo( "DEPUTY_SELLER" );

        assertWithMessage( "Deputy account locale" )
                .that( deputy.getLocale() )
                .isEqualTo( Locale.ENGLISH );
    }

    @Test
    public void validPubsubMessage_DeputyAccount_ObsoleteChange() throws Exception
    {
        LocalAccount localAccount = initLocalAccount();
        localAccount.setZoneId( "Europe/Paris" );

        new Expectations( tested )
        {
            {
                lap.check( ( PubsubCommand ) any );
                result = localAccount;

                tested.updateDeputy( ( LocalDeputyAccount ) any, ( Timestamp ) any );
                times = 0;

                tested.updateAccount( ( LocalAccount ) any, ( Timestamp ) any );
                times = 0;

                timestamp.isObsolete();
                result = true;
            }
        };

        PubsubMessage message = deputyAccountPubsubMessage();
        tested.onMessage( message, "account.changes" );
    }

    @Test
    public void validPubsubMessage_DeputyAccount_MissingEmail() throws Exception
    {
        LocalAccount localAccount = initLocalAccount();
        localAccount.setZoneId( "Europe/Paris" );

        new Expectations( tested )
        {
            {
                lap.check( ( PubsubCommand ) any );
                result = localAccount;

                tested.updateDeputy( ( LocalDeputyAccount ) any, ( Timestamp ) any );
                times = 0;

                tested.updateAccount( ( LocalAccount ) any, ( Timestamp ) any );
                times = 0;
            }
        };

        PubsubMessage message = deputyAccountEmptyEmailPubsubMessage();
        tested.onMessage( message, "account.changes" );
    }

    @Test
    public void validPubsubMessage_DeputyAccount_NotFound() throws Exception
    {
        LocalAccount localAccount = initLocalAccount();
        localAccount.setZoneId( "Europe/Paris" );

        AtomicReference<LocalAccount> setOwner = new AtomicReference<>();

        new MockUp<Ref<?>>()
        {
            @Mock
            public Ref<?> create( Object value )
            {
                setOwner.set( ( LocalAccount ) value );
                return null;
            }
        };

        new Expectations( tested )
        {
            {
                lap.check( ( PubsubCommand ) any );
                result = localAccount;

                lap.get( DEPUTY_EMAIL );
                result = null;

                tested.updateDeputy( ( LocalDeputyAccount ) any, ( Timestamp ) any );
                times = 1;

                tested.updateAccount( ( LocalAccount ) any, ( Timestamp ) any );
                times = 0;
            }
        };

        PubsubMessage message = deputyAccountPubsubMessage();
        tested.onMessage( message, "account.changes" );

        assertWithMessage( "Deputy account parent matches" )
                .that( Objects.equals( localAccount, setOwner.get() ) )
                .isTrue();
    }

    @Test
    public void validPubsubMessage_UninterestedDataType() throws Exception
    {
        PubsubMessage message = uninterestedPubsubMessage();
        tested.onMessage( message, "account.changes" );
    }

    @Test
    public void onMessage_InvalidPubsubMessage() throws Exception
    {
        PubsubMessage message = invalidPubsubMessage();
        tested.onMessage( message, "account.changes" );

        new Verifications()
        {
            {
                lap.check( ( PubsubCommand ) any );
                times = 0;
            }
        };
    }

    private PubsubMessage uninterestedPubsubMessage() throws IOException
    {
        TopicMessage.Builder builder = incompletePubsubMessageBuilder( "Uninterested" );
        builder.addAttribute( ACCOUNT_EMAIL, EMAIL );
        return builder.build().getMessages().get( 0 );
    }

    private PubsubMessage emailChangedPubsubMessage() throws IOException
    {
        TopicMessage.Builder builder = incompletePubsubMessageBuilder( Account.class.getSimpleName(),
                "account-email-changed.json" );

        builder.addAttribute( ACCOUNT_EMAIL, EMAIL_CHANGED );
        return builder.build().getMessages().get( 0 );
    }

    private PubsubMessage uninterestedAccountChangedPubsubMessage() throws IOException
    {
        TopicMessage.Builder builder = incompletePubsubMessageBuilder( Account.class.getSimpleName(),
                "account-uninterested.json" );

        builder.addAttribute( ACCOUNT_EMAIL, EMAIL );
        return builder.build().getMessages().get( 0 );
    }

    private PubsubMessage zoneChangedPubsubMessage() throws IOException
    {
        TopicMessage.Builder builder = incompletePubsubMessageBuilder( Account.class.getSimpleName(),
                "account-zone-changed.json" );

        builder.addAttribute( ACCOUNT_EMAIL, EMAIL );
        return builder.build().getMessages().get( 0 );
    }

    private PubsubMessage deputyAccountPubsubMessage() throws IOException
    {
        TopicMessage.Builder builder = incompletePubsubMessageBuilder( DeputyAccount.class.getSimpleName(),
                "deputy-account.json" );

        builder.addAttribute( ACCOUNT_EMAIL, EMAIL );
        return builder.build().getMessages().get( 0 );
    }

    private PubsubMessage deputyAccountEmptyEmailPubsubMessage() throws IOException
    {
        TopicMessage.Builder builder = incompletePubsubMessageBuilder( DeputyAccount.class.getSimpleName(),
                "deputy-account-empty-email.json" );

        builder.addAttribute( ACCOUNT_EMAIL, EMAIL );
        return builder.build().getMessages().get( 0 );
    }

    private PubsubMessage invalidPubsubMessage() throws IOException
    {
        return incompletePubsubMessageBuilder( Account.class.getSimpleName() ).build().getMessages().get( 0 );
    }

    private PubsubMessage invalidDomicilePubsubMessage() throws IOException
    {
        TopicMessage.Builder builder = incompletePubsubMessageBuilder( Account.class.getSimpleName(),
                "account-unsupported-domicile.json" );

        builder.addAttribute( ACCOUNT_EMAIL, EMAIL );
        return builder.build().getMessages().get( 0 );
    }

    private PubsubMessage validAccountPubsubMessage() throws IOException
    {
        TopicMessage.Builder builder = incompletePubsubMessageBuilder( Account.class.getSimpleName() );
        builder.addAttribute( ACCOUNT_EMAIL, EMAIL );
        return builder.build().getMessages().get( 0 );
    }

    /**
     * {@link PubsubCommand#ACCOUNT_EMAIL} is missing to be valid {@link Account} Pub/Sub message.
     */
    private TopicMessage.Builder incompletePubsubMessageBuilder( String dataType ) throws IOException
    {
        return incompletePubsubMessageBuilder( dataType, "account.json" );
    }

    /**
     * {@link PubsubCommand#ACCOUNT_EMAIL} is missing to be valid {@link Account} Pub/Sub message.
     */
    private TopicMessage.Builder incompletePubsubMessageBuilder( String dataType, String json ) throws IOException
    {
        InputStream stream = getClass().getResourceAsStream( json );
        byte[] bytes = ByteStreams.toByteArray( stream );

        TopicMessage.Builder builder = TopicMessage.newBuilder();
        String id = String.valueOf( ACCOUNT_ID );
        builder.setProjectId( "projectId-135" ).setTopicId( "a-topic" )
                .addMessage( bytes, ACCOUNT_UNIQUE_ID, id )
                .addAttribute( DATA_TYPE, dataType )
                .addAttribute( ACCOUNT_IDENTITY_ID, IDENTITY_ID )
                .addAttribute( ENCODED_UNIQUE_KEY, id );

        return builder;
    }

    private LocalAccount initLocalAccount()
    {
        return new LocalAccount( new Account()
                .setId( ACCOUNT_ID )
                .setEmail( EMAIL )
                .setIdentityId( IDENTITY_ID ) );
    }
}