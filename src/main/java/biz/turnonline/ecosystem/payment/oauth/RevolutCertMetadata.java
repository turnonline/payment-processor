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

package biz.turnonline.ecosystem.payment.oauth;

import com.google.common.base.MoreObjects;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.OnSave;
import org.ctoolkit.services.datastore.objectify.EntityStringIdentity;

import java.util.Date;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * The entity to keep current Revolut certificate metadata, not a sensitive data.
 * The entity identification is being based on the issuer (domain based)
 * that is unique for microservice (design concept).
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Entity( name = "PP_RevolutCertMetadata" )
public class RevolutCertMetadata
        extends EntityStringIdentity
{
    public static final String PRIVATE_KEY_NAME = "Revolut_private_key";

    private static final long serialVersionUID = -2081880614068205005L;

    private boolean accessAuthorised;

    private Date authorisedOn;

    private String clientId;

    private String code;

    /**
     * The private key name, initialized with default value
     */
    private String keyName = PRIVATE_KEY_NAME;

    /**
     * Returns the boolean indication whether access to Revolut for Business API
     * for current authorisation code has been already granted.
     *
     * @return true if access is already granted
     */
    public boolean isAccessAuthorised()
    {
        return accessAuthorised;
    }

    /**
     * Marks current authorisation code that has been consumed
     * and access to Revolut for Business API has been granted.
     */
    public RevolutCertMetadata accessGranted()
    {
        this.accessAuthorised = true;
        this.authorisedOn = new Date();
        return this;
    }

    /**
     * Returns date and time when access to Revolut for Business API for current authorisation code was granted.
     *
     * @return the date and time when access was granted
     */
    public Date getAuthorisedOn()
    {
        return authorisedOn;
    }

    /**
     * Returns the certificate client ID configured by user.
     *
     * @return the client ID
     */
    public String getClientId()
    {
        return clientId;
    }

    public RevolutCertMetadata setClientId( String clientId )
    {
        this.clientId = clientId;
        return this;
    }

    /**
     * Returns the authorisation code processed while OAuth redirect URI
     *
     * @return the authorisation code
     */
    public String getCode()
    {
        return code;
    }

    /**
     * Sets a new authorisation code to be authorized, {@link #isAccessAuthorised()} will return {@code false}.
     *
     * @param code the new authorisation code to be set
     */
    public RevolutCertMetadata setCode( String code )
    {
        this.accessAuthorised = false;
        this.authorisedOn = null;
        this.code = code;
        return this;
    }

    /**
     * Returns the secret manager private key name configured by user, or default one {@link #PRIVATE_KEY_NAME}.
     *
     * @return private key name
     */
    public String getKeyName()
    {
        return keyName;
    }

    public RevolutCertMetadata setKeyName( String keyName )
    {
        this.keyName = keyName;
        return this;
    }

    @OnSave
    void onSave()
    {
        if ( keyName == null )
        {
            keyName = PRIVATE_KEY_NAME;
        }
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

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
                .add( "accessAuthorised", accessAuthorised )
                .add( "authorisedOn", authorisedOn )
                .add( "clientId", clientId )
                .add( "code", code )
                .add( "keyName", keyName )
                .toString();
    }
}
