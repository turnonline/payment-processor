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

import biz.turnonline.ecosystem.payment.api.ApiValidationException;
import biz.turnonline.ecosystem.payment.api.model.BankAccount;
import biz.turnonline.ecosystem.payment.service.CodeBook;
import biz.turnonline.ecosystem.payment.service.LocalAccountProvider;
import ma.glasnost.orika.MappingContext;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import org.testng.annotations.Test;

import java.util.Locale;

import static biz.turnonline.ecosystem.payment.service.BackendServiceTestCase.getFromFile;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * {@link BankAccountMapper} unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@SuppressWarnings( "ResultOfMethodCallIgnored" )
public class BankAccountMapperTest
{
    @Tested
    private BankAccountMapper tested;

    @Injectable
    private CodeBook codeBook;

    @Mocked
    private MappingContext context;

    private LocalAccount account = new LocalAccount( new LocalAccountProvider.Builder()
            .email( "my.account@turnonline.biz" )
            .identityId( "64HGtr6ks" ) );

    @Mocked
    private BankCode bankCode;

    @Test
    public void mapApiToBackend()
    {
        String code = "1100";
        String country = "SK";

        new Expectations()
        {
            {
                context.getProperty( LocalAccount.class );
                result = account;

                codeBook.getBankCode( account, code, ( Locale ) any, anyString );
                result = bankCode;

                bankCode.getCode();
                result = code;

                bankCode.getCountry();
                result = country;
            }
        };

        BankAccount api = getFromFile( "bank-account-1.json", BankAccount.class );
        // country will be populated by the value taken from the code-book
        assertThat( api.getBank().getCountry() ).isNull();

        CompanyBankAccount backend;
        backend = new CompanyBankAccount( codeBook );

        tested.mapBtoA( api, backend, context );

        assertThat( backend.getId() ).isNull();

        assertThat( backend.getBankCode() ).isEqualTo( code );
        assertThat( backend.getName() ).isEqualTo( api.getName() );
        assertThat( backend.getIbanString() ).isEqualTo( api.getIban() );
        assertThat( backend.getBic() ).isEqualTo( api.getBic() );
        assertThat( backend.getCurrency() ).isEqualTo( api.getCurrency() );

        assertThat( backend.getCountry() ).isNotNull();
        assertThat( backend.isPrimary() ).isFalse();
    }

    @Test( expectedExceptions = ApiValidationException.class )
    public void mapApiToBackend_CurrencyValidationFailure()
    {
        String code = "1100";
        new Expectations()
        {
            {
                context.getProperty( LocalAccount.class );
                result = account;

                codeBook.getBankCode( account, code, ( Locale ) any, anyString );
                result = bankCode;

                bankCode.getCode();
                result = code;
            }
        };

        BankAccount api = getFromFile( "bank-account-1.json", BankAccount.class );
        api.setCurrency( "EUR_INV" );

        CompanyBankAccount backend;
        backend = new CompanyBankAccount( codeBook );

        tested.mapBtoA( api, backend, context );
    }

    @Test( expectedExceptions = ApiValidationException.class )
    public void mapApiToBackend_BankCodeNotFound()
    {
        String code = "1100";
        new Expectations()
        {
            {
                context.getProperty( LocalAccount.class );
                result = account;

                codeBook.getBankCode( account, code, ( Locale ) any, anyString );
                result = null;
            }
        };

        BankAccount api = getFromFile( "bank-account-1.json", BankAccount.class );

        CompanyBankAccount backend;
        backend = new CompanyBankAccount( codeBook );

        tested.mapBtoA( api, backend, context );
    }

    @Test( expectedExceptions = ApiValidationException.class )
    public void mapApiToBackend_BankCodeIsMissing()
    {
        BankAccount api = getFromFile( "bank-account-1.json", BankAccount.class );
        api.getBank().setCode( null );

        CompanyBankAccount backend;
        backend = new CompanyBankAccount( codeBook );

        tested.mapBtoA( api, backend, context );
    }

    @Test
    public void mapBackendToApi()
    {
        long id = 1123L;
        String code = "1100";
        CompanyBankAccount backend = new CompanyBankAccount( codeBook );

        new Expectations( backend )
        {
            {
                backend.getId();
                result = id;

                backend.getName();
                result = "My first bank account";

                backend.getIbanString();
                result = "SK6711000000002289198742";

                backend.getBic();
                result = "TATRSKBX";

                backend.getCurrency();
                result = "EUR";

                backend.isPrimary();
                result = true;

                backend.getBankCode();
                result = code;

                backend.getLocalizedLabel( ( Locale ) any );
                result = "Tatra banka, a.s.";

                backend.getCountry();
                result = "SK";
            }
        };

        BankAccount api = new BankAccount();
        tested.mapAtoB( backend, api, context );

        assertThat( api.getId() ).isEqualTo( id );
        assertThat( api.getName() ).isNotNull();
        assertThat( api.getIban() ).isNotNull();
        assertThat( api.getBic() ).isNotNull();
        assertThat( api.getCurrency() ).isNotNull();
        assertThat( api.isPrimary() ).isTrue();

        assertThat( api.getBank().getCode() ).isEqualTo( code );
        assertThat( api.getBank().getLabel() ).isNotNull();
        assertThat( api.getBank().getCountry() ).isNotNull();
    }

    @Test
    public void mapApiToBackend_DefaultPrimaryBankAccount()
    {
        BankAccount source = new BankAccount();

        CompanyBankAccount backend;
        backend = new CompanyBankAccount( codeBook );

        tested.mapBtoA( source, backend, context );
        assertWithMessage( "Default primary bank account" )
                .that( backend.isPrimary() )
                .isFalse();
    }

    @Test
    public void mapApiToBackend_PreservePrimaryBankAccount()
    {
        BankAccount source = new BankAccount();

        CompanyBankAccount backend;
        backend = new CompanyBankAccount( codeBook );
        backend.setPrimary( true );

        tested.mapBtoA( source, backend, context );
        assertWithMessage( "Primary bank account" )
                .that( backend.isPrimary() )
                .isTrue();
    }

    @Test
    public void mapApiToBackend_SetPrimaryBankAccount()
    {
        BankAccount source = new BankAccount();
        source.setPrimary( true );

        CompanyBankAccount backend;
        backend = new CompanyBankAccount( codeBook );
        backend.setPrimary( false );

        tested.mapBtoA( source, backend, context );
        assertWithMessage( "Primary bank account" )
                .that( backend.isPrimary() )
                .isTrue();
    }
}