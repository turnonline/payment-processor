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
 * The bill or invoice document settled by associated transaction.
 */
public class Bill
{
    @JsonProperty( "id" )
    private Long id;

    @JsonProperty( "invoiceId" )
    private Long invoiceId;

    @JsonProperty( "orderId" )
    private Long orderId;

    public Bill id( Long id )
    {
        this.id = id;
        return this;
    }

    /**
     * The unique identification of the bill (cash register document).
     **/
    @JsonProperty( "id" )
    public Long getId()
    {
        return id;
    }

    public void setId( Long id )
    {
        this.id = id;
    }

    public Bill invoiceId( Long invoiceId )
    {
        this.invoiceId = invoiceId;
        return this;
    }

    /**
     * The invoice identification, unique only for specified order.
     **/
    @JsonProperty( "invoiceId" )
    public Long getInvoiceId()
    {
        return invoiceId;
    }

    public void setInvoiceId( Long invoiceId )
    {
        this.invoiceId = invoiceId;
    }

    public Bill orderId( Long orderId )
    {
        this.orderId = orderId;
        return this;
    }

    /**
     * The unique identification of the order associated with the settled invoice.
     **/
    @JsonProperty( "orderId" )
    public Long getOrderId()
    {
        return orderId;
    }

    public void setOrderId( Long orderId )
    {
        this.orderId = orderId;
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
        Bill bill = ( Bill ) o;
        return Objects.equals( this.id, bill.id ) &&
                Objects.equals( this.invoiceId, bill.invoiceId ) &&
                Objects.equals( this.orderId, bill.orderId );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( id, invoiceId, orderId );
    }


    @Override
    public String toString()
    {
        return "class Bill {\n" +
                "    id: " + toIndentedString( id ) + "\n" +
                "    invoiceId: " + toIndentedString( invoiceId ) + "\n" +
                "    orderId: " + toIndentedString( orderId ) + "\n" +
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

