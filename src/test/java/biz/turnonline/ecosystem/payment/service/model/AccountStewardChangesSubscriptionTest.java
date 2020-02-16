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
import biz.turnonline.ecosystem.steward.model.Account;
import com.google.api.services.pubsub.model.PubsubMessage;
import com.google.common.io.ByteStreams;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.ctoolkit.restapi.client.pubsub.PubsubCommand;
import org.ctoolkit.restapi.client.pubsub.TopicMessage;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import static biz.turnonline.ecosystem.payment.service.model.LocalAccount.DEFAULT_ZONE;
import static com.google.common.truth.Truth.assertThat;
import static org.ctoolkit.restapi.client.pubsub.PubsubCommand.ACCOUNT_AUDIENCE;
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
public class AccountStewardChangesSubscriptionTest
{
    private static final String EMAIL = "my.account@turnonline.biz";

    private static final String EMAIL_CHANGED = "another.account@turnonline.biz";

    private static final String IDENTITY_ID = "34ghW4jL9";

    private static final String AUDIENCE = "turn-online";

    private static final Long ACCOUNT_ID = 1233219L;

    @Tested
    private AccountStewardChangesSubscription tested;

    @Injectable
    private LocalAccountProvider lap;

    @Mocked
    private Timestamp timestamp;

    @Test
    public void onMessage_ValidPubsubMessage_NoChange() throws Exception
    {
        LocalAccount localAccount = new LocalAccount( new LocalAccountProvider.Builder()
                .accountId( ACCOUNT_ID )
                .email( EMAIL )
                .identityId( IDENTITY_ID )
                .audience( AUDIENCE ) );

        localAccount.setLocale( "en" );
        localAccount.setDomicile( "SK" );
        localAccount.setZoneId( DEFAULT_ZONE );

        new Expectations( tested )
        {
            {
                //noinspection ConstantConditions
                lap.initGet( ( LocalAccountProvider.Builder ) any );
                result = localAccount;

                tested.updateAccount( localAccount, timestamp );
                times = 0;
            }
        };

        PubsubMessage message = validPubsubMessage();
        tested.onMessage( message, "account.changes" );

        assertThat( localAccount.getId() ).isEqualTo( ACCOUNT_ID );
        assertThat( localAccount.getEmail() ).isEqualTo( EMAIL );
        assertThat( localAccount.getIdentityId() ).isEqualTo( IDENTITY_ID );
        assertThat( localAccount.getLocale() ).isEqualTo( Locale.ENGLISH );
        assertThat( localAccount.getDomicile().name() ).isEqualTo( "SK" );

        new Verifications()
        {
            {
                LocalAccountProvider.Builder builder;
                lap.initGet( builder = withCapture() );

                assertThat( builder ).isNotNull();
                assertThat( builder.getAccountId() ).isEqualTo( ACCOUNT_ID );
                assertThat( builder.getEmail() ).isEqualTo( EMAIL );
                assertThat( builder.getIdentityId() ).isEqualTo( IDENTITY_ID );
            }
        };
    }

    @Test
    public void onMessage_ValidPubsubMessage_EmailChanged() throws Exception
    {
        LocalAccount localAccount = new LocalAccount( new LocalAccountProvider.Builder()
                .accountId( ACCOUNT_ID )
                .email( EMAIL )
                .identityId( IDENTITY_ID )
                .audience( AUDIENCE ) );
        localAccount.setZoneId( DEFAULT_ZONE );

        new Expectations( localAccount, tested )
        {
            {
                //noinspection ConstantConditions
                lap.initGet( ( LocalAccountProvider.Builder ) any );
                result = localAccount;

                tested.updateAccount( localAccount, timestamp );
                times = 1;

                timestamp.isObsolete();
                result = false;
            }
        };

        PubsubMessage message = emailChangedPubsubMessage();
        tested.onMessage( message, "account.changes" );

        assertThat( localAccount.getEmail() ).isEqualTo( EMAIL_CHANGED );
        assertThat( localAccount.getLocale() ).isEqualTo( Locale.ENGLISH );

        new Verifications()
        {
            {
                LocalAccountProvider.Builder builder;
                lap.initGet( builder = withCapture() );

                assertThat( builder ).isNotNull();
                assertThat( builder.getAccountId() ).isEqualTo( ACCOUNT_ID );
                assertThat( builder.getEmail() ).isEqualTo( EMAIL_CHANGED );
                assertThat( builder.getIdentityId() ).isEqualTo( IDENTITY_ID );
            }
        };
    }

    @Test
    public void onMessage_ValidPubsubMessage_LocaleChanged() throws Exception
    {
        LocalAccount localAccount = new LocalAccount( new LocalAccountProvider.Builder()
                .accountId( ACCOUNT_ID )
                .email( EMAIL )
                .identityId( IDENTITY_ID )
                .audience( AUDIENCE ) );
        localAccount.setLocale( "de" );
        localAccount.setZoneId( DEFAULT_ZONE );

        new Expectations( localAccount, tested )
        {
            {
                //noinspection ConstantConditions
                lap.initGet( ( LocalAccountProvider.Builder ) any );
                result = localAccount;

                tested.updateAccount( localAccount, timestamp );
                times = 1;

                timestamp.isObsolete();
                result = false;
            }
        };

        PubsubMessage message = validPubsubMessage();
        tested.onMessage( message, "account.changes" );

        assertThat( localAccount.getEmail() ).isEqualTo( EMAIL );
        assertThat( localAccount.getLocale() ).isEqualTo( Locale.ENGLISH );
    }

    @Test
    public void onMessage_ValidPubsubMessage_UninterestedDataType() throws Exception
    {
        PubsubMessage message = uninterestedPubsubMessage();
        tested.onMessage( message, "account.changes" );

        new Verifications()
        {
            {
                //noinspection ConstantConditions
                lap.initGet( ( LocalAccountProvider.Builder ) any );
                times = 0;
            }
        };
    }

    @Test
    public void onMessage_InvalidPubsubMessage() throws Exception
    {
        PubsubMessage message = invalidPubsubMessage();
        tested.onMessage( message, "account.changes" );

        new Verifications()
        {
            {
                //noinspection ConstantConditions
                lap.initGet( ( LocalAccountProvider.Builder ) any );
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

    private PubsubMessage invalidPubsubMessage() throws IOException
    {
        return incompletePubsubMessageBuilder( Account.class.getSimpleName() ).build().getMessages().get( 0 );
    }

    private PubsubMessage validPubsubMessage() throws IOException
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
                .addAttribute( ENCODED_UNIQUE_KEY, id )
                .addAttribute( ACCOUNT_AUDIENCE, AUDIENCE );

        return builder;
    }
}