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
 * The bank account (beneficiary).  A valid IBAN will set following properties too, not needed to be set separately: * bank code * branch * bank country
 */
public class BankAccount
{
    @JsonProperty( "id" )
    private Long id = null;

    @JsonProperty( "name" )
    private String name = null;

    @JsonProperty( "branch" )
    private String branch = null;

    @JsonProperty( "iban" )
    private String iban = null;

    @JsonProperty( "bic" )
    private String bic = null;

    @JsonProperty( "currency" )
    private String currency = null;

    @JsonProperty( "primary" )
    private Boolean primary = null;

    @JsonProperty( "bank" )
    private Bank bank = null;

    public BankAccount id( Long id )
    {
        this.id = id;
        return this;
    }

    /**
     * The unique bank account identification within service. Assigned by the service.
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

    public BankAccount name( String name )
    {
        this.name = name;
        return this;
    }

    /**
     * The user defined name of the bank account.
     **/
    @JsonProperty( "name" )
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public BankAccount branch( String branch )
    {
        this.branch = branch;
        return this;
    }

    /**
     * The bank branch, taken from valid IBAN while set.
     **/
    @JsonProperty( "branch" )
    public String getBranch()
    {
        return branch;
    }

    public void setBranch( String branch )
    {
        this.branch = branch;
    }

    public BankAccount iban( String iban )
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

    public BankAccount bic( String bic )
    {
        this.bic = bic;
        return this;
    }

    /**
     * The international Bank Identifier Code (BIC/ISO 9362, a normalized code - also known as Business Identifier Code, Bank International Code and SWIFT code).
     **/
    @JsonProperty( "bic" )
    public String getBic()
    {
        return bic;
    }

    public void setBic( String bic )
    {
        this.bic = bic;
    }

    public BankAccount currency( String currency )
    {
        this.currency = currency;
        return this;
    }

    /**
     * The bank account currency. An alphabetic code based on the ISO 4217.
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

    public BankAccount primary( Boolean primary )
    {
        this.primary = primary;
        return this;
    }

    /**
     * Boolean identification, whether this bank account is being marked by the user as a primary credit account. If yes, this bank account will be used as a default credit account unless specified another one. There might be only max single or none primary credit bank account per country.
     **/
    @JsonProperty( "primary" )
    public Boolean isPrimary()
    {
        return primary;
    }

    public void setPrimary( Boolean primary )
    {
        this.primary = primary;
    }

    public BankAccount bank( Bank bank )
    {
        this.bank = bank;
        return this;
    }

    /**
     * Bank identified by bank code and its brief description.
     **/
    @JsonProperty( "bank" )
    public Bank getBank()
    {
        return bank;
    }

    public void setBank( Bank bank )
    {
        this.bank = bank;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( !( o instanceof BankAccount ) ) return false;
        BankAccount that = ( BankAccount ) o;
        return Objects.equals( iban, that.iban );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( iban );
    }

    @Override
    public String toString()
    {
        return "class BankAccount {\n" +
                "    id: " + toIndentedString( id ) + "\n" +
                "    name: " + toIndentedString( name ) + "\n" +
                "    branch: " + toIndentedString( branch ) + "\n" +
                "    iban: " + toIndentedString( iban ) + "\n" +
                "    bic: " + toIndentedString( bic ) + "\n" +
                "    currency: " + toIndentedString( currency ) + "\n" +
                "    primary: " + toIndentedString( primary ) + "\n" +
                "    bank: " + toIndentedString( bank ) + "\n" +
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

