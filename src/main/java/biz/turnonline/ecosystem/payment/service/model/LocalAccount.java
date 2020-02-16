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

import biz.turnonline.ecosystem.payment.service.LocalAccountProvider;
import biz.turnonline.ecosystem.payment.service.NoRetryException;
import biz.turnonline.ecosystem.steward.facade.Domicile;
import biz.turnonline.ecosystem.steward.model.Account;
import biz.turnonline.ecosystem.steward.model.AccountBusiness;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import org.ctoolkit.restapi.client.NotFoundException;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.services.datastore.objectify.EntityLongIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * The datastore local account acts as an owner of the entities associated with an account.
 * It represents the TurnOnline.biz account taken from the Account Steward microservice.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Cache( expirationSeconds = 3600 )
@Entity( name = "PP_LocalAccount" )
public class LocalAccount
        extends EntityLongIdentity
{
    static final String DEFAULT_ZONE = "Europe/Paris";

    private static final long serialVersionUID = -2815259691876951647L;

    private static final Logger LOGGER = LoggerFactory.getLogger( LocalAccount.class );

    private static final Locale DEFAULT_LOCALE = new Locale( "en" );

    private static final String DEFAULT_DOMICILE = Domicile.getDefault().name();

    @Ignore
    private transient RestFacade facade;

    @Ignore
    private transient Account tAccount;

    @Index
    private String email;

    @Index
    private String identityId;

    @Index
    private String audience;

    private String domicile;

    private String zone;

    private String locale;

    @Inject
    LocalAccount( RestFacade facade )
    {
        this.facade = facade;
    }

    /**
     * Constructs local account. If no Account ID set then can't be saved.
     *
     * @param builder mandatory properties are: email, identityId, audience
     */
    LocalAccount( @Nonnull LocalAccountProvider.Builder builder )
    {
        checkNotNull( builder, "Builder can't be null" );
        this.email = checkNotNull( builder.getEmail(), "Account email is mandatory" );
        this.audience = checkNotNull( builder.getAudience(), "Account audience is mandatory" );
        this.identityId = checkNotNull( builder.getIdentityId(), "Account Identity ID is mandatory" );
        super.setId( builder.getAccountId() );
    }

    public LocalAccount( @Nonnull Account account )
    {
        checkNotNull( account, "Account is mandatory" );

        super.setId( checkNotNull( account.getId(), "Account ID is mandatory" ) );
        this.email = checkNotNull( account.getEmail(), "Account email is mandatory" );
        this.audience = checkNotNull( account.getAudience(), "Account audience is mandatory" );
        this.identityId = checkNotNull( account.getIdentityId(), "Account Identity ID is mandatory" );
        this.tAccount = account;
        init( account );
    }

    private void init( @Nonnull Account account )
    {
        this.locale = account.getLocale();

        AccountBusiness business = account.getBusiness();
        if ( business != null )
        {
            this.domicile = business.getDomicile();
        }

        String zoneId = account.getZoneId();
        if ( Strings.isNullOrEmpty( zoneId ) )
        {
            this.zone = DEFAULT_ZONE;
        }
        else
        {
            this.zone = zoneId;
        }
    }

    /**
     * The email account unique identification within third-party login provider.
     *
     * @return the unique identification of the email account
     */
    public String getIdentityId()
    {
        return identityId;
    }

    /**
     * The login email address of the account.
     *
     * @return the account login email
     */
    public String getEmail()
    {
        return email;
    }

    /**
     * Sets the login email address of the account.
     * <p>
     * In some (probably rare) cases an user might change its login email under the umbrella
     * of the same login provider. In this case the identity Id remains same,
     * while email has been changed.
     *
     * @param email the email to be set, not null
     */
    void setEmail( @Nonnull String email )
    {
        this.email = checkNotNull( email, "Login email can't be null" );
    }

    /**
     * The audience unique identification. The user identified by login email address
     * within one audience is a different user within another audience even with the same login email.
     * Those users have different Account.IDs.
     **/
    public String getAudience()
    {
        return audience;
    }

    /**
     * Retrieves remote account identified by {@link #getEmail()}.
     * Authentication against microservice is via service account.
     *
     * @return the account or {@code null} if not found
     */
    public Account getRemote()
    {
        if ( tAccount != null )
        {
            return tAccount;
        }

        try
        {
            tAccount = facade.get( Account.class )
                    .identifiedBy( String.valueOf( getId() ) )
                    .onBehalfOf( this )
                    .finish();
        }
        catch ( NotFoundException e )
        {
            LOGGER.warn( "The remote account with email: '" + getEmail() + "' not found." );
            tAccount = null;
        }
        return tAccount;
    }

    /**
     * Returns remote {@link Account} instance otherwise exception will be thrown.
     *
     * @return the remote account business instance
     * @throws NoRetryException if remote account not found
     * @see #getRemote()
     */
    public Account getAccount()
    {
        Account account = getRemote();
        if ( account == null )
        {
            String message = "The remote account " + this + " not found.";
            throw new NoRetryException( message );
        }

        return account;
    }

    /**
     * Returns {@link AccountBusiness} instance otherwise exception will be thrown.
     *
     * @return the remote account business instance
     * @throws NoRetryException if not configured yet
     */
    public AccountBusiness getBusiness()
    {
        Account account = getRemote();
        if ( account == null )
        {
            String message = "The remote account " + this + " not found.";
            throw new NoRetryException( message );
        }

        AccountBusiness business = account.getBusiness();
        if ( business == null )
        {
            String message = "Account " + this + " has no business setup yet.";
            throw new NoRetryException( message );
        }

        return business;
    }

    /**
     * Returns the account time-zone ID, such as Europe/Paris. Used to identify the rules
     * how to render date time properties of the resources associated with this account.
     *
     * @return the time-zone ID
     */
    public ZoneId getZoneId()
    {
        return ZoneId.of( checkNotNull( zone, "LocalAccount.zone property can't be null" ) );
    }

    /**
     * Sets the time-zone ID.
     *
     * @param zone the time-zone ID to be set, not null
     */
    void setZoneId( @Nonnull String zone )
    {
        this.zone = checkNotNull( zone, "Zone ID can't be null" );
    }

    /**
     * Returns the account locale. Always returns a value.
     * If none of the values has been found a {@link #DEFAULT_LOCALE} will be returned.
     *
     * @return the final locale, ISO 639 alpha-2 or alpha-3 language code
     */
    public Locale getLocale()
    {
        return getLocale( null );
    }

    /**
     * Sets the preferred account language. ISO 639 alpha-2 or alpha-3 language code.
     *
     * @param locale the language to be set
     */
    void setLocale( String locale )
    {
        this.locale = locale;
    }

    /**
     * Returns the account locale with specified preference.
     * Always returns a value. If none of the values has been found a {@link #DEFAULT_LOCALE} will be returned.
     *
     * @param locale the optional however preferred language
     * @return the final locale, ISO 639 alpha-2 or alpha-3 language code
     */
    public Locale getLocale( @Nullable Locale locale )
    {
        return locale != null ? locale : convertJavaLocale( this.locale, DEFAULT_LOCALE );
    }

    /**
     * Returns the account domicile with optional preference. Always returns a value.
     * If none domicile value found a {@link #DEFAULT_DOMICILE} will be returned.
     *
     * @param domicile the optional (preferred) ISO 3166 alpha-2 country code that represents a target domicile
     * @return the account domicile or default
     * @throws IllegalArgumentException if domicile value is none of the supported {@link Domicile}
     */
    public Domicile getDomicile( @Nullable String domicile )
    {
        if ( domicile == null )
        {
            if ( Strings.isNullOrEmpty( this.domicile ) )
            {
                domicile = DEFAULT_DOMICILE;
                LOGGER.warn( "Using service default locale: " + domicile );
            }
            else
            {
                domicile = this.domicile;
            }
        }
        return Domicile.valueOf( domicile.toUpperCase() );
    }

    /**
     * Returns the account domicile. Always returns a value.
     * If none domicile value found a {@link #DEFAULT_DOMICILE} will be returned.
     *
     * @return the account domicile or default
     * @throws IllegalArgumentException if domicile value is none of the supported {@link Domicile}
     */
    public Domicile getDomicile()
    {
        return getDomicile( null );
    }

    /**
     * Sets the ISO 3166 alpha-2 country code that represents account domicile.
     *
     * @param domicile the domicile to be set
     */
    void setDomicile( String domicile )
    {
        this.domicile = domicile;
    }

    @Override
    public void save()
    {
        if ( getId() == null )
        {
            String msg = "The Account ID is being expected to be set in advance from remote Account.";
            throw new IllegalArgumentException( msg );
        }
        ofy().transact( () -> ofy().save().entity( this ).now() );
    }

    @Override
    public void delete()
    {
        ofy().transact( () -> ofy().delete().entity( LocalAccount.this ).now() );
    }

    @Override
    protected long getModelVersion()
    {
        //21.10.2017 08:00:00 GMT+0200
        return 1508565600000L;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( !( o instanceof LocalAccount ) ) return false;
        LocalAccount that = ( LocalAccount ) o;
        return Objects.equals( this.getId(), that.getId() );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( getId() );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
                .add( "id", getId() )
                .add( "email", email )
                .add( "identityId", identityId )
                .add( "audience", audience )
                .add( "domicile", domicile )
                .add( "zone", zone )
                .add( "locale", locale )
                .toString();
    }
}
