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

    @Inject
    CodeBookBean( Cache cache )
    {
        this.cache = cache;
    }

    @Override
    public Map<String, BankCode> getBankCodes( @Nonnull LocalAccount account,
                                               @Nullable Locale locale,
                                               @Nullable String country )
    {
        checkNotNull( account );

        locale = account.getLocale( locale );
        country = country == null ? account.getDomicile().name() : country;

        String key = cacheKey( locale, country, null );
        if ( cache.containsKey( key ) )
        {
            //noinspection unchecked
            return ( Map<String, BankCode> ) cache.get( key );
        }

        String language = locale.getLanguage();

        Query<BankCode> query = ofy().transactionless().load().type( BankCode.class )
                .filter( "locale", language.toLowerCase() )
                .filter( "country", country )
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
    }

    @Override
    public BankCode getBankCode( @Nonnull LocalAccount account,
                                 @Nonnull String code,
                                 @Nullable Locale locale,
                                 @Nullable String country )
    {
        checkNotNull( account );
        checkNotNull( code );

        locale = account.getLocale( locale );
        country = country == null ? account.getDomicile().name() : country;
        String key = cacheKey( locale, country, code );

        if ( cache.containsKey( key ) )
        {
            return ( BankCode ) cache.get( key );
        }

        String language = locale.getLanguage();

        Query<BankCode> query = ofy().transactionless().load().type( BankCode.class )
                .filter( "code", code )
                .filter( "locale", language.toLowerCase() )
                .filter( "country", country );

        BankCode bankCode = query.first().now();
        if ( bankCode != null )
        {
            cache.put( key, bankCode );
        }
        return bankCode;
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
}
