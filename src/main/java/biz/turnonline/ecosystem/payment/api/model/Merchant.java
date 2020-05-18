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
 * The merchant details, available only for card payments.
 */
public class Merchant
{
    @JsonProperty( "category" )
    private String category;

    @JsonProperty( "city" )
    private String city;

    @JsonProperty( "name" )
    private String name;

    public Merchant category( String category )
    {
        this.category = category;
        return this;
    }

    /**
     * The merchant category.
     **/
    @JsonProperty( "category" )
    public String getCategory()
    {
        return category;
    }

    public void setCategory( String category )
    {
        this.category = category;
    }

    public Merchant city( String city )
    {
        this.city = city;
        return this;
    }

    /**
     * The merchant city.
     **/
    @JsonProperty( "city" )
    public String getCity()
    {
        return city;
    }

    public void setCity( String city )
    {
        this.city = city;
    }

    public Merchant name( String name )
    {
        this.name = name;
        return this;
    }

    /**
     * The merchant name.
     **/
    @JsonProperty( "name" )
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }
        Merchant merchant = ( Merchant ) o;
        return Objects.equals( this.name, merchant.name );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( name );
    }

    @Override
    public String toString()
    {
        return "class Merchant {\n" +
                "    category: " + toIndentedString( category ) + "\n" +
                "    city: " + toIndentedString( city ) + "\n" +
                "    name: " + toIndentedString( name ) + "\n" +
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

