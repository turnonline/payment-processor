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
 * Transaction that represents either a credit or debit operation.
 **/
public class Transaction
{
    private Double amount;

    private boolean credit;

    private String currency;

    private String type;

    private String key;

    private String reference;

    private Date completedAt;

    /**
     * The transaction amount absolute value.
     **/
    public Transaction amount( Double amount )
    {
        this.amount = amount;
        return this;
    }

    @JsonProperty( "amount" )
    public Double getAmount()
    {
        return amount;
    }

    public void setAmount( Double amount )
    {
        this.amount = amount;
    }

    /**
     * The boolean indicating whether the payment has positive or negative amount; true - credit, false - debit.
     **/
    public Transaction credit( boolean credit )
    {
        this.credit = credit;
        return this;
    }

    @JsonProperty( "credit" )
    public boolean getCredit()
    {
        return credit;
    }

    public void setCredit( boolean credit )
    {
        this.credit = credit;
    }

    /**
     * The payment currency alphabetic code based on the ISO 4217.
     **/
    public Transaction currency( String currency )
    {
        this.currency = currency;
        return this;
    }

    @JsonProperty( "currency" )
    public String getCurrency()
    {
        return currency;
    }

    public void setCurrency( String currency )
    {
        this.currency = currency;
    }

    /**
     * The payment type that has been used to make this payment.
     **/
    public Transaction type( String type )
    {
        this.type = type;
        return this;
    }

    @JsonProperty( "type" )
    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    /**
     * The unique payment identification related to the associated invoice or bill.
     **/
    public Transaction key( String key )
    {
        this.key = key;
        return this;
    }

    @JsonProperty( "key" )
    public String getKey()
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    /**
     * A user provided payment reference.
     */
    public Transaction reference( String reference )
    {
        this.reference = reference;
        return this;
    }

    @JsonProperty( "reference" )
    public String getReference()
    {
        return reference;
    }

    public void setReference( String reference )
    {
        this.reference = reference;
    }

    /**
     * The date when the transaction was completed.
     */
    public Transaction completedAt( Date completedAt )
    {
        this.completedAt = completedAt;
        return this;
    }

    @JsonProperty( "completedAt" )
    public Date getCompletedAt()
    {
        return completedAt;
    }

    public void setCompletedAt( Date completedAt )
    {
        this.completedAt = completedAt;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( !( o instanceof Transaction ) ) return false;
        Transaction that = ( Transaction ) o;
        return credit == that.credit &&
                Objects.equals( amount, that.amount ) &&
                Objects.equals( currency, that.currency ) &&
                Objects.equals( type, that.type ) &&
                Objects.equals( key, that.key );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( amount, credit, currency, type, key );
    }

    @Override
    public String toString()
    {
        return "class Transaction {\n" +
                "    amount: " + toIndentedString( amount ) + "\n" +
                "    credit: " + toIndentedString( credit ) + "\n" +
                "    currency: " + toIndentedString( currency ) + "\n" +
                "    form: " + toIndentedString( type ) + "\n" +
                "    key: " + toIndentedString( key ) + "\n" +
                "    reference: " + toIndentedString( reference ) + "\n" +
                "    completedAt: " + toIndentedString( completedAt ) + "\n" +
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

