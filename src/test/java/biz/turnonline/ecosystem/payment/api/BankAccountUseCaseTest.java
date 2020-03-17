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

package biz.turnonline.ecosystem.payment.api;

import biz.turnonline.ecosystem.payment.api.model.BankAccount;
import biz.turnonline.ecosystem.payment.service.BackendServiceTestCase;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.steward.model.Account;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.NotFoundException;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import org.ctoolkit.agent.service.impl.ImportTask;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.testng.FileAssert.fail;

/**
 * {@link BankAccount} service wide use case testing against emulated (local) App Engine services.
 * Calls crossing the boundaries of this microservice are mocked.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class BankAccountUseCaseTest
        extends BackendServiceTestCase
{
    @Inject
    private BankAccountEndpoint endpoint;

    @Mocked
    private HttpServletRequest request;

    @Mocked
    private User authUser;

    private Account account;

    private LocalAccount lAccount;

    @BeforeMethod
    public void before()
    {
        account = genericJsonFromFile( "account.json", Account.class );
        lAccount = new LocalAccount( account );
        lAccount.save();

        // import bank code code-book
        ImportTask task = new ImportTask( "/dataset/changeset_00001.xml" );
        task.run();

        // import test bank accounts
        task = new ImportTask( "/testdataset/changeset_local-account.xml" );
        task.run();
    }

    @Test
    public void bankAccountFullLifecycle() throws Exception
    {
        mockUpCheckAccount();
        mockUpLocalAccount();

        // creating bank accounts
        BankAccount apiBankAccount = getFromFile( "bank-account-1.json", BankAccount.class );
        BankAccount firstBankAccount = endpoint.insertBankAccount( apiBankAccount, request, authUser );

        assertThat( firstBankAccount ).isNotNull();
        assertThat( firstBankAccount.getBank().getCode() ).isEqualTo( "1100" );
        assertThat( firstBankAccount.getBank().getCountry() ).isEqualTo( "SK" );
        assertThat( firstBankAccount.getBank().getLabel() ).isEqualTo( "Tatra banka, a.s." );
        assertThat( firstBankAccount.isPrimary() ).isFalse();

        apiBankAccount = getFromFile( "bank-account-2.json", BankAccount.class );
        BankAccount secondBankAccount = endpoint.insertBankAccount( apiBankAccount, request, authUser );

        assertThat( secondBankAccount ).isNotNull();
        assertThat( secondBankAccount.getBank().getLabel() ).isNotNull();
        assertThat( secondBankAccount.getBank().getCode() ).isEqualTo( "0900" );
        assertThat( secondBankAccount.getBank().getCountry() ).isEqualTo( "SK" );
        assertThat( secondBankAccount.isPrimary() ).isFalse();

        apiBankAccount = getFromFile( "bank-account-3.json", BankAccount.class );
        BankAccount thirdBankAccount = endpoint.insertBankAccount( apiBankAccount, request, authUser );

        assertThat( thirdBankAccount ).isNotNull();
        assertThat( thirdBankAccount.getBank().getLabel() ).isNotNull();
        assertThat( thirdBankAccount.getBank().getCode() ).isEqualTo( "8360" );
        assertThat( thirdBankAccount.getBank().getCountry() ).isEqualTo( "SK" );
        assertThat( thirdBankAccount.isPrimary() ).isFalse();

        apiBankAccount = getFromFile( "bank-account-2.json", BankAccount.class );
        BankAccount duplicatedBankAccount = endpoint.insertBankAccount( apiBankAccount, request, authUser );
        assertThat( duplicatedBankAccount ).isNotNull();
        assertThat( duplicatedBankAccount.getBank().getCode() ).isEqualTo( "0900" );
        assertThat( duplicatedBankAccount.isPrimary() ).isFalse();

        // getting bank account test
        ofy().clear();

        BankAccount bankAccount = endpoint.getBankAccount( firstBankAccount.getId(), request, authUser );
        assertThat( bankAccount ).isNotNull();
        assertThat( bankAccount ).isEqualTo( firstBankAccount );

        // bank account update
        duplicatedBankAccount.setName( "Bank account that will be removed later" );
        Long id = duplicatedBankAccount.getId();

        endpoint.updateBankAccount( id, duplicatedBankAccount, request, authUser );
        ofy().clear();

        bankAccount = endpoint.getBankAccount( id, request, authUser );
        assertThat( bankAccount ).isNotNull();
        assertThat( bankAccount ).isEqualTo( duplicatedBankAccount );

        // getting primary account
        try
        {
            endpoint.getPrimaryBankAccount( "SK", request, authUser );
            fail( "There should not be a primary account at all" );
        }
        catch ( NotFoundException ignored )
        {
        }

        // delete bank account
        endpoint.deleteBankAccount( id, request, authUser );
        try
        {
            endpoint.getBankAccount( id, request, authUser );
            fail( "The bank account '" + duplicatedBankAccount.getName() + "' should be already deleted." );
        }
        catch ( NotFoundException ignored )
        {
        }

        // marking primary bank account
        bankAccount = endpoint.markBankAccountAsPrimary( firstBankAccount.getId(), request, authUser );
        assertThat( bankAccount ).isNotNull();
        assertThat( bankAccount.getId() ).isEqualTo( firstBankAccount.getId() );
        assertThat( bankAccount.isPrimary() ).isTrue();

        BankAccount primary = endpoint.getPrimaryBankAccount( "SK", request, authUser );
        assertThat( primary ).isNotNull();
        assertThat( primary.isPrimary() ).isTrue();
        assertThat( primary ).isEqualTo( bankAccount );

        try
        {
            endpoint.deleteBankAccount( primary.getId(), request, authUser );
            fail( "The primary bank account cannot be deleted" );
        }
        catch ( BadRequestException ignored )
        {
        }

        // getting alternative bank accounts
        List<BankAccount> alternative = endpoint.searchBankAccounts( null, null, "Sk",
                null, true, request, authUser );

        assertThat( alternative ).isNotNull();
        assertThat( alternative ).hasSize( 2 );
        assertThat( alternative ).doesNotContain( primary );
    }

    private void mockUpCheckAccount()
    {
        new MockUp<EndpointsCommon>()
        {
            @Mock
            public LocalAccount checkAccount( User authUser, HttpServletRequest request )
            {
                return lAccount;
            }
        };
    }

    private void mockUpLocalAccount()
    {
        new MockUp<LocalAccount>()
        {
            @Mock
            public Account getRemote()
            {
                return account;
            }
        };
    }
}
