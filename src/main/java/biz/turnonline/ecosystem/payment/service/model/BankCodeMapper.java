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

import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;

import javax.inject.Singleton;

/**
 * Mapper from {@link BankCode} to {@link biz.turnonline.ecosystem.payment.api.model.BankCode}.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
class BankCodeMapper
        extends CustomMapper<BankCode, biz.turnonline.ecosystem.payment.api.model.BankCode>
{
    @Override
    public void mapAtoB( BankCode source,
                         biz.turnonline.ecosystem.payment.api.model.BankCode bankCode,
                         MappingContext context )
    {
        bankCode.setCode( source.getCode() );
        bankCode.setLabel( source.getLabel() );
        bankCode.setLocale( source.getLocale() );
        bankCode.setCountry( source.getCountry() );
    }

    @Override
    public void mapBtoA( biz.turnonline.ecosystem.payment.api.model.BankCode source,
                         BankCode backend,
                         MappingContext context )
    {
        throw new UnsupportedOperationException();
    }
}
