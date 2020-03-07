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

package biz.turnonline.ecosystem.payment.oauth;

import biz.turnonline.ecosystem.payment.service.BackendServiceTestCase;
import biz.turnonline.ecosystem.payment.service.revolut.RevolutDebtorBankAccountsInitTest;
import biz.turnonline.ecosystem.revolut.business.account.model.Account;
import biz.turnonline.ecosystem.revolut.business.facade.RevolutBusinessProvider;
import com.google.api.client.auth.oauth2.TokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.json.Json;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceClient;
import com.google.common.io.ByteStreams;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import org.ctoolkit.restapi.client.RestFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.api.client.googleapis.testing.auth.oauth2.MockGoogleCredential.ACCESS_TOKEN;
import static com.google.api.client.googleapis.testing.auth.oauth2.MockGoogleCredential.REFRESH_TOKEN;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Unit testing Revolut OAuth2 refresh access flow against emulated datastore.
 * Mirroring the functionality from {@link RevolutOauth2AuthRedirect}.
 * <p>
 * This test case is being configured to keep the datastore state for whole class lifecycle.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@SuppressWarnings( "ResultOfMethodCallIgnored" )
public class RevolutRefreshAccessFlowDbTest
        extends BackendServiceTestCase
{
    private static final Logger LOGGER = LoggerFactory.getLogger( RevolutRefreshAccessFlowDbTest.class );

    private static final String CLIENT_ID = "client-123xvB";

    private static final String AUTHORISATION_CODE = "oa_sand_xC123..";

    private RevolutExchangeAuthorisationCode tested;

    @Inject
    private RevolutCredentialAdministration administration;

    @Inject
    private RevolutBusinessProvider revolut;

    @Inject
    @Injectable
    private RestFacade facade;

    @Mocked
    private SecretManagerServiceClient client;

    @Injectable
    private HttpResponse revolutResponse;

    private Date authorisedOn;

    @BeforeClass
    public void beforeClass()
    {
        super.helper.setUp();

        // Client ID provided upfront and access is granted.
        RevolutCertMetadata metadata = administration.get().setClientId( CLIENT_ID );
        metadata.accessGranted();
        metadata.save();
        LOGGER.info( "Initial state of " + metadata );

        authorisedOn = metadata.getAuthorisedOn();

        tested = new RevolutExchangeAuthorisationCode( metadata.entityKey() );
        tested.facade = facade;
        tested.revolut = revolut;
    }

    public void beforeMethod()
    {
        // we want to keep the state for class lifecycle
    }

    public void afterMethod()
    {
        // we want to keep the state for class lifecycle
        ofy().flush();
    }

    @AfterClass
    public void afterClass()
    {
        super.helper.tearDown();
    }

    /**
     * The standard step, refresh token is valid and calling Revolut API works fine (access granted).
     * Access token will be refreshed.
     */
    @Test
    public <R extends LowLevelHttpRequest> void step1_StandardCallRevolutAPI()
    {
        // pre-conditions checks
        assertWithMessage( "Revolut authorised on" )
                .that( authorisedOn )
                .isNotNull();

        assertWithMessage( "New authorisation code" )
                .that( administration.get().isNewCode() )
                .isFalse();

        mockUpSecretManager();

        // mock-up of the token server, happy answer
        new MockUp<TokenRequest>()
        {
            @Mock
            public TokenResponse execute()
            {
                return new TokenResponse()
                        .setRefreshToken( REFRESH_TOKEN )
                        .setAccessToken( ACCESS_TOKEN );
            }
        };

        // mock-up successful Revolut result
        new MockUp<R>()
        {
            @Mock
            public LowLevelHttpResponse execute( Invocation invocation )
            {
                String name = "revo-accounts.json";
                InputStream stream = RevolutDebtorBankAccountsInitTest.ListArray.class.getResourceAsStream( name );
                return new MockLowLevelHttpResponse().setContent( stream );
            }
        };

        AtomicReference<Boolean> tokenStored = new AtomicReference<>( false );
        new MockUp<RevolutCredentialAdministration>()
        {
            @Mock
            public void store( @Nonnull String clientId, @Nonnull String token )
            {
                tokenStored.set( true );
            }
        };

        // test call
        List<Account> accounts = facade.list( Account.class ).finish();

        assertWithMessage( "Revolut accounts" )
                .that( accounts )
                .isNotEmpty();

        assertWithMessage( "Refresh token stored" )
                .that( tokenStored.get() )
                .isFalse();

        assertWithMessage( "Revolut authorised on" )
                .that( administration.get().getAuthorisedOn() )
                .isEqualTo( authorisedOn );

        assertWithMessage( "Revolut authorisation code reset" )
                .that( administration.getCode( CLIENT_ID ) )
                .isNull();

        assertWithMessage( "New authorisation code" )
                .that( administration.get().isNewCode() )
                .isFalse();
    }

    /**
     * Use case when client will decide to refresh access (Revolut Certificate Settings)
     * even the current one did not expire yet. But for some reason the authorisation code is invalid
     * (Revolut API will refuse the call as 400).
     */
    @Test
    public void step2_NewAuthorisationCode_Unauthorized() throws IOException
    {
        // authorisation code taken from query parameter, that's coming when client has decided to refresh access
        administration.storeCode( AUTHORISATION_CODE );

        mockUpSecretManager();

        new Expectations()
        {
            {
                revolutResponse.getStatusCode();
                result = HttpStatusCodes.STATUS_CODE_UNAUTHORIZED;

                revolutResponse.getContentType();
                result = Json.MEDIA_TYPE;

                // details needed to make sure AccessToken will be reset, see Credential.refreshToken()
                revolutResponse.getContent();
                String name = "revolut_unauthorized_client.json";
                result = RevolutRefreshAccessFlowDbTest.class.getResourceAsStream( name );

                revolutResponse.getHeaders();
                result = new HttpHeaders();
            }
        };

        // mock-up of the token server, unauthorized response
        new MockUp<TokenRequest>()
        {
            @Mock
            public TokenResponse execute() throws IOException
            {
                throw TokenResponseException.from( JacksonFactory.getDefaultInstance(), revolutResponse );
            }
        };

        AtomicReference<Boolean> tokenStored = new AtomicReference<>( false );
        new MockUp<RevolutCredentialAdministration>()
        {
            @Mock
            public void store( @Nonnull String clientId, @Nonnull String token )
            {
                tokenStored.set( true );
            }
        };

        // test call
        tested.execute();

        assertWithMessage( "Refresh token stored" )
                .that( tokenStored.get() )
                .isFalse();

        assertWithMessage( "Revolut authorised on" )
                .that( administration.get().getAuthorisedOn() )
                .isEqualTo( authorisedOn );

        assertWithMessage( "Authorisation code reset" )
                .that( administration.getCode( CLIENT_ID ) )
                .isNull();

        assertWithMessage( "Authorisation code has been consumed" )
                .that( !administration.get().isNewCode() )
                .isTrue();
    }

    /**
     * In previous step the new authorisation code failed, however current refresh token is still valid.
     * Next call to Revolut API should then succeed.
     * <p>
     * Previous step has reset access token, token server to refresh token will be called.
     */
    @Test
    public <R extends LowLevelHttpRequest> void step3_StandardCallRevolutAPI()
    {
        mockUpSecretManager();

        // mock-up of the token server, happy answer
        new MockUp<TokenRequest>()
        {
            @Mock
            public TokenResponse execute()
            {
                return new TokenResponse()
                        .setRefreshToken( REFRESH_TOKEN )
                        .setAccessToken( ACCESS_TOKEN );
            }
        };

        // mock-up successful Revolut result
        new MockUp<R>()
        {
            @Mock
            public LowLevelHttpResponse execute( Invocation invocation )
            {
                String name = "revo-accounts.json";
                InputStream stream = RevolutDebtorBankAccountsInitTest.ListArray.class.getResourceAsStream( name );
                return new MockLowLevelHttpResponse().setContent( stream );
            }
        };

        // test call
        List<Account> accounts = facade.list( Account.class ).finish();

        assertWithMessage( "Revolut accounts" )
                .that( accounts )
                .isNotEmpty();
    }

    /**
     * Client has decided to refresh access (Revolut Certificate Settings) even the current one did not expire yet.
     * Exchange of the authorisation code will be successful.
     */
    @Test
    public <R extends LowLevelHttpRequest> void step4_ExchangeAuthorisationCode()
    {
        // authorisation code taken from query parameter, that's coming when client has decided to refresh access
        administration.storeCode( AUTHORISATION_CODE );

        mockUpSecretManager();

        // mock-up of the token server, happy answer
        new MockUp<TokenRequest>()
        {
            @Mock
            public TokenResponse execute()
            {
                return new TokenResponse()
                        .setRefreshToken( REFRESH_TOKEN )
                        .setAccessToken( ACCESS_TOKEN );
            }
        };

        // mock-up successful Revolut result
        new MockUp<R>()
        {
            @Mock
            public LowLevelHttpResponse execute( Invocation invocation )
            {
                // first call is the call to the Revolut API
                if ( invocation.getInvocationCount() > 1 )
                {
                    return invocation.proceed();
                }

                // rest of the calls are datastore (Objectify) calls
                String name = "revo-accounts.json";
                InputStream stream = RevolutDebtorBankAccountsInitTest.ListArray.class.getResourceAsStream( name );
                return new MockLowLevelHttpResponse().setContent( stream );
            }
        };

        AtomicReference<Boolean> tokenStored = new AtomicReference<>( false );
        new MockUp<RevolutCredentialAdministration>()
        {
            @Mock
            public void store( @Nonnull String clientId, @Nonnull String token )
            {
                assertWithMessage( "New refresh token to be stored" )
                        .that( token )
                        .isEqualTo( REFRESH_TOKEN );

                tokenStored.set( true );
            }
        };

        // test call
        tested.execute();

        assertWithMessage( "Refresh token stored" )
                .that( tokenStored.get() )
                .isTrue();

        assertWithMessage( "Revolut has been re-authorised on" )
                .that( administration.get().getAuthorisedOn() )
                .isGreaterThan( authorisedOn );

        assertWithMessage( "Revolut authorisation code reset" )
                .that( administration.getCode( CLIENT_ID ) )
                .isNull();

        assertWithMessage( "Authorisation code has been consumed" )
                .that( !administration.get().isNewCode() )
                .isTrue();
    }

    /**
     * The standard step, refresh token is valid and calling Revolut API works fine (access granted).
     * Access token doesn't have to be refreshed (as it was done in previous step).
     */
    @Test
    public <R extends LowLevelHttpRequest> void step5_StandardCallRevolutAPI()
    {
        mockUpSecretManager();

        // mock-up of the token server, expected to be not called (refresh and access token is  ull)
        new MockUp<TokenRequest>()
        {
            @Mock
            public TokenResponse execute()
            {
                return new TokenResponse();
            }
        };

        // mock-up successful Revolut result
        new MockUp<R>()
        {
            @Mock
            public LowLevelHttpResponse execute( Invocation invocation )
            {
                String name = "revo-accounts.json";
                InputStream stream = RevolutDebtorBankAccountsInitTest.ListArray.class.getResourceAsStream( name );
                return new MockLowLevelHttpResponse().setContent( stream );
            }
        };

        // test call
        List<Account> accounts = facade.list( Account.class ).finish();

        assertWithMessage( "Revolut accounts" )
                .that( accounts )
                .isNotEmpty();
    }

    /**
     * Mock-up of the secret manager results.
     */
    private void mockUpSecretManager()
    {
        new MockUp<RevolutCredentialAdministration>()
        {
            @Mock
            SecretManagerServiceClient client()
            {
                return client;
            }

            @Mock
            String readRefreshToken( SecretManagerServiceClient client )
            {
                return REFRESH_TOKEN;
            }

            @Mock
            void createRefreshTokenSecret( SecretManagerServiceClient client )
            {
            }

            @Mock
            byte[] readSecretKey( SecretManagerServiceClient client, String secretId ) throws IOException
            {
                return ByteStreams.toByteArray( getClass().getResourceAsStream( "rsa_private_pkcs8" ) );
            }

            @Mock
            void addRefreshToken( SecretManagerServiceClient client, String refreshToken )
            {
            }
        };
    }
}
