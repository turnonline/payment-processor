package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.account.client.model.Account;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;

import javax.annotation.Nonnull;

/**
 * The dedicated provider to handle local and remote account.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public interface LocalAccountProvider
{
    /**
     * Returns the associated local account entity instance.
     * LightAccount instance accessed for the first time is being stored in datastore (identified by email account
     * unique identification within third-party provider system). LocalAccount acts as an owner of the entities
     * associated with an account.
     *
     * @param account the authenticated remote account
     * @return the associated local account
     */
    LocalAccount getAssociatedLightAccount( @Nonnull Account account );
}
