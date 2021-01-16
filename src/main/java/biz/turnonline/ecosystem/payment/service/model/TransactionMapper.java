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

import biz.turnonline.ecosystem.payment.api.model.Transaction;
import biz.turnonline.ecosystem.payment.api.model.TransactionBank;
import biz.turnonline.ecosystem.payment.api.model.TransactionCategory;
import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.metadata.Type;

/**
 * Single direction base mapper from {@link CommonTransaction} to {@link Transaction}.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
abstract class TransactionMapper<T extends CommonTransaction>
        extends CustomConverter<T, Transaction>
{
    @Override
    public Transaction convert( T source,
                                Type<? extends Transaction> destinationType,
                                MappingContext mappingContext )
    {
        Transaction transaction = new Transaction();

        transaction.setTransactionId( source.getId() );
        transaction.setAmount( source.getAmount() );
        transaction.setBalance( source.getBalance() );
        transaction.setCompletedAt( source.getCompletedAt() );
        transaction.setCredit( source.isCredit() );
        transaction.setCurrency( source.getCurrency() );
        transaction.setReference( source.getReference() );
        transaction.setKey( source.getKey() );
        transaction.setBillAmount( source.getBillAmount() );
        transaction.setBillCurrency( source.getBillCurrency() );

        CommonTransaction.State status = source.getStatus();
        transaction.setStatus( status == null ? null : status.name() );

        FormOfPayment type = source.getType();
        transaction.setType( type == null ? null : type.name() );

        CompanyBankAccount bankAccount = source.loadBankAccount();
        if ( bankAccount != null )
        {
            TransactionBank bank = new TransactionBank();
            bank.setIban( bankAccount.getIbanString() );
            transaction.setBankAccount( bank );
        }

        String bankCode = source.getBankCode();
        if ( bankCode != null )
        {
            TransactionBank bank = transaction.getBankAccount();
            if ( bank == null )
            {
                bank = new TransactionBank();
                transaction.setBankAccount( bank );
            }

            bank.setCode( bankCode );
        }

        if ( source.getCategories() != null )
        {
            transaction.setCategories( mapperFacade.mapAsList( source.getCategories(), TransactionCategory.class ) );
        }

        return transaction;
    }
}
