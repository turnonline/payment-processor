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

import biz.turnonline.ecosystem.payment.service.CodeBook;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Subclass;
import nl.garvelink.iban.IBAN;

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * The company bank account.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Subclass( name = "Company", index = true )
public class CompanyBankAccount
        extends BankAccount
{
    private static final long serialVersionUID = 8061125921099231349L;

    @Index
    private PaymentGate paymentGate;

    @Index
    private String merchantId;

    private String notificationEmail;

    private boolean primary;

    private boolean gateEnabled;

    @Inject
    public CompanyBankAccount( CodeBook codeBook )
    {
        super( codeBook );
    }

    @Override
    public String getExternalId()
    {
        return super.getExternalId();
    }

    @Override
    public void setExternalId( @Nullable String externalId )
    {
        super.setExternalId( externalId );
    }

    /**
     * Returns the international bank account number as formatted string.
     */
    public String getIbanString()
    {
        IBAN iban = getIBAN();
        return iban == null ? null : iban.toString();
    }

    public PaymentGate getPaymentGate()
    {
        return paymentGate;
    }

    public void setPaymentGate( PaymentGate paymentGate )
    {
        this.paymentGate = paymentGate;
    }

    public String getMerchantId()
    {
        return merchantId;
    }

    public void setMerchantId( String merchantId )
    {
        this.merchantId = merchantId;
    }

    public String getNotificationEmail()
    {
        return notificationEmail;
    }

    public void setNotificationEmail( String notificationEmail )
    {
        this.notificationEmail = notificationEmail;
    }

    /**
     * Boolean identification, whether this bank account is being marked by the user as a primary account.
     * If yes, this bank account will be used as a default account unless specified another one.
     * There is always only single primary bank account per country.
     */
    public boolean isPrimary()
    {
        return primary;
    }

    public void setPrimary( boolean primary )
    {
        this.primary = primary;
    }

    public boolean isGateEnabled()
    {
        return gateEnabled;
    }

    public void setGateEnabled( boolean gateEnabled )
    {
        this.gateEnabled = gateEnabled;
    }


    /**
     * Returns the boolean indication whether this bank account is valid to be debited via API.
     * <p>
     * To be ready it must have a non null value for following properties:
     * <ul>
     *     <li>{@link #getIBAN()}</li>
     *     <li>{@link #getCurrency()}</li>
     *     <li>{@link #getExternalId()}</li>
     *     <li>{@link #getBankCode()} ()}</li>
     * </ul>
     *
     * @return true if the bank account is ready
     */
    public boolean isDebtorReady()
    {
        return getIBAN() != null
                && !Strings.isNullOrEmpty( getCurrency() )
                && !Strings.isNullOrEmpty( getExternalId() )
                && !Strings.isNullOrEmpty( getBankCode() );
    }

    @Override
    protected long getModelVersion()
    {
        //21.10.2017 08:00:00 GMT+0200
        return 1508565600000L;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
                .addValue( super.toString() )
                .add( "paymentGate", paymentGate )
                .add( "merchantId", merchantId )
                .add( "notificationEmail", notificationEmail )
                .add( "primary", primary )
                .add( "gateEnabled", gateEnabled )
                .toString();
    }

    @Override
    public String getKind()
    {
        return "CompanyBankAccount";
    }
}
