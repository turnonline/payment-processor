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

import com.googlecode.objectify.annotation.Subclass;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The bank code definition as a code-book item.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Subclass( name = "BankCode", index = true )
public class BankCode
        extends CodeBookItem
{
    private static final long serialVersionUID = -8861313492104308022L;

    public BankCode()
    {
    }

    public BankCode( String code, String label, String locale, String domicile )
    {
        super.setCode( checkNotNull( code ) );
        super.setLabel( checkNotNull( label ) );
        super.setLocale( checkNotNull( locale ) );
        super.setCountry( checkNotNull( domicile ) );
    }

    @Override
    public String toString()
    {
        return "class BankCode {\n" +
                "    code: " + toIndentedString( getCode() ) + "\n" +
                "    country: " + toIndentedString( getCountry() ) + "\n" +
                "    label: " + toIndentedString( getLabel() ) + "\n" +
                "    locale: " + toIndentedString( getLocale() ) + "\n" +
                "    version: " + toIndentedString( getVersion() ) + "\n" +
                "}";
    }
}
