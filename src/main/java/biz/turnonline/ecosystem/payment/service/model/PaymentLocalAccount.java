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

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import org.ctoolkit.services.datastore.objectify.EntityStringIdentity;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * The single account for payment microservice (design concept).
 * It keeps the association to the {@link LocalAccount} that's considered as a payment service owner.
 * <p>
 * <strong>Project ID</strong>
 * </p>
 * Identification of this account is based on: {@code ServiceOptions.getDefaultProjectId()} known as project Id.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Entity( name = "PP_PaymentAccount" )
public class PaymentLocalAccount
        extends EntityStringIdentity
{
    private static final long serialVersionUID = 3638777835772543224L;

    private Ref<LocalAccount> account;

    @SuppressWarnings( "unused" )
    PaymentLocalAccount()
    {
    }

    public PaymentLocalAccount( @Nonnull LocalAccount account, @Nonnull String id )
    {
        setId( checkNotNull( id, "Payment account ID can't be null" ) );
        this.account = Ref.create( checkNotNull( account, "LocalAccount can't be null" ) );
    }

    public LocalAccount get()
    {
        return fromRef( account, null );
    }

    @Override
    protected long getModelVersion()
    {
        //21.10.2017 08:00:00 GMT+0200
        return 1508565600000L;
    }

    @Override
    public void save()
    {
        ofy().transact( () -> ofy().defer().save().entity( this ) );
    }

    @Override
    public void delete()
    {
        ofy().transact( () -> ofy().defer().delete().entity( this ) );
    }
}
