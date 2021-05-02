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

/**
 * Exchange amount of the transaction.
 */
public class ExchangeAmount
        implements Serializable
{
    private static final long serialVersionUID = -5812572907379838078L;

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
    public Double getAmount()
    {
        return amount;
    }

    public ExchangeAmount currency( String currency )
    {

        this.currency = currency;
        return this;
    }

    /**
     * IOS 4217 currency code.
     **/
    public String getCurrency()
    {
        return currency;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
                .add( "amount", amount )
                .add( "currency", currency )
                .toString();
    }
}

