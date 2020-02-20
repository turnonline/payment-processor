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

import biz.turnonline.ecosystem.revolut.business.account.model.Account;
import biz.turnonline.ecosystem.revolut.business.facade.RevolutBusinessProvider;
import com.googlecode.objectify.Key;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.ctoolkit.restapi.client.ClientErrorException;
import org.ctoolkit.restapi.client.ListRetrievalRequest;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.restapi.client.UnauthorizedException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * {@link RevolutExchangeAuthorisationCode} unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class RevolutExchangeAuthorisationCodeTest
{
    @Tested
    private RevolutExchangeAuthorisationCode tested;

    @Injectable
    private RestFacade facade;

    @Injectable
    private RevolutBusinessProvider revolut;

    @Injectable
    private Key<RevolutCertMetadata> key;

    @Mocked
    private ListRetrievalRequest<Account> request;

    private RevolutCertMetadata details;

    @BeforeMethod
    public void before()
    {
        details = new RevolutCertMetadata();
        details.setClientId( "client-id-123" );
        details.setCode( "auth-code-456.." );
    }

    @Test
    public void execute_AccessGranted()
    {
        new Expectations( tested, details )
        {
            {
                tested.workWith( null );
                result = details;

                details.accessGranted();
                details.save();
            }
        };

        tested.execute();
    }

    @Test
    public void execute_Unauthorized()
    {
        new Expectations( tested, details )
        {
            {
                request.finish();
                result = new UnauthorizedException( "Unauthorized" );

                tested.workWith( null );
                result = details;

                details.save();
            }
        };

        tested.execute();

        assertWithMessage( "Authorisation code has been reset" )
                .that( details.getCode() )
                .isNull();

        new Verifications()
        {
            {
                details.accessGranted();
                times = 0;
            }
        };
    }

    @Test
    public void execute_ClientError()
    {
        new Expectations( tested, details )
        {
            {
                request.finish();
                result = new ClientErrorException( "Client error" );

                tested.workWith( null );
                result = details;

                details.save();
            }
        };

        tested.execute();

        assertWithMessage( "Authorisation code has been reset" )
                .that( details.getCode() )
                .isNull();

        new Verifications()
        {
            {
                details.accessGranted();
                times = 0;
            }
        };
    }

    @Test
    public void execute_Forbidden()
    {
        new Expectations( tested, details )
        {
            {
                request.finish();
                result = new ClientErrorException( "Forbidden" );

                tested.workWith( null );
                result = details;

                details.save();
            }
        };

        tested.execute();

        assertWithMessage( "Authorisation code has been reset" )
                .that( details.getCode() )
                .isNull();

        new Verifications()
        {
            {
                details.accessGranted();
                times = 0;
            }
        };
    }
}