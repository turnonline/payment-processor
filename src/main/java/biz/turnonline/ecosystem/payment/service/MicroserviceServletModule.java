package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.payment.oauth.RevolutOauth2AuthRedirect;
import com.google.inject.servlet.ServletModule;

/**
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class MicroserviceServletModule
        extends ServletModule
{
    @Override
    protected void configureServlets()
    {
        serve( "/payment/oauth2" ).with( RevolutOauth2AuthRedirect.class );
    }
}
