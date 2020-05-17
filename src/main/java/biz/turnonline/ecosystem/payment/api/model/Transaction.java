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
    private TransactionBank bankAccount;

    @JsonProperty( "bill" )
    private Bill bill;

    @JsonProperty( "completedAt" )
    private Date completedAt;

    @JsonProperty( "credit" )
    private Boolean credit;

    @JsonProperty( "currency" )
    private String currency;

    @JsonProperty( "key" )
    private String key;

    @JsonProperty( "merchant" )
    private Merchant merchant;

    @JsonProperty( "reference" )
    private String reference;

    @JsonProperty( "status" )
    private String status;

    @JsonProperty( "transactionId" )
    private Long transactionId;

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

    public Transaction bankAccount( TransactionBank bankAccount )
    {
        this.bankAccount = bankAccount;
        return this;
    }

    /**
     * The bank account associated with this transaction.
     **/
    @JsonProperty( "bankAccount" )
    public TransactionBank getBankAccount()
    {
        return bankAccount;
    }

    public void setBankAccount( TransactionBank bankAccount )
    {
        this.bankAccount = bankAccount;
    }

    public Transaction bill( Bill bill )
    {
        this.bill = bill;
        return this;
    }

    /**
     * Identification of the bill (receipt) or invoice document settled by this transaction.
     *
     * @return bill
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
     * The date when the transaction was completed (status COMPLETED).
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

    public Transaction merchant( Merchant merchant )
    {
        this.merchant = merchant;
        return this;
    }

    /**
     * The merchant details, available only for card payments.
     **/
    @JsonProperty( "merchant" )
    public Merchant getMerchant()
    {
        return merchant;
    }

    public void setMerchant( Merchant merchant )
    {
        this.merchant = merchant;
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

    public Transaction status( String status )
    {
        this.status = status;
        return this;
    }

    /**
     * The transaction status.
     **/
    @JsonProperty( "status" )
    public String getStatus()
    {
        return status;
    }

    public void setStatus( String status )
    {
        this.status = status;
    }

    public Transaction transactionId( Long transactionId )
    {
        this.transactionId = transactionId;
        return this;
    }

    /**
     * The identification of the transaction within payment processor service unique for single Ecosystem account.
     **/
    @JsonProperty( "transactionId" )
    public Long getTransactionId()
    {
        return transactionId;
    }

    public void setTransactionId( Long transactionId )
    {
        this.transactionId = transactionId;
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
        return Objects.equals( this.transactionId, transaction.transactionId );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( transactionId );
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
                "    key: " + toIndentedString( key ) + "\n" +
                "    reference: " + toIndentedString( reference ) + "\n" +
                "    status: " + toIndentedString( status ) + "\n" +
                "    transactionId: " + toIndentedString( transactionId ) + "\n" +
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

