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
