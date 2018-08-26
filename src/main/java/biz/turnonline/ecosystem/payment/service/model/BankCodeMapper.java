package biz.turnonline.ecosystem.payment.service.model;

import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;

import javax.inject.Singleton;

/**
 * Mapper from {@link BankCode} to {@link biz.turnonline.ecosystem.payment.api.model.BankCode}.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
class BankCodeMapper
        extends CustomMapper<BankCode, biz.turnonline.ecosystem.payment.api.model.BankCode>
{
    @Override
    public void mapAtoB( BankCode source,
                         biz.turnonline.ecosystem.payment.api.model.BankCode bankCode,
                         MappingContext context )
    {
        bankCode.setCode( source.getCode() );
        bankCode.setLabel( source.getLabel() );
        bankCode.setLocale( source.getLocale() );
        bankCode.setCountry( source.getCountry() );
    }

    @Override
    public void mapBtoA( biz.turnonline.ecosystem.payment.api.model.BankCode source,
                         BankCode backend,
                         MappingContext context )
    {
        throw new UnsupportedOperationException();
    }
}
