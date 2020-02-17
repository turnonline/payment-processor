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

import java.util.Date;
import java.util.Objects;

/**
 * Certificate metadata, not a sensitive data.
 */
public class Certificate
{
    @JsonProperty( "accessAuthorised" )
    private Boolean accessAuthorised = null;

    @JsonProperty( "authorisedOn" )
    private Date authorisedOn = null;

    @JsonProperty( "clientId" )
    private String clientId = null;

    @JsonProperty( "keyName" )
    private String keyName = null;

    public Certificate accessAuthorised( Boolean accessAuthorised )
    {
        this.accessAuthorised = accessAuthorised;
        return this;
    }

    /**
     * Boolean indication whether access to bank API has been already granted.
     **/
    @JsonProperty( "accessAuthorised" )
    public Boolean isAccessAuthorised()
    {
        return accessAuthorised;
    }

    public void setAccessAuthorised( Boolean accessAuthorised )
    {
        this.accessAuthorised = accessAuthorised;
    }

    public Certificate authorisedOn( Date authorisedOn )
    {
        this.authorisedOn = authorisedOn;
        return this;
    }

    /**
     * Date and time when access to bank API was granted.
     **/
    @JsonProperty( "authorisedOn" )
    public Date getAuthorisedOn()
    {
        return authorisedOn;
    }

    public void setAuthorisedOn( Date authorisedOn )
    {
        this.authorisedOn = authorisedOn;
    }

    public Certificate clientId( String clientId )
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

    public Certificate keyName( String keyName )
    {
        this.keyName = keyName;
        return this;
    }

    /**
     * Secret manager private key name configurable by the client, or default one (default value depends on concrete bank).
     **/
    @JsonProperty( "keyName" )
    public String getKeyName()
    {
        return keyName;
    }

    public void setKeyName( String keyName )
    {
        this.keyName = keyName;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( !( o instanceof Certificate ) ) return false;
        Certificate that = ( Certificate ) o;
        return Objects.equals( clientId, that.clientId );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( clientId );
    }

    @Override
    public String toString()
    {
        return "class Certificate {\n" +
                "    accessAuthorised: " + toIndentedString( accessAuthorised ) + "\n" +
                "    authorisedOn: " + toIndentedString( authorisedOn ) + "\n" +
                "    clientId: " + toIndentedString( clientId ) + "\n" +
                "    keyName: " + toIndentedString( keyName ) + "\n" +
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

