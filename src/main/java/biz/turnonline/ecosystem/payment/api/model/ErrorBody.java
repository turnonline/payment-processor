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
import java.util.List;
import java.util.Objects;

/**
 * ErrorBody
 */
public class ErrorBody
{
    @JsonProperty( "code" )
    private Integer code = null;

    @JsonProperty( "errors" )
    private List<ErrorReason> errors;

    @JsonProperty( "message" )
    private String message = null;

    public ErrorBody code( Integer code )
    {
        this.code = code;
        return this;
    }

    /**
     * The HTTP status code.
     **/
    @JsonProperty( "code" )
    @NotNull
    public Integer getCode()
    {
        return code;
    }

    public void setCode( Integer code )
    {
        this.code = code;
    }

    public ErrorBody errors( List<ErrorReason> errors )
    {
        this.errors = errors;
        return this;
    }

    public ErrorBody addErrorsItem( ErrorReason errorsItem )
    {
        this.errors.add( errorsItem );
        return this;
    }

    /**
     * Get errors
     **/
    @JsonProperty( "errors" )
    @NotNull
    public List<ErrorReason> getErrors()
    {
        return errors;
    }

    public void setErrors( List<ErrorReason> errors )
    {
        this.errors = errors;
    }

    public ErrorBody message( String message )
    {
        this.message = message;
        return this;
    }

    /**
     * The error message.
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
        ErrorBody errorBody = ( ErrorBody ) o;
        return Objects.equals( this.code, errorBody.code ) &&
                Objects.equals( this.errors, errorBody.errors ) &&
                Objects.equals( this.message, errorBody.message );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( code, errors, message );
    }

    @Override
    public String toString()
    {
        return "class ErrorBody {\n" +
                "    code: " + toIndentedString( code ) + "\n" +
                "    errors: " + toIndentedString( errors ) + "\n" +
                "    message: " + toIndentedString( message ) + "\n" +
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
