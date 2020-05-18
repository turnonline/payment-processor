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

import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.metadata.TypeFactory;
import org.ctoolkit.restapi.client.adapter.BeanMapperConfig;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * The payment model orika mapper configuration.
 * Here is the place to configure all of the payment related orika mappers and factories.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
public class PaymentBeanMapperConfig
        implements BeanMapperConfig
{
    private final BankCodeMapper bankCodeMapper;

    private final BankAccountMapper bankAccountMapper;

    private final BankAccountFactory bankAccountFactory;

    private final TransactionReceiptMapper transactionBillMapper;

    private final TransactionInvoiceMapper transactionInvoiceMapper;

    @Inject
    public PaymentBeanMapperConfig( BankCodeMapper bankCodeMapper,
                                    BankAccountMapper bankAccountMapper,
                                    BankAccountFactory bankAccountFactory,
                                    TransactionReceiptMapper transactionBillMapper,
                                    TransactionInvoiceMapper transactionInvoiceMapper )
    {
        this.bankCodeMapper = bankCodeMapper;
        this.bankAccountMapper = bankAccountMapper;
        this.bankAccountFactory = bankAccountFactory;
        this.transactionBillMapper = transactionBillMapper;
        this.transactionInvoiceMapper = transactionInvoiceMapper;
    }

    @Override
    public void config( MapperFactory factory )
    {
        factory.registerMapper( bankCodeMapper );
        factory.registerMapper( bankAccountMapper );

        factory.registerObjectFactory( bankAccountFactory, TypeFactory.valueOf( CompanyBankAccount.class ) );
        factory.getConverterFactory().registerConverter( transactionBillMapper );
        factory.getConverterFactory().registerConverter( transactionInvoiceMapper );
    }
}
