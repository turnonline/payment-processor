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

package biz.turnonline.ecosystem.payment.service.revolut;

import biz.turnonline.ecosystem.payment.service.CodeBook;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.revolut.business.account.model.Account;
import biz.turnonline.ecosystem.revolut.business.account.model.AccountBankDetailsItem;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.ctoolkit.restapi.client.ListRetrievalRequest;
import org.ctoolkit.restapi.client.RestFacade;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static biz.turnonline.ecosystem.payment.service.BackendServiceTestCase.getFromFile;
import static biz.turnonline.ecosystem.payment.service.PaymentConfig.REVOLUT_BANK_EU_CODE;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * {@link RevolutDebtorBankAccountsInit} init testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@SuppressWarnings( "unchecked" )
public class RevolutDebtorBankAccountsInitTest
{
    private static final String IBAN = "GB51 REVO 0099 6908 4603 55";

    private static final String BIC = "REVOGB21";

    private final LocalAccount account = new LocalAccount( new biz.turnonline.ecosystem.steward.model.Account()
            .setId( 18495L )
            .setEmail( "my.account@turnonline.biz" )
            .setIdentityId( "87hHtr6uJ" )
            .setAudience( "b2c" ) );

    @Injectable
    private final String bankCode = REVOLUT_BANK_EU_CODE;

    @Tested
    private RevolutDebtorBankAccountsInit tested;

    @Injectable
    private Key<LocalAccount> accountKey;

    @Injectable
    private RestFacade facade;

    @Injectable
    private PaymentConfig config;

    @Injectable
    private CodeBook codeBook;

    @Mocked
    private ListRetrievalRequest<?> authBy;

    private List<Account> accounts;

    private List<AccountBankDetailsItem> details1;

    private List<AccountBankDetailsItem> details2;

    private List<AccountBankDetailsItem> details3;

    @BeforeMethod
    public void before()
    {
        accounts = getFromFile( "revo-accounts.json", ListArray.class );
        details1 = getFromFile( "details-ace7f9ac-4a89-11ea-b77f-2e728ce88120.json", DetailsArray.class );
        details2 = getFromFile( "details-ace7fdf8-4a89-11ea-b77f-2e728ce88123.json", DetailsArray.class );
        details3 = getFromFile( "details-ace7ff88-4a89-11ea-b77f-2e728ce88129.json", DetailsArray.class );

        new MockUp<Ref<?>>()
        {
            @Mock
            public Ref<?> create( Object value )
            {
                return null;
            }
        };
    }

    @Test
    public void execute_BankAccountsSuccessfullyStored()
    {
        new Expectations( tested )
        {
            {
                tested.workWith();
                result = account;

                tested.save( ( List<CompanyBankAccount> ) any );

                authBy.finish();
                result = accounts;
                result = details1;
                result = details2;
                result = details3;
            }
        };

        tested.execute();

        new Verifications()
        {
            {
                List<CompanyBankAccount> bas;
                tested.save( bas = withCapture() );

                assertWithMessage( "List of company bank accounts" )
                        .that( bas )
                        .isNotNull();

                assertWithMessage( "Number of company bank accounts stored" )
                        .that( bas )
                        .hasSize( 3 );

                // bank account 1
                CompanyBankAccount cba = bas.get( 0 );
                assertWithMessage( "Company bank account.1 IBAN" )
                        .that( cba.getIbanString() )
                        .isEqualTo( IBAN );

                assertWithMessage( "Company bank account.1 BIC" )
                        .that( cba.getBic() )
                        .isEqualTo( BIC );

                assertWithMessage( "Company bank account.1 Currency" )
                        .that( cba.getCurrency() )
                        .isEqualTo( "EUR" );

                assertWithMessage( "Company bank account.1 External ID" )
                        .that( cba.getExternalId() )
                        .isEqualTo( "ace7f9ac-4a89-11ea-b77f-2e728ce88120" );

                // bank account 2
                cba = bas.get( 1 );
                assertWithMessage( "Company bank account.2 IBAN" )
                        .that( cba.getIbanString() )
                        .isEqualTo( IBAN );

                assertWithMessage( "Company bank account.2 BIC" )
                        .that( cba.getBic() )
                        .isEqualTo( BIC );

                assertWithMessage( "Company bank account.2 Currency" )
                        .that( cba.getCurrency() )
                        .isEqualTo( "USD" );

                assertWithMessage( "Company bank account.2 External ID" )
                        .that( cba.getExternalId() )
                        .isEqualTo( "ace7fdf8-4a89-11ea-b77f-2e728ce88123" );

                // bank account 3
                cba = bas.get( 2 );
                assertWithMessage( "Company bank account.3 IBAN" )
                        .that( cba.getIbanString() )
                        .isEqualTo( IBAN );

                assertWithMessage( "Company bank account.3 BIC" )
                        .that( cba.getBic() )
                        .isEqualTo( BIC );

                assertWithMessage( "Company bank account.3 Currency" )
                        .that( cba.getCurrency() )
                        .isEqualTo( "GBP" );

                assertWithMessage( "Company bank account.3 External ID" )
                        .that( cba.getExternalId() )
                        .isEqualTo( "ace7ff88-4a89-11ea-b77f-2e728ce88129" );
            }
        };
    }

