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
import com.googlecode.objectify.annotation.Subclass;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Beneficiary bank account as a counterparty.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Subclass( name = "Beneficiary", index = true )
public class BeneficiaryBankAccount
        extends BankAccount
{
    private static final long serialVersionUID = 4462909552919142290L;

    @Inject
    public BeneficiaryBankAccount( CodeBook codeBook )
    {
        super( codeBook );
    }

    @Override
    public String getExternalId( @Nonnull String code )
    {
        return super.getExternalId( code );
    }

    @Override
    public void setExternalId( @Nonnull String code, @Nullable String externalId )
    {
        super.setExternalId( code, externalId );
    }

    @Override
    public String getKind()
    {
        return "BeneficiaryBankAccount";
    }
}
