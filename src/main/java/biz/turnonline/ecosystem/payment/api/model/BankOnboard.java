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
 * BankOnboard
 */
public class BankOnboard
{
    @JsonProperty( "clientId" )
    private String clientId = null;

    public BankOnboard clientId( String clientId )
    {
        this.clientId = clientId;
        return this;
    }

    /**
     * Identification of the app within the bank.
     **/
    @JsonProperty( "clientId" )
    public String getClientId()
    {
        return clientId;
    }

    public void setClientId( String clientId )
    {
        this.clientId = clientId;
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
        BankOnboard bankOnboard = ( BankOnboard ) o;
        return Objects.equals( this.clientId, bankOnboard.clientId );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( clientId );
    }

    @Override
    public String toString()
    {
        return "class BankOnboard {\n" +
                "    clientId: " + toIndentedString( clientId ) + "\n" +
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
