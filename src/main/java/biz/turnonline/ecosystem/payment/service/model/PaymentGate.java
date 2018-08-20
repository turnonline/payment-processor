package biz.turnonline.ecosystem.payment.service.model;


import biz.turnonline.ecosystem.payment.service.CardPay;
import biz.turnonline.ecosystem.payment.service.EPlatbyVUB;
import biz.turnonline.ecosystem.payment.service.PayPal;
import biz.turnonline.ecosystem.payment.service.Sporopay;
import biz.turnonline.ecosystem.payment.service.TatraPay;
import biz.turnonline.ecosystem.payment.service.Transfer;
import biz.turnonline.ecosystem.payment.service.TrustPay;

/**
 * The payment gate marker.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public enum PaymentGate
{
    // International payment gates
    PAYPAL( PayPal.class ),
    TRUSTPAY( TrustPay.class ),

    // Slovak payment gates
    TATRAPAY( TatraPay.class ),
    CARDPAY( CardPay.class ),
    SPOROPAY( Sporopay.class ),
    EPLATBY_VUB( EPlatbyVUB.class ),
    TRANSFER( Transfer.class );


    private Class serviceClass;

    PaymentGate( Class serviceClass )
    {
        this.serviceClass = serviceClass;
    }

    public Class getServiceClass()
    {
        return serviceClass;
    }
}
