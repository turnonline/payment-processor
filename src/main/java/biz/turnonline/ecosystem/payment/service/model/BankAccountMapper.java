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

import biz.turnonline.ecosystem.payment.api.ApiValidationException;
import biz.turnonline.ecosystem.payment.api.model.Bank;
import biz.turnonline.ecosystem.payment.service.CodeBook;
import com.google.common.net.HttpHeaders;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.Optional;

/**
 * Mapper: {@link CompanyBankAccount} back and forth {@link biz.turnonline.ecosystem.payment.api.model.BankAccount}.
 * It supports patch semantics for direction from API to Backend, however
 * following properties are being ignored as they are managed solely by the backend service.
 * <ul>
 * <li>id</li>k
 * <li>bank.label</li>
 * </ul>
 * In order to make property 'bank.label' locale sensitive for direction Backend to API,
 * make sure the context property is being set: {@code context.setProperty( HttpHeaders.ACCEPT_LANGUAGE, language );}.
 * <p>
 * The direction API to Backend performs validation and might throw {@link ApiValidationException}.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
class BankAccountMapper
        extends CustomMapper<CompanyBankAccount, biz.turnonline.ecosystem.payment.api.model.BankAccount>
{
    private final CodeBook codeBook;

    @Inject
    BankAccountMapper( CodeBook codeBook )
    {
        this.codeBook = codeBook;
    }

    @Override
    public void mapAtoB( CompanyBankAccount source,
                         biz.turnonline.ecosystem.payment.api.model.BankAccount bankAccount,
                         MappingContext context )
    {
        bankAccount.setId( source.getId() );
        bankAccount.setName( source.getName() );
        bankAccount.setBranch( source.getBranch() );
        bankAccount.setIban( source.getIbanString() );
        bankAccount.setBic( source.getBic() );
        bankAccount.setCurrency( source.getCurrency() );
        bankAccount.setPrimary( source.isPrimary() );

        String bankCode = source.getBankCode();
        if ( bankCode != null )
        {
            Locale locale = ( Locale ) context.getProperty( HttpHeaders.ACCEPT_LANGUAGE );
            LocalAccount account = ( LocalAccount ) context.getProperty( LocalAccount.class );
            if ( account == null )
            {
                String message = "Authenticated account is mandatory, expected as a MappingContext property with key: "
                        + LocalAccount.class;
                throw new IllegalArgumentException( message );
            }

            Bank bank = new Bank();
            bank.setCode( bankCode );
            bank.setLabel( source.getLocalizedLabel( locale, account ) );
            bank.setCountry( source.getCountry() );

            bankAccount.setBank( bank );
        }
    }

    @Override
    public void mapBtoA( biz.turnonline.ecosystem.payment.api.model.BankAccount source,
                         CompanyBankAccount backend,
                         MappingContext context )
    {
        Optional<String> sValue;

        try
        {
            sValue = Optional.ofNullable( source.getCurrency() );
            sValue.ifPresent( backend::setCurrency );
        }
        catch ( IllegalArgumentException e )
        {
            throw ApiValidationException.prepare( "errors.validation.currency", source.getCurrency() );
        }

        sValue = Optional.ofNullable( source.getName() );
        sValue.ifPresent( backend::setName );

        // A non null IBAN will set bankCode, branch and country
        sValue = Optional.ofNullable( source.getIban() );
        sValue.ifPresent( backend::setIban );

        sValue = Optional.ofNullable( source.getBic() );
        sValue.ifPresent( backend::setBic );

        if ( backend.getIBAN() == null )
        {
            String key = "errors.validation.mandatory.property.missing";
            throw ApiValidationException.prepare( key, "iban" );
        }

        String code = backend.getBankCode();
        if ( code == null )
        {
            String key = "errors.validation.mandatory.property.missing.iban";
            throw ApiValidationException.prepare( key );
        }

        LocalAccount account = ( LocalAccount ) context.getProperty( LocalAccount.class );
        if ( account == null )
        {
            String message = "Authenticated account is mandatory, expected as a MappingContext property with key: "
                    + LocalAccount.class;
            throw new IllegalArgumentException( message );
        }

        String country = backend.getCountry();
        BankCode bankCode = codeBook.getBankCode( code, null, country );
        if ( bankCode == null )
        {
            if ( country == null )
            {
                String key = "errors.validation.bankAccount.bankCode.unsupported";
                throw ApiValidationException.prepare( key, code );
            }
            else
            {
                String key = "errors.validation.bankAccount.bankCode.country.unsupported";
                throw ApiValidationException.prepare( key, code, country );
            }
        }
    }
}
