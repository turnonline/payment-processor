package biz.turnonline.ecosystem.payment.subscription;

import biz.turnonline.ecosystem.billing.model.IncomingInvoice;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.steward.model.Account;
import com.googlecode.objectify.Key;
import mockit.Injectable;
import mockit.Tested;
import org.ctoolkit.restapi.client.RestFacade;
import org.testng.annotations.Test;

import static biz.turnonline.ecosystem.payment.service.BackendServiceTestCase.genericJsonFromFile;

/**
 * {@link RevolutBeneficiarySyncTask} unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class RevolutBeneficiarySyncTaskTest
{
    @Tested
    private RevolutBeneficiarySyncTask tested;

    @Injectable
    private Key<LocalAccount> accountKey;

    @Injectable
    private String json = "{}";

    @Injectable
    private RestFacade facade;

    @Injectable
    private PaymentConfig config;

    private LocalAccount account = new LocalAccount( new Account()
            .setId( 1735L )
            .setEmail( "my.account@turnonline.biz" )
            .setIdentityId( "64HGtr6ks" )
            .setAudience( "a1b" ) );


    @Test
    public void execute()
    {
        IncomingInvoice invoice = genericJsonFromFile( "incoming-invoice.pubsub.json", IncomingInvoice.class );
        tested.execute( account, invoice );
    }
}