    @Test
    public void execute_BankAccountsSuccessfullyStored_ExclInactive()
    {
        accounts.get( 1 ).setState( Account.StateEnum.INACTIVE );
        new Expectations( tested )
        {
            {
                tested.workWith();
                result = account;

                tested.save( ( List<CompanyBankAccount> ) any );

                authBy.finish();
                result = accounts;
                result = details1;
                result = details2;
                result = details3;
            }
        };

        tested.execute();

        new Verifications()
        {
            {
                List<CompanyBankAccount> bas;
                tested.save( bas = withCapture() );

                assertWithMessage( "List of company bank accounts" )
                        .that( bas )
                        .isNotNull();

                assertWithMessage( "Number of company bank accounts stored" )
                        .that( bas )
                        .hasSize( 2 );
            }
        };
    }

    @Test
    public void execute_BankAccountsSuccessfullyStored_InclNotPublic()
    {
        accounts.get( 2 ).setPublic( Boolean.FALSE );
        new Expectations( tested )
        {
            {
                tested.workWith();
                result = account;

                tested.save( ( List<CompanyBankAccount> ) any );

                authBy.finish();
                result = accounts;
                result = details1;
                result = details2;
                result = details3;
            }
        };

        tested.execute();

        new Verifications()
        {
            {
                List<CompanyBankAccount> bas;
                tested.save( bas = withCapture() );

                assertWithMessage( "List of company bank accounts" )
                        .that( bas )
                        .isNotNull();

                assertWithMessage( "Number of company bank accounts stored" )
                        .that( bas )
                        .hasSize( 3 );
            }
        };
    }

    @Test
    public void execute_Idempotent()
    {
        CompanyBankAccount bankAccount = new CompanyBankAccount( codeBook );
        bankAccount.setIban( "GB77REVO00996922793674" );
        bankAccount.setCurrency( "EUR" );

        List<CompanyBankAccount> existing = new ArrayList<>();
        existing.add( bankAccount );

        new Expectations( tested )
        {
            {
                tested.workWith();
                result = account;

                config.getBankAccounts( REVOLUT_BANK_EU_CODE );
                result = existing;

                tested.save( ( List<CompanyBankAccount> ) any );

                authBy.finish();
                result = accounts;
                result = details1;
                result = details2;
                result = details3;
            }
        };

        tested.execute();

        new Verifications()
        {
            {
                List<CompanyBankAccount> bas;
                tested.save( bas = withCapture() );

                assertWithMessage( "List of company bank accounts" )
                        .that( bas )
                        .isNotNull();

                assertWithMessage( "Number of company bank accounts stored" )
                        .that( bas )
                        .hasSize( 3 );

                for ( CompanyBankAccount next : bas )
                {
                    if ( "EUR".equals( next.getCurrency() ) )
                    {
                        assertWithMessage( "Changed IBAN" )
                                .that( next.getIBAN() )
                                .isEqualTo( nl.garvelink.iban.IBAN.valueOf( IBAN ) );

                        assertWithMessage( "Existing company bank account" )
                                .that( next )
                                .isSameInstanceAs( bankAccount );
                    }
                }
            }
        };
    }

    @Test
    public void execute_RevolutAccountsEmpty()
    {
        accounts.clear();
        new Expectations( tested )
        {
            {
                tested.workWith();
                result = account;

                authBy.finish();
                result = accounts;
                result = details1;
                result = details2;
                result = details3;
            }
        };

        tested.execute();

        new Verifications()
        {
            {
                tested.save( ( List<CompanyBankAccount> ) any );
                times = 0;
            }
        };
    }

    public static class ListArray
            extends ArrayList<Account>
    {
        private static final long serialVersionUID = 1L;
    }

    public static class DetailsArray
            extends ArrayList<AccountBankDetailsItem>
    {
        private static final long serialVersionUID = 1L;
    }
}