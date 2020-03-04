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

package biz.turnonline.ecosystem.payment.service.model;

import com.google.common.base.MoreObjects;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Index;
import org.ctoolkit.services.datastore.objectify.EntityLongIdentity;
import org.ctoolkit.services.storage.HasOwner;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Database representation of the transaction (either credit or debit).
 **/
@Entity( name = "PP_Transaction" )
public class Transaction
        extends EntityLongIdentity
        implements HasOwner<LocalAccount>
{
    private static final long serialVersionUID = -1148494734053283570L;

    @Index
    private Ref<LocalAccount> owner;

    @Index
    private String bankCode;

    @Index
    private Date completedAt;

    @Index
    private String key;

    private Double amount;

    private String currency;

    private boolean credit;

    private FormOfPayment type;

    private String reference;

    private String extId;

    /**
     * The transaction amount absolute value.
     **/
    public Transaction amount( Double amount )
    {
        this.amount = amount;
        return this;
    }

    public Double getAmount()
    {
        return amount;
    }

    public void setAmount( Double amount )
    {
        this.amount = amount;
    }

    /**
     * The bank identified by code
     */
    public Transaction bankCode( String bankCode )
    {
        this.bankCode = bankCode;
        return this;
    }

    public String getBankCode()
    {
        return bankCode;
    }

    public void setBankCode( String bankCode )
    {
        this.bankCode = bankCode;
    }

    /**
     * The date when the transaction was completed.
     */
    public Transaction completedAt( Date completedAt )
    {
        this.completedAt = completedAt;
        return this;
    }

    public Date getCompletedAt()
    {
        return completedAt;
    }

    public void setCompletedAt( Date completedAt )
    {
        this.completedAt = completedAt;
    }

    /**
     * The boolean indicating whether the payment has positive or negative amount; true - credit, false - debit.
     **/
    public Transaction credit( boolean credit )
    {
        this.credit = credit;
        return this;
    }

    public boolean isCredit()
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
    public Transaction type( FormOfPayment type )
    {
        this.type = type;
        return this;
    }

    public FormOfPayment getType()
    {
        return type;
    }

    public void setType( FormOfPayment type )
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

    public String getReference()
    {
        return reference;
    }

    public void setReference( String reference )
    {
        this.reference = reference;
    }

    public Transaction externalId( String extId )
    {
        this.extId = extId;
        return this;
    }

    /**
     * Returns the external identification of the transaction synchronized in to bank system.
     *
     * @return the external Id
     */
    public String getExternalId()
    {
        return extId;
    }

    public void setExternalId( String extId )
    {
        this.extId = extId;
    }

    @Override
    public void save()
    {
        ofy().transact( () -> ofy().defer().save().entity( this ) );
    }

    @Override
    public void delete()
    {
        ofy().transact( () -> ofy().defer().delete().entity( this ) );
    }

    @Override
    protected long getModelVersion()
    {
        //21.10.2017 08:00:00 GMT+0200
        return 1508565600000L;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( !( o instanceof Transaction ) ) return false;
        Transaction that = ( Transaction ) o;
        return credit == that.credit &&
                Objects.equals( amount, that.amount ) &&
                Objects.equals( bankCode, that.bankCode ) &&
                Objects.equals( currency, that.currency ) &&
                Objects.equals( type, that.type ) &&
                Objects.equals( key, that.key );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( amount, bankCode, credit, currency, type, key );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
                .addValue( super.toString() )
                .add( "owner", owner )
                .add( "bankCode", bankCode )
                .add( "completedAt", completedAt )
                .add( "key", key )
                .add( "amount", amount )
                .add( "currency", currency )
                .add( "credit", credit )
                .add( "type", type )
                .add( "reference", reference )
                .add( "extId", extId )
                .toString();
    }

    @Override
    public LocalAccount getOwner()
    {
        return fromRef( owner, null );
    }

    @Override
    public void setOwner( @Nonnull LocalAccount owner )
    {
        this.owner = Ref.create( checkNotNull( owner, "LocalAccount as an owner can't be null" ) );
    }
}

