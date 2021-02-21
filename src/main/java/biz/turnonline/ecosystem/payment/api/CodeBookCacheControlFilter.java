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

import org.ctoolkit.services.endpoints.CacheControlFilter;

import javax.inject.Singleton;

import static biz.turnonline.ecosystem.payment.api.EndpointsApiProfile.API_NAME;
import static biz.turnonline.ecosystem.payment.api.EndpointsApiProfile.CURRENT_VERSION;


/**
 * The REST API 'Cache-Control' filter for all 'codebook'.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
public class CodeBookCacheControlFilter
        extends CacheControlFilter
{
    public static final String FILTER_PATH = "/api/" + API_NAME + "/"
            + CURRENT_VERSION + "/codebook/*";

    @Override
    public Integer getMaxAge()
    {
        // 30 days in seconds
        return 2592000;
    }
}
