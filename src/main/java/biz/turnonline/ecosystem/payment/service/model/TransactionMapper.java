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

import biz.turnonline.ecosystem.payment.api.model.Bank;
import biz.turnonline.ecosystem.payment.api.model.BankAccount;
import biz.turnonline.ecosystem.payment.api.model.Transaction;
import com.googlecode.objectify.Key;
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

        transaction.setAmount( source.getAmount() );
        transaction.setBalance( source.getBalance() );
        transaction.setCompletedAt( source.getCompletedAt() );
        transaction.setCredit( source.isCredit() );
        transaction.setCurrency( source.getCurrency() );
        transaction.setReference( source.getReference() );
        transaction.setKey( source.getKey() );

        CommonTransaction.State status = source.getStatus();
        transaction.setStatus( status == null ? null : status.name() );

        FormOfPayment type = source.getType();
        transaction.setType( type == null ? null : type.name() );

        Key<CompanyBankAccount> key = source.getBankAccountKey();
        if ( key != null )
        {
            BankAccount bankAccount = new BankAccount();
            bankAccount.setId( key.getId() );
            transaction.setBankAccount( bankAccount );
        }

        String bankCode = source.getBankCode();
        if ( bankCode != null )
        {
            BankAccount bankAccount = transaction.getBankAccount();
            if ( bankAccount == null )
            {
                bankAccount = new BankAccount();
                transaction.setBankAccount( bankAccount );
            }

            Bank code = new Bank();
            code.setCode( bankCode );
            bankAccount.setBank( code );
        }

        return transaction;
    }
}
