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

package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.payment.oauth.RevolutOauth2AuthRedirect;
import biz.turnonline.ecosystem.payment.webhook.RevolutWebhookSubscription;
import com.google.inject.servlet.ServletModule;

/**
 * Servlet injection configuration.
 * <p>
 * Path '/revolut/oauth2' mapped to servlet that processes OAuth2
 * redirection to authorise access to Revolut Business API.
 * </p>
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 * @see RevolutOauth2AuthRedirect
 */
public class MicroserviceServletModule
        extends ServletModule
{
    @Override
    protected void configureServlets()
    {
        serve( "/revolut/oauth2" ).with( RevolutOauth2AuthRedirect.class );
        serve( "/revolut/webhook" ).with( RevolutWebhookSubscription.class );
    }
}
