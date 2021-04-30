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

import biz.turnonline.ecosystem.payment.service.CodeBook;
import mockit.Injectable;
import mockit.Tested;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.testng.annotations.Test;

import static biz.turnonline.ecosystem.payment.service.PaymentConfig.REVOLUT_BANK_EU_CODE;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * {@link CompanyBankAccount} unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class BankAccountTest
{
    @Tested
    private CompanyBankAccount tested;

    @Injectable
    private CodeBook codeBook;

    @Test
    public void setIban_DE_Societe_Generale()
    {
        tested.setIban( "DE75 5121 0800 1245126199" );

        assertWithMessage( "IBAN country" )
                .that( tested.getCountry() )
                .isEqualTo( "DE" );

        assertWithMessage( "IBAN bank code" )
                .that( tested.getBankCode() )
                .isEqualTo( "51210800" );

        assertWithMessage( "IBAN branch" )
                .that( tested.getBranch() )
                .isNull();

        assertWithMessage( "Formatted IBAN" )
                .that( tested.getIbanString() )
                .isEqualTo( "DE75 5121 0800 1245 1261 99" );
    }

    @Test
    public void setIban_SK_CSOB()
    {
        tested.setIban( "SK897500 00000000 1234 5671" );

        assertWithMessage( "IBAN country" )
                .that( tested.getCountry() )
                .isEqualTo( "SK" );

        assertWithMessage( "IBAN bank code" )
                .that( tested.getBankCode() )
                .isEqualTo( "7500" );

        assertWithMessage( "IBAN branch" )
                .that( tested.getBranch() )
                .isNull();

        assertWithMessage( "Formatted IBAN" )
                .that( tested.getIbanString() )
                .isEqualTo( "SK89 7500 0000 0000 1234 5671" );
    }

    @Test
    public void setIban_GB_Revolut()
    {
        tested.setIban( "GB35 REVO 00996912346754" );

        assertWithMessage( "IBAN country" )
                .that( tested.getCountry() )
                .isEqualTo( "GB" );

        assertWithMessage( "IBAN bank code" )
                .that( tested.getBankCode() )
                .isEqualTo( "REVO" );

        assertWithMessage( "IBAN branch" )
                .that( tested.getBranch() )
                .isEqualTo( "009969" );

        assertWithMessage( "Formatted IBAN" )
                .that( tested.getIbanString() )
                .isEqualTo( "GB35 REVO 0099 6912 3467 54" );
    }

    @Test
    public void setIban_LT_Revolut()
    {
        Iban iban = new Iban.Builder()
                .countryCode( CountryCode.LT )
                .bankCode( REVOLUT_BANK_EU_CODE )
                .buildRandom();

        tested.setIban( iban.toString() );

        assertWithMessage( "IBAN country" )
                .that( tested.getCountry() )
                .isEqualTo( "LT" );

        assertWithMessage( "IBAN bank code" )
                .that( tested.getBankCode() )
                .isEqualTo( REVOLUT_BANK_EU_CODE );

        assertWithMessage( "IBAN branch" )
                .that( tested.getBranch() )
                .isNull();

        assertWithMessage( "Formatted IBAN" )
                .that( tested.getIbanString() )
                .isEqualTo( iban.toFormattedString() );
    }

    @Test( expectedExceptions = IllegalArgumentException.class )
    public void setIban_Invalid()
    {
        tested.setIban( "LU000019400644750000" );
    }
}