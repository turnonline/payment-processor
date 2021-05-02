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

package biz.turnonline.ecosystem.payment.service.model;

import com.google.common.base.MoreObjects;

import java.io.Serializable;
import java.util.Date;

/**
 * The transaction exchange rate.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class ExchangeRate
        implements Serializable
{
    private static final long serialVersionUID = -7240028136641175644L;

    private ExchangeAmount from;

    private ExchangeAmount to;

    private Double rate;

    private ExchangeAmount fee;

    private Date rateDate;

    public ExchangeRate from( ExchangeAmount from )
    {

        this.from = from;
        return this;
    }

    /**
     * The amount and currency that exchanged from.
     **/
    public ExchangeAmount getFrom()
    {
        return from;
    }

    public ExchangeRate to( ExchangeAmount to )
    {

        this.to = to;
        return this;
    }

    /**
     * The amount and currency that exchanged to.
     **/
    public ExchangeAmount getTo()
    {
        return to;
    }

    public ExchangeRate rate( Double rate )
    {

        this.rate = rate;
        return this;
    }

    /**
     * The exchange rate for this transaction.
     */
    public Double getRate()
    {
        return rate;
    }

    public ExchangeRate fee( ExchangeAmount fee )
    {

        this.fee = fee;
        return this;
    }

    /**
     * The fee for this transaction.
     **/
    public ExchangeAmount getFee()
    {
        return fee;
    }

    public ExchangeRate rateDate( Date rateDate )
    {

        this.rateDate = rateDate;
        return this;
    }

    /**
     * The date of the proposed exchange rate.
     **/
    public Date getRateDate()
    {
        return rateDate;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
                .add( "from", from )
                .add( "to", to )
                .add( "rate", rate )
                .add( "fee", fee )
                .add( "rateDate", rateDate )
                .toString();
    }
}
