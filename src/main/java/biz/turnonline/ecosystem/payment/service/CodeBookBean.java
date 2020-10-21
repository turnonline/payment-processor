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
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import com.googlecode.objectify.cmd.Query;
import net.sf.jsr107cache.Cache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static biz.turnonline.ecosystem.payment.service.MicroserviceModule.API_PREFIX;
import static biz.turnonline.ecosystem.payment.service.PaymentConfig.REVOLUT_BANK_CODE;
import static biz.turnonline.ecosystem.payment.service.model.LocalAccount.DEFAULT_DOMICILE;
import static biz.turnonline.ecosystem.payment.service.model.LocalAccount.DEFAULT_LOCALE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Code-book service implementation.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
class CodeBookBean
        implements CodeBook
{
    private static final String CACHE_PREFIX = API_PREFIX + "::Code-book::";

    private final Cache cache;

    private final LocalAccountProvider lap;

    @Inject
    CodeBookBean( Cache cache, LocalAccountProvider lap )
    {
        this.cache = cache;
        this.lap = lap;
    }

    @Override
    public Map<String, BankCode> getBankCodes( @Nullable Locale locale, @Nullable String country )
    {
        locale = getLocale( locale );
        String domicile = getDomicile( country );

        String key = cacheKey( locale, domicile, null );
        if ( cache.containsKey( key ) )
        {
            //noinspection unchecked
            return ( Map<String, BankCode> ) cache.get( key );
        }

        String language = locale.getLanguage();

        return ofy().transactionless( () -> {
            Query<BankCode> query = ofy().load()
                    .type( BankCode.class )
                    .filter( "locale", language.toLowerCase() )
                    .filter( "country", domicile )
                    .order( "code" );

            Map<String, BankCode> result = new TreeMap<>();

            for ( BankCode item : query.list() )
            {
                result.put( item.getCode(), item );
            }

            if ( !result.isEmpty() )
            {
                cache.put( key, result );
            }

            return result;
        } );
    }

    @Override
    public BankCode getBankCode( @Nonnull String code, @Nullable Locale locale, @Nullable String country )
    {
        checkNotNull( code );

        locale = getLocale( locale );
        String domicile;
        if ( country == null && REVOLUT_BANK_CODE.equals( code ) )
        {
            domicile = "GB";
        }
        else
        {
            domicile = getDomicile( country );
        }

        String key = cacheKey( locale, domicile, code );

        if ( cache.containsKey( key ) )
        {
            return ( BankCode ) cache.get( key );
        }

        String language = locale.getLanguage();

        return ofy().transactionless( () -> {
            Query<BankCode> query = ofy().load()
                    .type( BankCode.class )
                    .filter( "code", code )
                    .filter( "locale", language.toLowerCase() )
                    .filter( "country", domicile );

            BankCode bankCode = query.first().now();
            if ( bankCode != null )
            {
                cache.put( key, bankCode );
            }
            return bankCode;
        } );
    }

    private String cacheKey( @Nonnull Locale locale,
                             @Nonnull String domicile,
                             @Nullable String code )
    {
        StringBuilder builder = new StringBuilder( CACHE_PREFIX + BankCode.class.getSimpleName() );
        if ( code != null )
        {
            builder.append( "-" ).append( code );
        }
        builder.append( "-" );
        builder.append( locale.getLanguage().toLowerCase() );
        builder.append( "-" );
        builder.append( domicile );

        return builder.toString();
    }

    private Locale getLocale( @Nullable Locale locale )
    {
        if ( locale != null )
        {
            return locale;
        }

        LocalAccount localAccount = lap.get();
        if ( localAccount == null )
        {
            return DEFAULT_LOCALE;
        }
        return localAccount.getLocale();
    }

    private String getDomicile( String country )
    {
        if ( country != null )
        {
            return country;
        }

        LocalAccount localAccount = lap.get();
        if ( localAccount == null )
        {
            return DEFAULT_DOMICILE;
        }
        return localAccount.getDomicile().name();
    }
}
