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

    @Inject
    public PaymentBeanMapperConfig( BankCodeMapper bankCodeMapper,
                                    BankAccountMapper bankAccountMapper,
                                    BankAccountFactory bankAccountFactory )
    {
        this.bankCodeMapper = bankCodeMapper;
        this.bankAccountMapper = bankAccountMapper;
        this.bankAccountFactory = bankAccountFactory;
    }

    @Override
    public void config( MapperFactory factory )
    {
        factory.registerMapper( bankCodeMapper );
        factory.registerMapper( bankAccountMapper );

        factory.registerObjectFactory( bankAccountFactory, TypeFactory.valueOf( CompanyBankAccount.class ) );
    }
}
