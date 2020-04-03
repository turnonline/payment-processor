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

package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.payment.service.model.BankCode;
import org.ctoolkit.agent.service.impl.ImportTask;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

/**
 * {@link CodeBookBean} unit testing against emulated (local) App Engine services including datastore.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class CodeBookBeanDbTest
        extends BackendServiceTestCase
{
    private static final Locale DEFAULT_LOCALE = new Locale( "en" );

    private static final String DEFAULT_DOMICILE = "SK";

    @Inject
    private CodeBook tested;

    @BeforeMethod
    public void before()
    {
        // import bank code code-book
        ImportTask task = new ImportTask( "/dataset/changeset_00001.xml" );
        task.run();

        task = new ImportTask( "/testdataset/changeset_local-account.xml" );
        task.run();
    }

    @Test
    public void getBankCodes()
    {
        // en-SK
        Map<String, BankCode> bankCodes = tested.getBankCodes( DEFAULT_LOCALE, DEFAULT_DOMICILE );
        assertThat( bankCodes ).isNotNull();
        assertThat( bankCodes ).hasSize( 36 );

        BankCode bankCode = bankCodes.get( "0200" );
        assertThat( bankCode ).isNotNull();
        assertThat( bankCode.getLocale() ).isEqualTo( "en" );
        assertThat( bankCode.getCountry() ).isEqualTo( DEFAULT_DOMICILE );

        // cached value retrieval
        bankCodes = tested.getBankCodes( DEFAULT_LOCALE, DEFAULT_DOMICILE );
        assertThat( bankCodes ).isNotNull();
        assertThat( bankCodes ).hasSize( 36 );

        // default locale and domicile taken from the account
        bankCodes = tested.getBankCodes( null, null );
        assertThat( bankCodes ).isNotNull();
        assertThat( bankCodes ).hasSize( 36 );

        // cs-SK
        bankCodes = tested.getBankCodes( new Locale( "cs" ), "SK" );
        assertThat( bankCodes ).isNotNull();
        assertThat( bankCodes ).hasSize( 36 );

        bankCode = bankCodes.get( "0200" );
        assertThat( bankCode ).isNotNull();
        assertThat( bankCode.getLocale() ).isEqualTo( "cs" );
        assertThat( bankCode.getCountry() ).isEqualTo( "SK" );

        // sk-SK
        bankCodes = tested.getBankCodes( new Locale( "sk" ), "SK" );
        assertThat( bankCodes ).isNotNull();
        assertThat( bankCodes ).hasSize( 36 );

        bankCode = bankCodes.get( "0200" );
        assertThat( bankCode ).isNotNull();
        assertThat( bankCode.getLocale() ).isEqualTo( "sk" );
        assertThat( bankCode.getCountry() ).isEqualTo( "SK" );

        // en-CZ
        bankCodes = tested.getBankCodes( new Locale( "en" ), "CZ" );
        assertThat( bankCodes ).isNotNull();
        assertThat( bankCodes ).hasSize( 53 );

        bankCode = bankCodes.get( "0100" );
        assertThat( bankCode ).isNotNull();
        assertThat( bankCode.getLocale() ).isEqualTo( "en" );
        assertThat( bankCode.getCountry() ).isEqualTo( "CZ" );

        // cs-CZ
        bankCodes = tested.getBankCodes( new Locale( "cs" ), "CZ" );
        assertThat( bankCodes ).isNotNull();
        assertThat( bankCodes ).hasSize( 53 );

        bankCode = bankCodes.get( "0100" );
        assertThat( bankCode ).isNotNull();
        assertThat( bankCode.getLocale() ).isEqualTo( "cs" );
        assertThat( bankCode.getCountry() ).isEqualTo( "CZ" );

        // sk-CZ
        bankCodes = tested.getBankCodes( new Locale( "sk" ), "CZ" );
        assertThat( bankCodes ).isNotNull();
        assertThat( bankCodes ).hasSize( 53 );

        bankCode = bankCodes.get( "0100" );
        assertThat( bankCode ).isNotNull();
        assertThat( bankCode.getLocale() ).isEqualTo( "sk" );
        assertThat( bankCode.getCountry() ).isEqualTo( "CZ" );
    }

    @Test
    public void singleBankCodeRetrieval()
    {
        BankCode bankCode = tested.getBankCode( "1111", new Locale( "en" ), "SK" );
        assertThat( bankCode ).isNotNull();
        assertThat( bankCode.getLocale() ).isEqualTo( "en" );
        assertThat( bankCode.getCountry() ).isEqualTo( "SK" );

        // testing caching
        BankCode cached = tested.getBankCode( "1111", new Locale( "en" ), "SK" );
        assertThat( cached ).isNotNull();
        assertThat( cached.getLocale() ).isEqualTo( "en" );
        assertThat( cached.getCountry() ).isEqualTo( "SK" );
    }

    @Test
    public void singleBankCodeRetrieval_WithDefaultLocaleDomicile()
    {
        BankCode bankCode = tested.getBankCode( "5600", null, null );
        assertThat( bankCode ).isNotNull();
        // account locale is 'en'
        assertThat( bankCode.getLocale() ).isEqualTo( "en" );
        // account business domicile is 'SK'
        assertThat( bankCode.getCountry() ).isEqualTo( "SK" );

        // testing caching
        BankCode cached = tested.getBankCode( "5600", null, null );
        assertThat( cached ).isNotNull();
        assertThat( cached.getLocale() ).isEqualTo( "en" );
        assertThat( cached.getCountry() ).isEqualTo( "SK" );
    }

    @Test
    public void singleBankCodeRetrieval_NotFound()
    {
        BankCode bankCode = tested.getBankCode( "0987", null, null );
        assertThat( bankCode ).isNull();
    }
}