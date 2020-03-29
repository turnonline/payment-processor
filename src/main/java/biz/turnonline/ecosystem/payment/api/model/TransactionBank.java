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
 * The bank account associated with this transaction.
 */
public class TransactionBank
{
    @JsonProperty( "code" )
    private String code;

    @JsonProperty( "iban" )
    private String iban;

    public TransactionBank code( String code )
    {
        this.code = code;
        return this;
    }

    /**
     * The bank identified by a bank code.
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

    public TransactionBank iban( String iban )
    {
        this.iban = iban;
        return this;
    }

    /**
     * The international bank account number.
     **/
    @JsonProperty( "iban" )
    public String getIban()
    {
        return iban;
    }

    public void setIban( String iban )
    {
        this.iban = iban;
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
        TransactionBank transactionBank = ( TransactionBank ) o;
        return Objects.equals( this.code, transactionBank.code ) &&
                Objects.equals( this.iban, transactionBank.iban );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( code, iban );
    }

    @Override
    public String toString()
    {
        return "class TransactionBank {\n" +
                "    code: " + toIndentedString( code ) + "\n" +
                "    iban: " + toIndentedString( iban ) + "\n" +
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

