package biz.turnonline.ecosystem.payment.service.model;

import biz.turnonline.ecosystem.payment.service.CodeBook;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.ObjectFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * {@link BankAccount} factory responsible to create a new instance to be used by Orika.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
class BankAccountFactory
        implements ObjectFactory<BankAccount>
{
    private final CodeBook codeBook;

    @Inject
    BankAccountFactory( CodeBook codeBook )
    {
        this.codeBook = codeBook;
    }

    @Override
    public BankAccount create( Object source, MappingContext mappingContext )
    {
        return new BankAccount( codeBook );
    }
}