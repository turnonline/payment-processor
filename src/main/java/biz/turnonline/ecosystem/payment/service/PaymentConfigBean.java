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

import biz.turnonline.ecosystem.billing.model.InvoicePayment;
import biz.turnonline.ecosystem.payment.api.ApiValidationException;
import biz.turnonline.ecosystem.payment.api.model.Certificate;
import biz.turnonline.ecosystem.payment.oauth.RevolutCertMetadata;
import biz.turnonline.ecosystem.payment.oauth.RevolutCredentialAdministration;
import biz.turnonline.ecosystem.payment.service.model.BankCode;
import biz.turnonline.ecosystem.payment.service.model.BeneficiaryBankAccount;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import nl.garvelink.iban.IBAN;
import org.ctoolkit.services.storage.EntityExecutor;
import org.ctoolkit.services.storage.HasOwner;
import org.ctoolkit.services.storage.criteria.Criteria;
import org.ctoolkit.services.task.TaskExecutor;
import org.iban4j.BicFormatException;
import org.iban4j.BicUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Payment configuration and execution implementation.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
class PaymentConfigBean
        implements PaymentConfig
{
    private static final Logger LOGGER = LoggerFactory.getLogger( PaymentConfigBean.class );

    private static final String TEMPLATE = "{0} cannot be null";

    private final EntityExecutor datastore;

    private final CodeBook codeBook;

    private final TaskExecutor executor;

    private final RevolutCredentialAdministration revolut;

    @Inject
    PaymentConfigBean( EntityExecutor datastore,
                       CodeBook codeBook,
                       TaskExecutor executor,
                       RevolutCredentialAdministration revolut )
    {
        this.datastore = datastore;
        this.codeBook = codeBook;
        this.executor = executor;
        this.revolut = revolut;
    }

    @Override
    public Certificate enableApiAccess( @Nonnull LocalAccount owner,
                                        @Nonnull String bank,
                                        @Nonnull Certificate certificate )
    {
        checkNotNull( owner, TEMPLATE, "LocalAccount" );
        checkNotNull( bank, TEMPLATE, "Bank code" );
        checkNotNull( certificate, TEMPLATE, "Certificate" );

        BankCode bankCode = codeBook.getBankCode( owner, bank.toUpperCase(), null, null );
        if ( bankCode == null )
        {
            throw new BankCodeNotFound( bank );
        }

        if ( REVOLUT_BANK_CODE.equals( bankCode.getCode() ) )
        {
            RevolutCertMetadata metadata = revolut.get();
            Optional<String> clientId = Optional.ofNullable( certificate.getClientId() );
            clientId.ifPresent( metadata::setClientId );

            Optional<String> keyName = Optional.ofNullable( certificate.getKeyName() );
            keyName.ifPresent( metadata::setKeyName );

            if ( clientId.isPresent() || keyName.isPresent() )
            {
                metadata.save();
            }

            executor.schedule( new RevolutDebtorBankAccountsInit( owner.entityKey() ) );

            return new Certificate()
                    .accessAuthorised( metadata.isAccessAuthorised() )
                    .authorisedOn( metadata.getAuthorisedOn() )
                    .clientId( metadata.getClientId() )
                    .keyName( metadata.getKeyName() );
        }
        else
        {
            String key = "errors.validation.bankAccount.bankCode.onboard.unsupported";
            throw ApiValidationException.prepare( key, bank );
        }
    }

    @Override
    public CompanyBankAccount getBankAccount( @Nonnull LocalAccount account, @Nonnull Long id )
    {
        checkNotNull( account, TEMPLATE, "LocalAccount" );

        CompanyBankAccount bankAccount = loadBankAccount( checkNotNull( id, TEMPLATE, "Bank account ID" ) );
        if ( bankAccount == null )
        {
            throw new BankAccountNotFound( id );
        }
        return checkOwner( account, bankAccount );
    }

    private CompanyBankAccount loadBankAccount( @Nonnull Long id )
    {
        return ofy().load().type( CompanyBankAccount.class ).id( id ).now();
    }

    @Override
    public void insert( @Nonnull LocalAccount account, @Nonnull CompanyBankAccount bankAccount )
    {
        checkNotNull( account, TEMPLATE, "LocalAccount" );

        if ( checkNotNull( bankAccount, TEMPLATE, CompanyBankAccount.class.getSimpleName() ).getId() != null )
        {
            String message = bankAccount.entityKey() + " should be in memory instance only, not persisted object.";
            throw new IllegalArgumentException( message );
        }

        bankAccount.setOwner( account );
        bankAccount.save();
    }

    @Override
    public List<CompanyBankAccount> getBankAccounts( @Nonnull LocalAccount account,
                                                     @Nullable Integer offset,
                                                     @Nullable Integer limit,
                                                     @Nullable String country,
                                                     @Nullable String bankCode )
    {
        return internalGetBankAccounts( account, offset, limit, country, bankCode );
    }

    @Override
    public List<CompanyBankAccount> getBankAccounts( @Nonnull LocalAccount owner, @Nonnull String bank )
    {
        checkNotNull( bank, TEMPLATE, "Bank code" );
        return internalGetBankAccounts( owner, null, null, null, bank );
    }

    @VisibleForTesting
    List<CompanyBankAccount> internalGetBankAccounts( @Nonnull LocalAccount account,
                                                      @Nullable Integer offset,
                                                      @Nullable Integer limit,
                                                      @Nullable String country,
                                                      @Nullable String bankCode )
    {
        checkNotNull( account, "LocalAccount cannot be null" );
        Criteria<CompanyBankAccount> criteria = Criteria.of( CompanyBankAccount.class );

        criteria.reference( "owner", account );
        if ( country != null )
        {
            criteria.equal( "country", country.toUpperCase() );
        }

        if ( bankCode != null )
        {
            criteria.equal( "bankCode", bankCode.toUpperCase() );
        }

        if ( offset != null )
        {
            if ( limit == null )
            {
                limit = 10;
            }
            criteria.offset( offset );
            criteria.limit( limit );
        }

        List<CompanyBankAccount> list = datastore.list( criteria );
        LOGGER.info( list.size() + " bank accounts has been found." );

        return list;
    }

    @Override
    public void update( @Nonnull LocalAccount account, @Nonnull CompanyBankAccount bankAccount )
    {
        checkNotNull( account, TEMPLATE, "LocalAccount" );

        CompanyBankAccount checked = checkOwner( account, checkNotNull( bankAccount, TEMPLATE, "BankAccount" ) );
        checked.save();
    }

    @Override
    public CompanyBankAccount deleteBankAccount( @Nonnull LocalAccount account, @Nonnull Long id )
    {
        // TODO remove payment gate from account config; old: config.removePaymentGateway( bankAccount.getPaymentGate() );
        CompanyBankAccount bankAccount = getBankAccount(
                checkNotNull( account, TEMPLATE, "LocalAccount" ),
                checkNotNull( id, TEMPLATE, "Bank account ID" ) );

        if ( bankAccount.isPrimary() )
        {
            String key = "errors.validation.bankAccount.deletion.primary";
            throw ApiValidationException.prepare( key, bankAccount.getId() );
        }
        else
        {
            bankAccount.delete();
        }

        return bankAccount;
    }

    @Override
    public CompanyBankAccount markBankAccountAsPrimary( @Nonnull LocalAccount account, @Nonnull Long id )
    {
        CompanyBankAccount bankAccount = getBankAccount(
                checkNotNull( account, TEMPLATE, "LocalAccount" ),
                checkNotNull( id, TEMPLATE, "Bank account ID" ) );

        // mark old primary bank accounts to not primary
        List<CompanyBankAccount> bankAccounts = internalGetBankAccounts( account, null, null, null, null );

        Collection<CompanyBankAccount> primary = bankAccounts.stream().filter( new BankAccountPrimary() ).collect( Collectors.toList() );

        ofy().transact( () -> {
            for ( CompanyBankAccount primaryBankAccount : primary )
            {
                primaryBankAccount.setPrimary( false );
                primaryBankAccount.save();
            }

            // mark new bank account as primary
            bankAccount.setPrimary( true );
            bankAccount.save();
        } );

        return bankAccount;
    }

    @Override
    public CompanyBankAccount getPrimaryBankAccount( @Nonnull LocalAccount account, @Nullable String country )
    {
        CompanyBankAccount primary = getInternalPrimaryBankAccount( account, country );
        if ( primary == null )
        {
            throw new BankAccountNotFound( -1L );
        }
        return primary;
    }

    @Override
    public CompanyBankAccount getDebtorBankAccount( @Nonnull LocalAccount debtor, @Nonnull InvoicePayment payment )
    {
        return getInternalPrimaryBankAccount( debtor, null );
    }

    CompanyBankAccount getInternalPrimaryBankAccount( @Nonnull LocalAccount account, @Nullable String country )
    {
        List<CompanyBankAccount> list = internalGetBankAccounts( checkNotNull( account, TEMPLATE, "LocalAccount" ), null, null, null, null );
        Collections.sort( list );

        country = country == null ? account.getDomicile().name() : country;

        Predicate<CompanyBankAccount> and = new BankAccountCountryPredicate( country ).and( new BankAccountPrimary() );
        Collection<CompanyBankAccount> filtered = list.stream().filter( and ).collect( Collectors.toList() );

        CompanyBankAccount bankAccount;

        if ( filtered.isEmpty() )
        {
            // no match yet, thus return any bank account marked as primary
            bankAccount = list.stream().filter( new BankAccountPrimary() ).findFirst().orElse( null );
        }
        else
        {
            bankAccount = filtered.iterator().next();
        }

        return bankAccount;
    }

    @Override
    public List<CompanyBankAccount> getAlternativeBankAccounts( @Nonnull LocalAccount account,
                                                                @Nullable Integer offset,
                                                                @Nullable Integer limit,
                                                                @Nullable Locale locale,
                                                                @Nullable String country )
    {
        CompanyBankAccount exclude = getInternalPrimaryBankAccount( checkNotNull( account, TEMPLATE, "LocalAccount" ), country );
        country = country == null ? account.getDomicile().name() : country;
        locale = account.getLocale( locale );

        List<CompanyBankAccount> list = internalGetBankAccounts( account, offset, limit, null, null );
        list.sort( new BankAccountSellerSorting( country ) );
        Iterator<CompanyBankAccount> iterator = list.iterator();

        List<CompanyBankAccount> filtered = new ArrayList<>();

        while ( iterator.hasNext() )
        {
            CompanyBankAccount next = iterator.next();

            if ( exclude == null || !exclude.equals( next ) )
            {
                String description = next.getLocalizedLabel( locale );
                if ( description != null )
                {
                    filtered.add( next );
                }
            }
        }

        return filtered;
    }

    @Override
    public BeneficiaryBankAccount insertBeneficiary( @Nonnull LocalAccount owner,
                                                     @Nonnull String iban,
                                                     @Nonnull String bic,
                                                     @Nonnull String currency )
    {
        checkNotNull( currency, "Currency can't be null" );

        try
        {
            BicUtil.validate( bic );
        }
        catch ( BicFormatException e )
        {
            throw new IllegalArgumentException( "'" + bic + "' " + e.getMessage() );
        }

        BeneficiaryBankAccount beneficiary;
        if ( isBeneficiary( owner, iban ) )
        {
            LOGGER.info( "Beneficiary bank account with IBAN: "
                    + iban
                    + " already exist at Owner: "
                    + owner.getId() );

            beneficiary = getBeneficiary( owner, iban );
        }
        else
        {
            beneficiary = new BeneficiaryBankAccount( codeBook );
            beneficiary.setOwner( owner );
            beneficiary.setIban( iban );
            beneficiary.setBic( bic );
            beneficiary.setCurrency( currency );
            beneficiary.save();
        }

        return beneficiary;
    }

    @Override
    public BeneficiaryBankAccount getBeneficiary( @Nonnull LocalAccount owner, @Nonnull String iban )
    {
        Criteria<BeneficiaryBankAccount> criteria = beneficiaryQuery( owner, iban );
        return datastore.first( criteria );
    }

    @Override
    public boolean isBeneficiary( @Nonnull LocalAccount owner, @Nonnull String iban )
    {
        Criteria<BeneficiaryBankAccount> criteria = beneficiaryQuery( owner, iban );
        return datastore.count( criteria ) > 0;
    }

    private Criteria<BeneficiaryBankAccount> beneficiaryQuery( @Nonnull LocalAccount owner, @Nonnull String iban )
    {
        checkNotNull( owner, "LocalAccount can't be null" );
        checkNotNull( iban, "IBAN can't be null" );

        Criteria<BeneficiaryBankAccount> criteria = Criteria.of( BeneficiaryBankAccount.class );
        // make IBAN compact, without formatting
        criteria.equal( "iban", IBAN.valueOf( iban ).toPlainString() );
        criteria.reference( "owner", owner );

        return criteria;
    }

    /**
     * Checks whether entity has the same owner as the authenticated account.
     * If yes, the input entity instance will be returned, otherwise exception will be thrown.
     *
     * @param account the authenticated account
     * @param entity  the entity to be manipulated
     * @return the input entity
     * @throws WrongEntityOwner if entity has a different owner as the authenticated account
     */
    private <T extends HasOwner<LocalAccount>> T checkOwner( @Nonnull LocalAccount account, @Nonnull T entity )
    {
        LocalAccount owner = checkNotNull( entity, "Entity cannot be null" ).getOwner();
        checkNotNull( owner );

        if ( !entity.checkOwner( account ) )
        {
            WrongEntityOwner wrongOwnerException = new WrongEntityOwner( account, entity );
            LOGGER.warn( wrongOwnerException.getMessage() );
            throw wrongOwnerException;
        }
        return entity;
    }

    static class BankAccountPrimary
            implements Predicate<CompanyBankAccount>
    {
        @Override
        public boolean test( @Nullable CompanyBankAccount input )
        {
            return input != null && input.isPrimary();
        }
    }

    private static class BankAccountCountryPredicate
            implements Predicate<CompanyBankAccount>
    {
        private String countryCode;

        BankAccountCountryPredicate( String countryCode )
        {
            this.countryCode = checkNotNull( countryCode );
        }

        @Override
        public boolean test( @Nullable CompanyBankAccount input )
        {
            return input != null && countryCode.equals( input.getCountry() );
        }
    }

    private static class BankAccountSellerSorting
            implements Comparator<CompanyBankAccount>, Serializable
    {
        private static final long serialVersionUID = 1L;

        private String country;

        BankAccountSellerSorting( @Nonnull String country )
        {
            this.country = checkNotNull( country );
        }

        @Override
        public int compare( CompanyBankAccount left, CompanyBankAccount right )
        {
            if ( country.contentEquals( right.getCountry() ) )
            {
                return 0;
            }
            return ComparisonChain.start()
                    .compare( left.getCountry(), right.getCountry(), Ordering.natural().nullsLast() )
                    .result();
        }
    }
}
