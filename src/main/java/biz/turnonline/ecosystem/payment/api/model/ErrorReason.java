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

/**
 * ErrorReason
 */
public class ErrorReason
{
    @JsonProperty( "domain" )
    private String domain = null;

    @JsonProperty( "message" )
    private String message = null;

    @JsonProperty( "reason" )
    private String reason = null;

    public ErrorReason domain( String domain )
    {
        this.domain = domain;
        return this;
    }

    /**
     * The overall scope of the error message.
     **/
    @JsonProperty( "domain" )
    @NotNull
    public String getDomain()
    {
        return domain;
    }

    public void setDomain( String domain )
    {
        this.domain = domain;
    }

    public ErrorReason message( String message )
    {
        this.message = message;
        return this;
    }

    /**
     * The detailed error message.
     **/
    @JsonProperty( "message" )
    @NotNull
    public String getMessage()
    {
        return message;
    }

    public void setMessage( String message )
    {
        this.message = message;
    }

    public ErrorReason reason( String reason )
    {
        this.reason = reason;
        return this;
    }

    /**
     * The error reason (error key).
     **/
    @JsonProperty( "reason" )
    @NotNull
    public String getReason()
    {
        return reason;
    }

    public void setReason( String reason )
    {
        this.reason = reason;
    }

    @Override
    public String toString()
    {
        return "class ErrorReason {\n" +
                "    domain: " + toIndentedString( domain ) + "\n" +
                "    message: " + toIndentedString( message ) + "\n" +
                "    reason: " + toIndentedString( reason ) + "\n" +
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
