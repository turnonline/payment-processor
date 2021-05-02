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

import java.util.Date;

/**
 * The transaction exchange rate.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class ExchangeRate
{
    private ExchangeAmount fee;

    private ExchangeAmount from;

    private Double rate;

    private Date rateDate;

    private ExchangeAmount to;

    public ExchangeRate fee( ExchangeAmount fee )
    {
        this.fee = fee;
        return this;
    }

    /**
     * The fee for this transaction.
     **/
    @JsonProperty( "fee" )
    public ExchangeAmount getFee()
    {
        return fee;
    }

    public void setFee( ExchangeAmount fee )
    {
        this.fee = fee;
    }

    public ExchangeRate from( ExchangeAmount from )
    {
        this.from = from;
        return this;
    }

    /**
     * The amount and currency that exchanged from.
     **/
    @JsonProperty( "from" )
    public ExchangeAmount getFrom()
    {
        return from;
    }

    public void setFrom( ExchangeAmount from )
    {
        this.from = from;
    }

    public ExchangeRate rate( Double rate )
    {
        this.rate = rate;
        return this;
    }

    /**
     * The exchange rate for this transaction.
     **/
    @JsonProperty( "rate" )
    public Double getRate()
    {
        return rate;
    }

    public void setRate( Double rate )
    {
        this.rate = rate;
    }

    public ExchangeRate rateDate( Date rateDate )
    {
        this.rateDate = rateDate;
        return this;
    }

    /**
     * The date of the proposed exchange rate.
     **/
    @JsonProperty( "rateDate" )
    public Date getRateDate()
    {
        return rateDate;
    }

    public void setRateDate( Date rateDate )
    {
        this.rateDate = rateDate;
    }

    public ExchangeRate to( ExchangeAmount to )
    {
        this.to = to;
        return this;
    }

    /**
     * The amount and currency that exchanged to.
     **/
    @JsonProperty( "to" )
    public ExchangeAmount getTo()
    {
        return to;
    }

    public void setTo( ExchangeAmount to )
    {
        this.to = to;
    }

    @Override
    public String toString()
    {
        return "class ExchangeRate {\n" +
                "    fee: " + toIndentedString( fee ) + "\n" +
                "    from: " + toIndentedString( from ) + "\n" +
                "    rate: " + toIndentedString( rate ) + "\n" +
                "    rateDate: " + toIndentedString( rateDate ) + "\n" +
                "    to: " + toIndentedString( to ) + "\n" +
                "}";
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString( java.lang.Object o )
    {
        if ( o == null )
        {
            return "null";
        }
        return o.toString().replace( "\n", "\n    " );
    }
}
