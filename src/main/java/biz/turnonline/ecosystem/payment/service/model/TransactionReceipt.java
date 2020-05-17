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

import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Subclass;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Transaction that represents a payment for a receipt (statement of charges from cash register etc.).
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Subclass( name = "Receipt", index = true )
public class TransactionReceipt
        extends CommonTransaction
{
    private static final long serialVersionUID = 5129057271906568133L;

    @Index
    private String category;

    private String city;

    private String name;

    @Index
    private Long receipt;

    /**
     * Needed if instantiated by objectify.
     */
    @SuppressWarnings( "unused" )
    TransactionReceipt()
    {
    }

    public TransactionReceipt( @Nonnull String extId )
    {
        super.externalId( checkNotNull( extId, "The receipt external ID can't be null" ) );
    }

    /**
     * Returns the merchant category (code).
     *
     * @return the merchant category
     */
    public String getCategory()
    {
        return category;
    }

    public void setCategory( String category )
    {
        this.category = category;
    }

    /**
     * Returns the merchant city.
     *
     * @return the merchant city
     */
    public String getCity()
    {
        return city;
    }

    public void setCity( String city )
    {
        this.city = city;
    }

    /**
     * Returns the merchant name.
     *
     * @return the merchant name
     */
    public String getMerchantName()
    {
        return name;
    }

    public void setMerchantName( String name )
    {
        this.name = name;
    }

    /**
     * Returns the unique identification of the receipt within Billing Processor service.
     *
     * @return the receipt ID
     */
    public Long getReceipt()
    {
        return receipt;
    }

    /**
     * Sets the receipt identified by ID within Billing Processor service to be associated with this transaction.
     *
     * @param id the receipt ID to be set
     */
    public void setReceipt( Long id )
    {
        this.receipt = id;
    }
}
