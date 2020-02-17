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
package biz.turnonline.ecosystem.payment.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * The brief description of the bank for concrete bank code.
 */
public class BankCode
{
    @JsonProperty( "code" )
    private String code = null;

    @JsonProperty( "label" )
    private String label = null;

    @JsonProperty( "locale" )
    private String locale = null;

    @JsonProperty( "country" )
    private String country = null;

    public BankCode code( String code )
    {
        this.code = code;
        return this;
    }

    /**
     * The bank code is a numeric code assigned by a central bank to concrete bank.
     **/
    @JsonProperty( "code" )
    @NotNull
    public String getCode()
    {
        return code;
    }

    public void setCode( String code )
    {
        this.code = code;
    }

    public BankCode label( String label )
    {
        this.label = label;
        return this;
    }

    /**
     * The localized name of the bank.
     **/
    @JsonProperty( "label" )
    @NotNull
    public String getLabel()
    {
        return label;
    }

    public void setLabel( String label )
    {
        this.label = label;
    }

    public BankCode locale( String locale )
    {
        this.locale = locale;
        return this;
    }

    /**
     * The label language. ISO 639 alpha-2 or alpha-3 language code.
     **/
    @JsonProperty( "locale" )
    @NotNull
    public String getLocale()
    {
        return locale;
    }

    public void setLocale( String locale )
    {
        this.locale = locale;
    }

    public BankCode country( String country )
    {
        this.country = country;
        return this;
    }

    /**
     * The ISO 3166 alpha-2 country code. The country of the bank code that belongs to.
     **/
    @JsonProperty( "country" )
    @NotNull
    public String getCountry()
    {
        return country;
    }

    public void setCountry( String country )
    {
        this.country = country;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( !( o instanceof BankCode ) ) return false;
        BankCode bankCode = ( BankCode ) o;
        return Objects.equals( code, bankCode.code ) &&
                Objects.equals( country, bankCode.country );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( code, country );
    }

    @Override
    public String toString()
    {
        return "class BankCode {\n" +
                "    code: " + toIndentedString( code ) + "\n" +
                "    label: " + toIndentedString( label ) + "\n" +
                "    locale: " + toIndentedString( locale ) + "\n" +
                "    country: " + toIndentedString( country ) + "\n" +
                "}";
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString( Object o )
    {
        if ( o == null )
        {
            return "null";
        }
        return o.toString().replace( "\n", "\n    " );
    }
}

