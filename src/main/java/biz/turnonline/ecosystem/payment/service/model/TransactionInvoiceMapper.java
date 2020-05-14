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

import biz.turnonline.ecosystem.payment.api.model.Bill;
import biz.turnonline.ecosystem.payment.api.model.Transaction;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.metadata.Type;

import javax.inject.Singleton;

/**
 * Single direction base mapper from {@link TransactionInvoice} to {@link Transaction}.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
class TransactionInvoiceMapper
        extends TransactionMapper<TransactionInvoice>
{
    @Override
    public Transaction convert( TransactionInvoice source,
                                Type<? extends Transaction> destinationType,
                                MappingContext mappingContext )
    {
        Transaction transaction = super.convert( source, destinationType, mappingContext );

        Bill bill = new Bill();
        bill.setOrder( source.getOrderId() );
        bill.setInvoice( source.getInvoiceId() );

        if ( source.getOrderId() != null || source.getInvoiceId() != null )
        {
            transaction.setBill( bill );
        }

        return transaction;
    }
}
