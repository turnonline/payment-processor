/*
 * Copyright (c) 2021 TurnOnline.biz s.r.o. All Rights Reserved.
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

/**
 * Exchange amount of the transaction.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class ExchangeAmount
{
    private Double amount;

    private String currency;

    public ExchangeAmount amount( Double amount )
    {

        this.amount = amount;
        return this;
    }

    /**
     * The amount of the transaction.
     **/
    @JsonProperty( "amount" )
    public Double getAmount()
    {
        return amount;
    }

    public void setAmount( Double amount )
    {
        this.amount = amount;
    }

    public ExchangeAmount currency( String currency )
    {

        this.currency = currency;
        return this;
    }

    /**
     * IOS 4217 currency code.
     **/
    @JsonProperty( "currency" )
    public String getCurrency()
    {
        return currency;
    }

    public void setCurrency( String currency )
    {
        this.currency = currency;
    }

    @Override
    public String toString()
    {
        return "class ExchangeAmount {\n" +
                "    amount: " + toIndentedString( amount ) + "\n" +
                "    currency: " + toIndentedString( currency ) + "\n" +
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

