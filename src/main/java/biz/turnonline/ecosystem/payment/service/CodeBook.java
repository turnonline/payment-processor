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
import biz.turnonline.ecosystem.payment.service.model.CodeBookItem;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

/**
 * The product billing related code-books.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 * @see CodeBookItem
 */
public interface CodeBook
{
    /**
     * Returns the all bank codes available for specified country (defined by country).
     *
     * @param account the authenticated account as a source of default locale and country if missing
     * @param locale  the optional language to prefer in results
     * @param country the optional ISO 3166 alpha-2 country code that represents a target country
     * @return the all bank codes for specific country
     */
    Map<String, BankCode> getBankCodes( @Nonnull LocalAccount account,
                                        @Nullable Locale locale,
                                        @Nullable String country );

    /**
     * Returns the specified bank code for given country (defined by country).
     *
     * @param account the authenticated account as a source of default locale and country if missing
     * @param code    the numeric bank code assigned to concrete bank to be retrieved
     * @param locale  the optional language to prefer in results
     * @param country the optional ISO 3166 alpha-2 country code that represents a target country
     * @return the requested bank code
     */
    BankCode getBankCode( @Nonnull LocalAccount account,
                          @Nonnull String code,
                          @Nullable Locale locale,
                          @Nullable String country );

}
