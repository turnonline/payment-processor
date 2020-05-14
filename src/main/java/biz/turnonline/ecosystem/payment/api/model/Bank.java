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

import java.util.Objects;

/**
 * Bank
 */
public class Bank
{
    @JsonProperty( "code" )
    private String code = null;

    @JsonProperty( "label" )
    private String label = null;

    @JsonProperty( "country" )
    private String country = null;

    public Bank code( String code )
    {
        this.code = code;
        return this;
    }

    /**
     * The bank identified by a bank code, taken from the code-book. Taken from valid IBAN while set.
     **/
    @JsonProperty( "code" )
    public String getCode()
    {
        return code;
    }

    public void setCode( String code )
    {
        this.code = code;
    }

    public Bank label( String label )
    {
        this.label = label;
        return this;
    }

    /**
     * The localized name of the bank, taken from the code-book and based on either default or specified language.   The value will be managed by the service once Accept-Language header will be provided while bank account getting.
     **/
    @JsonProperty( "label" )
    public String getLabel()
    {
        return label;
    }

    public void setLabel( String label )
    {
        this.label = label;
    }

    public Bank country( String country )
    {
        this.country = country;
        return this;
    }

    /**
     * The country of the bank domicile, taken from valid IBAN while set. The ISO 3166 alpha-2 country code. It’s case insensitive.
     **/
    @JsonProperty( "country" )
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
        if ( !( o instanceof Bank ) ) return false;
        Bank bank = ( Bank ) o;
        return Objects.equals( code, bank.code ) &&
                Objects.equals( country, bank.country );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( code, country );
    }

    @Override
    public String toString()
    {
        return "class Bank {\n" +
                "    code: " + toIndentedString( code ) + "\n" +
                "    label: " + toIndentedString( label ) + "\n" +
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

