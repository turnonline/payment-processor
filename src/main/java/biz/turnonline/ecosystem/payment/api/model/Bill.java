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
 * Identification of the bill (receipt) or invoice document settled by associated transaction. Valid invoice identification includes order identification too.
 */
public class Bill
{
    @JsonProperty( "receipt" )
    private Long receipt;

    @JsonProperty( "invoice" )
    private Long invoice;

    @JsonProperty( "order" )
    private Long order;

    public Bill receipt( Long receipt )
    {
        this.receipt = receipt;
        return this;
    }

    /**
     * The unique identification of the bill (receipt) within Billing Processor service.
     **/
    @JsonProperty( "receipt" )
    public Long getReceipt()
    {
        return receipt;
    }

    public void setReceipt( Long receipt )
    {
        this.receipt = receipt;
    }

    public Bill invoice( Long invoice )
    {
        this.invoice = invoice;
        return this;
    }

    /**
     * The invoice identification, unique only for specified order.
     **/
    @JsonProperty( "invoice" )
    public Long getInvoice()
    {
        return invoice;
    }

    public void setInvoice( Long invoice )
    {
        this.invoice = invoice;
    }

    public Bill order( Long order )
    {
        this.order = order;
        return this;
    }

    /**
     * The unique identification of the order associated with the settled invoice.
     **/
    @JsonProperty( "order" )
    public Long getOrder()
    {
        return order;
    }

    public void setOrder( Long order )
    {
        this.order = order;
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
        return Objects.equals( this.receipt, bill.receipt ) &&
                Objects.equals( this.invoice, bill.invoice ) &&
                Objects.equals( this.order, bill.order );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( receipt, invoice, order );
    }


    @Override
    public String toString()
    {
        return "class Bill {\n" +
                "    receipt: " + toIndentedString( receipt ) + "\n" +
                "    invoice: " + toIndentedString( invoice ) + "\n" +
                "    order: " + toIndentedString( order ) + "\n" +
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

