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
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Index;
import org.ctoolkit.services.datastore.objectify.EntityLongIdentity;
import org.ctoolkit.services.datastore.objectify.IndexModificationDate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Database representation of the transaction (either credit or debit).
 **/
@Entity( name = "PP_Transaction" )
public abstract class CommonTransaction
        extends EntityLongIdentity
        implements IndexModificationDate
{
    private static final long serialVersionUID = -4345791753726496666L;

    private Long accountId;

    private Double balance;

    @Index
    private String bankCode;

    @Index
    private Date completedAt;

    @Index
    private String key;

    private Double amount;

    private String currency;

    @Index
    private boolean credit;

    @Index
    private boolean failure = true;

    @Index
    private FormOfPayment type;

    private String reference;

    private List<Object> origins;

    @Index
    private String extId;

    /**
     * Either a debtor or creditor bank account identification.
     *
     * @return the account Id
     */
    public CommonTransaction accountId( Long accountId )
    {
        this.accountId = accountId;
        return this;
    }

    public Long getAccountId()
    {
        return accountId;
    }

    /**
     * The transaction amount absolute value.
     **/
    public CommonTransaction amount( Double amount )
    {
        this.amount = amount;
        return this;
    }

    public Double getAmount()
    {
        return amount;
    }

    /**
     * The balance after the transaction.
     */
    public CommonTransaction balance( Double balance )
    {
        this.balance = balance;
        return this;
    }

    public Double getBalance()
    {
        return balance;
    }

    /**
     * The bank identified by code
     */
    public CommonTransaction bankCode( String bankCode )
    {
        this.bankCode = bankCode;
        return this;
    }

    public String getBankCode()
    {
        return bankCode;
    }

    /**
     * The date when the transaction was completed.
     */
    public CommonTransaction completedAt( OffsetDateTime completedAt )
    {
        this.completedAt = toDate( completedAt );
        return this;
    }

    public Date getCompletedAt()
    {
        return completedAt;
    }

    /**
     * The boolean indicating whether the payment has positive or negative amount; true - credit, false - debit.
     **/
    public CommonTransaction credit( boolean credit )
    {
        this.credit = credit;
        return this;
    }

    public boolean isCredit()
    {
        return credit;
    }

    /**
     * The boolean indication whether transaction has failed.
     */
    public CommonTransaction failure( boolean failure )
    {
        this.failure = failure;
        return this;
    }

    /**
     * Returns {@code true} if transaction has failed.
     */
    public boolean isFailure()
    {
        return failure;
    }

    /**
     * The payment currency alphabetic code based on the ISO 4217.
     **/
    public CommonTransaction currency( String currency )
    {
        this.currency = currency;
        return this;
    }

    public String getCurrency()
    {
        return currency;
    }

    /**
     * The payment type that has been used to make this payment.
     **/
    public CommonTransaction type( FormOfPayment type )
    {
        this.type = type;
        return this;
    }

    public FormOfPayment getType()
    {
        return type;
    }

    /**
     * The unique payment identification related to the associated invoice or bill.
     **/
    public CommonTransaction key( String key )
    {
        this.key = key;
        return this;
    }

    public String getKey()
    {
        return key;
    }

    /**
     * A user provided payment reference.
     */
    public CommonTransaction reference( String reference )
    {
        this.reference = reference;
        return this;
    }

    public String getReference()
    {
        return reference;
    }

    public CommonTransaction externalId( String extId )
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

    /**
     * Adds incoming transaction to be stored for archiving purpose.
     *
     * @param origin the origin incoming transaction
     */
    public void addOrigin( @Nonnull Object origin )
    {
        checkNotNull( origin, "Origin incoming transaction can't be null" );
        if ( this.origins == null )
        {
            this.origins = new ArrayList<>();
        }

        this.origins.add( origin );
    }

    public List<Object> getOrigins()
    {
        return origins;
    }

    public Date toDate( @Nullable OffsetDateTime odt )
    {
        if ( odt == null )
        {
            return null;
        }

        return Date.from( odt.toInstant() );
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
        if ( !( o instanceof CommonTransaction ) ) return false;
        CommonTransaction that = ( CommonTransaction ) o;
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
                .add( "bankCode", bankCode )
                .add( "completedAt", completedAt )
                .add( "key", key )
                .add( "amount", amount )
                .add( "currency", currency )
                .add( "credit", credit )
                .add( "type", type )
                .add( "reference", reference )
                .add( "extId", extId )
                .add( "origins", origins )
                .toString();
    }
}

