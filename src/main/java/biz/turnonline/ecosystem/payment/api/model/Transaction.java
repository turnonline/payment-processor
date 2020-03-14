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
 */
public class Transaction
{
    @JsonProperty( "amount" )
    private Double amount;

    @JsonProperty( "balance" )
    private Double balance;

    @JsonProperty( "bankAccount" )
    private BankAccount bankAccount;

    @JsonProperty( "bill" )
    private Bill bill;

    @JsonProperty( "completedAt" )
    private Date completedAt;

    @JsonProperty( "credit" )
    private Boolean credit;

    @JsonProperty( "currency" )
    private String currency;

    @JsonProperty( "reference" )
    private String reference;

    @JsonProperty( "key" )
    private String key;

    @JsonProperty( "status" )
    private String status;

    @JsonProperty( "type" )
    private String type;

    public Transaction amount( Double amount )
    {
        this.amount = amount;
        return this;
    }

    /**
     * The transaction amount absolute value.
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

    public Transaction balance( Double balance )
    {
        this.balance = balance;
        return this;
    }

    /**
     * The balance after the transaction.
     **/
    @JsonProperty( "balance" )
    public Double getBalance()
    {
        return balance;
    }

    public void setBalance( Double balance )
    {
        this.balance = balance;
    }

    public Transaction bankAccount( BankAccount bankAccount )
    {
        this.bankAccount = bankAccount;
        return this;
    }

    /**
     * The bank account associated with this transaction.
     **/
    @JsonProperty( "bankAccount" )
    public BankAccount getBankAccount()
    {
        return bankAccount;
    }

    public void setBankAccount( BankAccount bankAccount )
    {
        this.bankAccount = bankAccount;
    }

    public Transaction bill( Bill bill )
    {
        this.bill = bill;
        return this;
    }

    /**
     * The bill or invoice document settled by this transaction.
     **/
    @JsonProperty( "bill" )
    public Bill getBill()
    {
        return bill;
    }

    public void setBill( Bill bill )
    {
        this.bill = bill;
    }

    public Transaction completedAt( Date completedAt )
    {
        this.completedAt = completedAt;
        return this;
    }

    /**
     * The date when the transaction was completed.
     **/
    @JsonProperty( "completedAt" )
    public Date getCompletedAt()
    {
        return completedAt;
    }

    public void setCompletedAt( Date completedAt )
    {
        this.completedAt = completedAt;
    }

    public Transaction credit( Boolean credit )
    {
        this.credit = credit;
        return this;
    }

    /**
     * The boolean indicating whether the payment has positive or negative amount; true - credit, false - debit.
     **/
    @JsonProperty( "credit" )
    public Boolean isCredit()
    {
        return credit;
    }

    public void setCredit( Boolean credit )
    {
        this.credit = credit;
    }

    public Transaction currency( String currency )
    {
        this.currency = currency;
        return this;
    }

    /**
     * The payment currency alphabetic code based on the ISO 4217.
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

    public Transaction reference( String reference )
    {
        this.reference = reference;
        return this;
    }

    /**
     * A user provided payment reference.
     **/
    @JsonProperty( "reference" )
    public String getReference()
    {
        return reference;
    }

    public void setReference( String reference )
    {
        this.reference = reference;
    }

    public Transaction key( String key )
    {
        this.key = key;
        return this;
    }

    /**
     * The unique payment identification related to the associated bill.
     **/
    @JsonProperty( "key" )
    public String getKey()
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    public Transaction status( String status )
    {
        this.status = status;
        return this;
    }

    /**
     * The transaction status.
     */
    @JsonProperty( "status" )
    public String getStatus()
    {
        return status;
    }

    public void setStatus( String status )
    {
        this.status = status;
    }

    public Transaction type( String type )
    {
        this.type = type;
        return this;
    }

    /**
     * The payment type that has been used to make this payment.
     **/
    @JsonProperty( "type" )
    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
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
        Transaction transaction = ( Transaction ) o;
        return Objects.equals( this.amount, transaction.amount ) &&
                Objects.equals( this.bill, transaction.bill ) &&
                Objects.equals( this.credit, transaction.credit ) &&
                Objects.equals( this.currency, transaction.currency ) &&
                Objects.equals( this.key, transaction.key ) &&
                Objects.equals( this.status, transaction.status ) &&
                Objects.equals( this.type, transaction.type );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( amount, bill, credit, currency, key, status, type );
    }

    @Override
    public String toString()
    {
        return "class Transaction {\n" +
                "    amount: " + toIndentedString( amount ) + "\n" +
                "    balance: " + toIndentedString( balance ) + "\n" +
                "    bankAccount: " + toIndentedString( bankAccount ) + "\n" +
                "    bill: " + toIndentedString( bill ) + "\n" +
                "    completedAt: " + toIndentedString( completedAt ) + "\n" +
                "    credit: " + toIndentedString( credit ) + "\n" +
                "    currency: " + toIndentedString( currency ) + "\n" +
                "    reference: " + toIndentedString( reference ) + "\n" +
                "    key: " + toIndentedString( key ) + "\n" +
                "    status: " + toIndentedString( status ) + "\n" +
                "    type: " + toIndentedString( type ) + "\n" +
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

