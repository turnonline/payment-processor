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
import com.google.api.gax.rpc.NotFoundException;
import com.google.api.gax.rpc.StatusCode;
import com.google.appengine.api.utils.SystemProperty;
import com.google.cloud.secretmanager.v1beta1.AccessSecretVersionRequest;
import com.google.cloud.secretmanager.v1beta1.AddSecretVersionRequest;
import com.google.cloud.secretmanager.v1beta1.CreateSecretRequest;
import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceClient;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.testng.annotations.Test;

import static biz.turnonline.ecosystem.payment.oauth.RevolutCertMetadata.PRIVATE_KEY_NAME;
import static biz.turnonline.ecosystem.payment.oauth.RevolutCredentialAdministration.REFRESH_TOKEN_NAME;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * {@link RevolutCredentialAdministration} unit testing incl. tests against emulated (local) App Engine datastore.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class RevolutCredentialAdministrationDbTest
        extends BackendServiceTestCase
{
    public static final String REFRESH_TOKEN = "oa_sand_GhcvXvB..";

    private static final String CLIENT_ID = "client_X123cyx";

    static
    {
        SystemProperty.applicationId.set( "b2x-app" );
    }

    @Tested
    private RevolutCredentialAdministration tested;

    @Mocked
    private SecretManagerServiceClient client;

    @Mocked
    private StatusCode statusCode;

    @Test
    public void storeClientId()
    {
        String clientId = "123";
        tested.storeClientId( clientId );

        assertWithMessage( "Revolut stored client ID" )
                .that( tested.clientId() )
                .isEqualTo( clientId );
    }

    @Test
    public void storeCode()
    {
        String clientId = "123";
        tested.storeClientId( clientId );
        // mark as granted to check later whether new authorisation code will change it to false
        tested.get().accessGranted().save();

        String code = "123";
        tested.storeCode( code );

        assertWithMessage( "Revolut access authorised" )
                .that( tested.get().isAccessAuthorised() )
                .isFalse();

        assertWithMessage( "Revolut stored authorisation code" )
                .that( tested.getCode( clientId ) )
                .isEqualTo( code );
    }

    @Test
    public void clientId()
    {
        RevolutCertMetadata details = tested.get().setClientId( CLIENT_ID );
        details.save();

        // first not cached yet
        assertWithMessage( "Revolut stored Client ID" )
                .that( tested.clientId() )
                .isEqualTo( CLIENT_ID );

        // now getting cached version
        new Expectations( tested )
        {
            {
                tested.get();
                result = details;
                times = 0;
            }
        };

        assertWithMessage( "Revolut cached Client ID" )
                .that( tested.clientId() )
                .isEqualTo( CLIENT_ID );
    }

    @Test
    public void clientId_NotSetYet()
    {
        assertWithMessage( "Revolut Client ID" )
                .that( tested.clientId() )
                .isNull();
    }

    @Test
    public void issuer()
    {
        assertWithMessage( "Revolut issuer (env variable)" )
                .that( tested.issuer() )
                .isEqualTo( "payment.service.cloud" );
    }

    @Test
    public void getCode()
    {
        String code = "oa_sand_AmHJf..";
        RevolutCertMetadata details = tested.get().setCode( code );
        details.save();

        assertWithMessage( "Revolut authorisation code" )
                .that( tested.getCode( CLIENT_ID ) )
                .isEqualTo( code );
    }

    @Test
    public void getRefreshToken()
    {
        // set precondition first
        tested.get().accessGranted();

        new Expectations( tested )
        {
            {
                tested.readRefreshToken( client );
                result = REFRESH_TOKEN;
                times = 1;
            }
        };

        // first not cached yet
        assertWithMessage( "Revolut refresh token" )
                .that( tested.getRefreshToken( CLIENT_ID ) )
                .isEqualTo( REFRESH_TOKEN );

        // now getting cached version
        new Expectations( tested )
        {
            {
                tested.readRefreshToken( client );
                result = REFRESH_TOKEN;
                times = 0;
            }
        };

        assertWithMessage( "Revolut cached refresh token" )
                .that( tested.getRefreshToken( CLIENT_ID ) )
                .isEqualTo( REFRESH_TOKEN );
    }

    @Test
    public void getRefreshToken_AccessNotGrantedYet()
    {
        new Expectations( tested )
        {
            {
                tested.readRefreshToken( client );
                result = REFRESH_TOKEN;
                times = 0;
            }
        };

        assertWithMessage( "Revolut refresh token" )
                .that( tested.getRefreshToken( CLIENT_ID ) )
                .isNull();
    }

    @Test
    public void getRefreshToken_CheckAccessRequest()
    {
        // set precondition first
        tested.get().accessGranted();

        // test call
        tested.getRefreshToken( CLIENT_ID );

        new Verifications()
        {
            {
                AccessSecretVersionRequest request;
                client.accessSecretVersion( request = withCapture() );

                assertWithMessage( "Read refresh token request" )
                        .that( request )
                        .isNotNull();

                assertWithMessage( "Read refresh token path name" )
                        .that( request.getName() )
                        .isEqualTo( "projects/b2x-app/secrets/" + REFRESH_TOKEN_NAME + "/versions/latest" );
            }
        };
    }

    @Test
    public void getRefreshToken_Exception()
    {
        // set precondition first
        tested.get().accessGranted();

        new Expectations( tested )
        {
            {
                tested.readRefreshToken( client );
                result = new RuntimeException( "Something went wrong" );
            }
        };

        assertWithMessage( "Revolut refresh token" )
                .that( tested.getRefreshToken( CLIENT_ID ) )
                .isNull();
    }

    @Test
    public void getSecretKey()
    {
        new Expectations( tested )
        {
            {
                tested.readSecretKey( client, PRIVATE_KEY_NAME );
                result = new byte[0];
                times = 1;
            }
        };

        // first not cached yet
        assertWithMessage( "Revolut private key" )
                .that( tested.getSecretKey( CLIENT_ID ) )
                .isNotNull();

        // now getting cached version
        new Expectations( tested )
        {
            {
                tested.readSecretKey( client, anyString );
                result = REFRESH_TOKEN;
                times = 0;
            }
        };

        assertWithMessage( "Revolut cached private key" )
                .that( tested.getSecretKey( CLIENT_ID ) )
                .isNotNull();
    }

    @Test
    public void getSecretKey_NotSetYet()
    {
        new Expectations( tested )
        {
            {
                tested.readSecretKey( client, PRIVATE_KEY_NAME );
                result = null;
                times = 1;
            }
        };

        assertWithMessage( "Revolut private key" )
                .that( tested.getSecretKey( CLIENT_ID ) )
                .isNull();
    }

    @Test
    public void getSecretKey_CheckAccessRequest()
    {
        tested.getSecretKey( CLIENT_ID );

        new Verifications()
        {
            {
                AccessSecretVersionRequest request;
                client.accessSecretVersion( request = withCapture() );

                assertWithMessage( "Read private key request" )
                        .that( request )
                        .isNotNull();

                assertWithMessage( "Read private key path name" )
                        .that( request.getName() )
                        .isEqualTo( "projects/b2x-app/secrets/" + PRIVATE_KEY_NAME + "/versions/latest" );
            }
        };
    }

    @Test
    public void getSecretKey_Exception()
    {
        new Expectations( tested )
        {
            {
                tested.readSecretKey( client, PRIVATE_KEY_NAME );
                result = new RuntimeException( "Something went wrong" );
            }
        };

        assertWithMessage( "Revolut private key" )
                .that( tested.getSecretKey( CLIENT_ID ) )
                .isNull();
    }

    @Test
    public void store_SecretAlreadyExist()
    {
        String code = "123";
        tested.storeCode( code );

        new Expectations( tested )
        {
            {
                tested.createRefreshTokenSecret( client );
                times = 0;
            }
        };

        tested.store( CLIENT_ID, REFRESH_TOKEN );

        assertWithMessage( "Revolut refresh token" )
                .that( tested.getRefreshToken( CLIENT_ID ) )
                .isEqualTo( REFRESH_TOKEN );

        assertWithMessage( "Revolut access authorised" )
                .that( tested.get().isAccessAuthorised() )
                .isTrue();

        assertWithMessage( "Revolut authorisation code has been consumed" )
                .that( tested.getCode( CLIENT_ID ) )
                .isNull();

        new Verifications()
        {
            {
                AddSecretVersionRequest request;
                client.addSecretVersion( request = withCapture() );

                assertWithMessage( "Add secret version request" )
                        .that( request )
                        .isNotNull();

                assertWithMessage( "Refresh token path name" )
                        .that( request.getParent() )
                        .isEqualTo( "projects/b2x-app/secrets/" + REFRESH_TOKEN_NAME );

                assertWithMessage( "Refresh token sent to secret manager" )
                        .that( request.getPayload().getData().toStringUtf8() )
                        .isEqualTo( REFRESH_TOKEN );
            }
        };
    }

    @Test
    public void store_SecretNotExistYet()
    {
        String code = "123";
        tested.storeCode( code );

        new Expectations( tested )
        {
            {
                tested.addRefreshToken( client, REFRESH_TOKEN );
                result = new NotFoundException( new RuntimeException(), statusCode, false );
                result = null;
                times = 2;
            }
        };

        tested.store( CLIENT_ID, REFRESH_TOKEN );

        assertWithMessage( "Revolut refresh token" )
                .that( tested.getRefreshToken( CLIENT_ID ) )
                .isEqualTo( REFRESH_TOKEN );

        assertWithMessage( "Revolut access authorised" )
                .that( tested.get().isAccessAuthorised() )
                .isTrue();

        assertWithMessage( "Revolut authorisation code has been consumed" )
                .that( tested.getCode( CLIENT_ID ) )
                .isNull();

        new Verifications()
        {
            {
                CreateSecretRequest request;
                client.createSecret( request = withCapture() );

                assertWithMessage( "Create secret request" )
                        .that( request )
                        .isNotNull();

                assertWithMessage( "Secrete manager Project Id" )
                        .that( request.getParent() )
                        .isEqualTo( "projects/b2x-app" );

                assertWithMessage( "Refresh token secrete name" )
                        .that( request.getSecretId() )
                        .isEqualTo( REFRESH_TOKEN_NAME );
            }
        };
    }

    @Test
    public void store_Exception()
    {
        String code = "123";
        tested.storeCode( code );

        new Expectations( tested )
        {
            {
                tested.addRefreshToken( client, REFRESH_TOKEN );
                result = new RuntimeException( "Something went wrong" );
            }
        };

        tested.store( CLIENT_ID, REFRESH_TOKEN );

        assertWithMessage( "Revolut refresh token" )
                .that( tested.getRefreshToken( CLIENT_ID ) )
                .isNull();

        assertWithMessage( "Revolut authorisation code has been consumed" )
                .that( tested.getCode( CLIENT_ID ) )
                .isNull();
    }
}