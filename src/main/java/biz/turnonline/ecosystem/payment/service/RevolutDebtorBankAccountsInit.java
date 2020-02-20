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

import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.revolut.business.account.model.Account;
import biz.turnonline.ecosystem.revolut.business.account.model.AccountBankDetailsItem;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.googlecode.objectify.Key;
import org.ctoolkit.restapi.client.Identifier;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.services.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static biz.turnonline.ecosystem.payment.service.PaymentConfig.REVOLUT_BANK_CODE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Initialization of the Revolut bank accounts, taken from the bank backend. Idempotent.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
class RevolutDebtorBankAccountsInit
        extends Task<LocalAccount>
{
    private static final long serialVersionUID = 8695929281304701916L;

    private static final Logger LOGGER = LoggerFactory.getLogger( RevolutDebtorBankAccountsInit.class );

    @Inject
    transient private RestFacade facade;

    @Inject
    transient private PaymentConfig config;

    @Inject
    transient private CodeBook codeBook;

    RevolutDebtorBankAccountsInit( @Nonnull Key<LocalAccount> accountKey )
    {
        super( "Init-Revolut-BankAccounts" );
        super.setEntityKey( checkNotNull( accountKey, "LocalAccount key can't be null" ) );
    }

    private static boolean inclPublicActive( Account account )
    {
        if ( ( account.getPublic() == null ? false : account.getPublic() )
                && account.getState() == Account.StateEnum.ACTIVE )
        {
            return true;
        }
        else
        {
            LOGGER.warn( "Revolut bank account import skipped, not public or inactive " + account );
            return false;
        }
    }

    @Override
    protected void execute()
    {
        LocalAccount owner = workWith();

        List<Account> accounts = facade.list( Account.class ).finish();

        if ( accounts == null || accounts.isEmpty() )
        {
            LOGGER.warn( "Revolut bank accounts not found at all for owner identified by '" + owner.getId() + "'" );
            return;
        }

        List<CompanyBankAccount> existing = config.getBankAccounts( owner, REVOLUT_BANK_CODE );
        List<CompanyBankAccount> bankAccounts = new ArrayList<>();
        CompanyBankAccount bankAccount;
        String currency;

        for ( Account next : accounts.stream()
                .filter( RevolutDebtorBankAccountsInit::inclPublicActive )
                .collect( Collectors.toList() ) )
        {
            String accountId = next.getId().toString();
            Identifier ofAccount = new Identifier( accountId );
            List<AccountBankDetailsItem> details = facade.list( AccountBankDetailsItem.class, ofAccount ).finish();

            for ( AccountBankDetailsItem detail : details )
            {
                // exclude if missing IBAN
                String iban = detail.getIban();
                if ( !Strings.isNullOrEmpty( iban ) )
                {
                    currency = next.getCurrency();

                    // init or get existing
                    bankAccount = initGet( existing, currency );
                    bankAccount.setIban( iban );
                    bankAccount.setBic( detail.getBic() );
                    bankAccount.setCurrency( currency );
                    bankAccount.setExternalId( accountId );
                    bankAccount.setOwner( owner );
                    bankAccounts.add( bankAccount );

                    break;
                }
            }
        }

        if ( !bankAccounts.isEmpty() )
        {
            save( bankAccounts );
        }

        LOGGER.info( "Number of Revolut bank accounts synced for owner identified by '"
                + owner.getId()
                + "' are: "
                + bankAccounts.size() );
    }

    @VisibleForTesting
    void save( List<CompanyBankAccount> bankAccounts )
    {
        ofy().transact( () -> bankAccounts.forEach( CompanyBankAccount::save ) );
    }

    /**
     * Returns bank account instance, either an existing one or creates a new instance.
     */
    private CompanyBankAccount initGet( @Nonnull List<CompanyBankAccount> existing,
                                        @Nullable String currency )
    {
        return existing.stream()
                .filter( cba -> currency != null && currency.equals( cba.getCurrency() ) )
                .findFirst()
                .orElse( new CompanyBankAccount( codeBook ) );
    }
}
