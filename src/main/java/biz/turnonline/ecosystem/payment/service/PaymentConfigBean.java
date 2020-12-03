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

import biz.turnonline.ecosystem.billing.model.BillPayment;
import biz.turnonline.ecosystem.billing.model.IncomingInvoice;
import biz.turnonline.ecosystem.payment.api.ApiValidationException;
import biz.turnonline.ecosystem.payment.api.model.Certificate;
import biz.turnonline.ecosystem.payment.oauth.RevolutCertMetadata;
import biz.turnonline.ecosystem.payment.oauth.RevolutCredentialAdministration;
import biz.turnonline.ecosystem.payment.service.model.BankCode;
import biz.turnonline.ecosystem.payment.service.model.BeneficiaryBankAccount;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction.State;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.FormOfPayment;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.payment.service.model.PaymentLocalAccount;
import biz.turnonline.ecosystem.payment.service.model.TransactionInvoice;
import biz.turnonline.ecosystem.payment.service.model.TransactionReceipt;
import biz.turnonline.ecosystem.payment.service.revolut.RevolutDebtorBankAccountsInit;
import com.google.cloud.ServiceOptions;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import nl.garvelink.iban.IBAN;
import org.ctoolkit.services.storage.EntityExecutor;
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

import static biz.turnonline.ecosystem.payment.service.PaymentConfig.Operation.BOTH;
import static biz.turnonline.ecosystem.payment.service.PaymentConfig.Operation.CREDIT;
import static biz.turnonline.ecosystem.payment.service.PaymentConfig.Operation.DEBIT;
import static biz.turnonline.ecosystem.payment.service.PaymentConfig.Operation.valueOf;
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

    private static final String TEMPLATE = "%s cannot be null";

    private final EntityExecutor datastore;

    private final CodeBook codeBook;

    private final TaskExecutor executor;

    private final RevolutCredentialAdministration revolut;

    private final LocalAccountProvider lap;

    @Inject
    PaymentConfigBean( EntityExecutor datastore,
                       CodeBook codeBook,
                       TaskExecutor executor,
                       RevolutCredentialAdministration revolut,
                       LocalAccountProvider lap )
    {
        this.datastore = datastore;
        this.codeBook = codeBook;
        this.executor = executor;
        this.revolut = revolut;
        this.lap = lap;
    }

    @Override
    public Certificate enableApiAccess( @Nonnull LocalAccount owner,
                                        @Nonnull String bank,
                                        @Nonnull Certificate certificate )
    {
        checkNotNull( owner, TEMPLATE, "LocalAccount" );
        checkNotNull( bank, TEMPLATE, "Bank code" );
        checkNotNull( certificate, TEMPLATE, "Certificate" );

        BankCode bankCode = codeBook.getBankCode( bank.toUpperCase(), null, null );
        if ( bankCode == null )
        {
            throw new BankCodeNotFound( bank );
        }

        if ( datastore.count( Criteria.of( PaymentLocalAccount.class ) ) == 0 )
        {
            new PaymentLocalAccount( owner, ServiceOptions.getDefaultProjectId() ).save();
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

            // Init bank accounts only Client ID is present, otherwise initialization will fail
            if ( !Strings.isNullOrEmpty( metadata.getClientId() ) )
            {
                executor.schedule( new RevolutDebtorBankAccountsInit( owner.entityKey() ) );
            }

            return new Certificate()
                    .accessAuthorised( metadata.getAuthorisedOn() != null )
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
    public CompanyBankAccount getBankAccount( @Nonnull Long id )
    {
        CompanyBankAccount bankAccount = loadBankAccount( checkNotNull( id, TEMPLATE, "Bank account ID" ) );
        if ( bankAccount == null )
        {
            throw new BankAccountNotFound( id );
        }
        return bankAccount;
    }

    @Override
    public CompanyBankAccount getBankAccount( @Nonnull String externalId )
    {
        checkNotNull( externalId, TEMPLATE, "External Id" );

        Criteria<CompanyBankAccount> criteria = Criteria.of( CompanyBankAccount.class );
        for ( CompanyBankAccount bankAccount : datastore.list( criteria ) )
        {
            if ( externalId.equals( bankAccount.getExternalId() ) )
            {
                return bankAccount;
            }
        }

        return null;
    }

    private CompanyBankAccount loadBankAccount( @Nonnull Long id )
    {
        return ofy().load().type( CompanyBankAccount.class ).id( id ).now();
    }

    @Override
    public List<CompanyBankAccount> getBankAccounts( @Nullable Integer offset,
                                                     @Nullable Integer limit,
                                                     @Nullable String country,
                                                     @Nullable String bankCode )
    {
        return internalGetBankAccounts( offset, limit, country, bankCode );
    }

    @Override
    public List<CompanyBankAccount> getBankAccounts( @Nonnull String bank )
    {
        checkNotNull( bank, TEMPLATE, "Bank code" );
        return internalGetBankAccounts( null, null, null, bank );
    }

    @VisibleForTesting
    List<CompanyBankAccount> internalGetBankAccounts( @Nullable Integer offset,
                                                      @Nullable Integer limit,
                                                      @Nullable String country,
                                                      @Nullable String bankCode )
    {
        Criteria<CompanyBankAccount> criteria = Criteria.of( CompanyBankAccount.class );

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
    public CompanyBankAccount deleteBankAccount( @Nonnull Long id )
    {
        CompanyBankAccount bankAccount = getBankAccount( checkNotNull( id, TEMPLATE, "Bank account ID" ) );

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
    public CompanyBankAccount markBankAccountAsPrimary( @Nonnull Long id )
    {
        CompanyBankAccount bankAccount = getBankAccount( checkNotNull( id, TEMPLATE, "Bank account ID" ) );

        // mark old primary bank accounts to not primary
        List<CompanyBankAccount> bankAccounts = internalGetBankAccounts( null, null, null, null );

        Collection<CompanyBankAccount> primary = bankAccounts
                .stream()
                .filter( new BankAccountPrimary() )
                .collect( Collectors.toList() );

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
    public CompanyBankAccount getPrimaryBankAccount( @Nullable String country )
    {
        CompanyBankAccount primary = getInternalPrimaryBankAccount( country );
        if ( primary == null )
        {
            throw new BankAccountNotFound( -1L );
        }
        return primary;
    }

    @Override
    public CompanyBankAccount getDebtorBankAccount( @Nonnull BillPayment payment )
    {
        return getInternalPrimaryBankAccount( null );
    }

    CompanyBankAccount getInternalPrimaryBankAccount( @Nullable String country )
    {
        LocalAccount account = checkNotNull( lap.get(), TEMPLATE, "LocalAccount" );
        List<CompanyBankAccount> list = internalGetBankAccounts( null, null, null, null );
        Collections.sort( list );

        country = country == null ? account.getDomicile().name() : country;

        Predicate<CompanyBankAccount> and = new BankAccountCountryPredicate( country ).and( new BankAccountPrimary() );
        Collection<CompanyBankAccount> filtered = list
                .stream()
                .filter( and )
                .collect( Collectors.toList() );

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
    public List<CompanyBankAccount> getAlternativeBankAccounts( @Nullable Integer offset,
                                                                @Nullable Integer limit,
                                                                @Nullable Locale locale,
                                                                @Nullable String country )
    {
        LocalAccount account = checkNotNull( lap.get(), TEMPLATE, "LocalAccount" );
        CompanyBankAccount exclude = getInternalPrimaryBankAccount( country );
        country = country == null ? account.getDomicile().name() : country;
        locale = account.getLocale( locale );

        List<CompanyBankAccount> list = internalGetBankAccounts( offset, limit, null, null );
        list.sort( new BankAccountSellerSorting( country ) );
        Iterator<CompanyBankAccount> iterator = list.iterator();

        List<CompanyBankAccount> filtered = new ArrayList<>();

        while ( iterator.hasNext() )
        {
            CompanyBankAccount next = iterator.next();

            if ( exclude == null || !exclude.equals( next ) )
            {
                String description = next.getLocalizedLabel( locale, account );
                if ( description != null )
                {
                    filtered.add( next );
                }
            }
        }

        return filtered;
    }

    @Override
    public BeneficiaryBankAccount insertBeneficiary( @Nonnull String iban,
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
        if ( isBeneficiary( iban ) )
        {
            LOGGER.info( "Beneficiary bank account with IBAN: " + iban + " already exist" );

            beneficiary = getBeneficiary( iban );
        }
        else
        {
            beneficiary = new BeneficiaryBankAccount( codeBook );
            beneficiary.setIban( iban );
            beneficiary.setBic( bic );
            beneficiary.setCurrency( currency );
            beneficiary.save();
        }

        return beneficiary;
    }

    @Override
    public BeneficiaryBankAccount getBeneficiary( @Nonnull String iban )
    {
        Criteria<BeneficiaryBankAccount> criteria = beneficiaryQuery( iban );
        return datastore.first( criteria );
    }

    @Override
    public boolean isBeneficiary( @Nonnull String iban )
    {
        Criteria<BeneficiaryBankAccount> criteria = beneficiaryQuery( iban );
        return datastore.count( criteria ) > 0;
    }

    @Override
    public CommonTransaction initGetTransactionDraft( @Nonnull IncomingInvoice invoice )
    {
        checkNotNull( invoice, "Incoming invoice cannot be null" );

        Long orderId = invoice.getOrderId();
        Long invoiceId = invoice.getId();

        Criteria<TransactionInvoice> criteria = Criteria.of( TransactionInvoice.class );
        criteria.equal( "orderId", orderId );
        criteria.equal( "invoiceId", invoiceId );

        List<TransactionInvoice> list = datastore.list( criteria );
        TransactionInvoice transaction;

        if ( list.isEmpty() )
        {
            transaction = new TransactionInvoice( orderId, invoiceId );
            transaction.save();
        }
        else
        {
            transaction = list.get( 0 );
            if ( list.size() > 1 )
            {
                LOGGER.warn( "Expected only single transaction, got " + list.size() );
            }
        }

        return transaction;
    }

    @Override
    public CommonTransaction initGetTransaction( @Nonnull String extId )
    {
        CommonTransaction transaction;
        try
        {
            transaction = searchTransaction( extId );
        }
        catch ( TransactionNotFound e )
        {
            // If the transaction record not found, the invoice hasn't been issued
            // and the incoming transaction represents an expenses paid outside of the service.
            transaction = new TransactionReceipt( extId );
            transaction.save();
        }

        return transaction;
    }

    @Override
    public CommonTransaction searchTransaction( @Nonnull String extId )
    {
        Criteria<CommonTransaction> criteria = Criteria.of( CommonTransaction.class );
        criteria.equal( "extId", extId );
        List<CommonTransaction> transactions = datastore.list( criteria );

        CommonTransaction transaction;
        if ( !transactions.isEmpty() )
        {
            transaction = transactions.get( 0 );

            int size = transactions.size();
            if ( size > 1 )
            {
                LOGGER.warn( "There are more transactions for single Ext ID: " + extId + ", number of records " + size );
            }
        }
        else
        {
            throw new TransactionNotFound( extId );
        }

        return transaction;
    }

    @Override
    public List<CommonTransaction> filterTransactions( @Nonnull Filter filter )
    {
        Integer offset = filter.getOffset();
        Integer limit = filter.getLimit();

        if ( offset != null && offset < 0 )
        {
            throw ApiValidationException.prepare( "errors.validation.query.offset.invalid", offset );
        }
        if ( limit != null && limit < 0 )
        {
            throw ApiValidationException.prepare( "errors.validation.query.limit.invalid", limit );
        }

        Criteria<CommonTransaction> criteria = Criteria.of( CommonTransaction.class );
        criteria.descending( "createdDate" );

        try
        {
            String op = Strings.isNullOrEmpty( filter.getOperation() ) ? BOTH.name() : filter.getOperation();
            Operation operation = valueOf( op.toUpperCase() );

            if ( CREDIT == operation )
            {
                criteria.equal( "credit", true );
            }
            else if ( DEBIT == operation )
            {
                criteria.equal( "credit", false );
            }
        }
        catch ( IllegalArgumentException e )
        {
            String key = "errors.validation.query.operation.invalid";
            throw ApiValidationException.prepare( key, filter.getOperation() );
        }

        try
        {
            State status = Strings.isNullOrEmpty( filter.getStatus() ) ? null : State.valueOf( filter.getStatus() );
            if ( status != null )
            {
                criteria.equal( "status", filter.getStatus() );
            }
        }
        catch ( IllegalArgumentException e )
        {
            String key = "errors.validation.query.status.invalid";
            throw ApiValidationException.prepare( key, filter.getStatus() );
        }

        if ( filter.getCreatedDateFrom() != null )
        {
            criteria.ge( "createdDate", filter.getCreatedDateFrom() );
        }
        if ( filter.getCreatedDateTo() != null )
        {
            criteria.le( "createdDate", filter.getCreatedDateTo() );
        }

        Long accountId = filter.getAccountId();
        if ( accountId != null )
        {
            criteria.reference( "accountKey", CompanyBankAccount.class, accountId );
        }

        Long orderId = filter.getOrderId();
        Long invoiceId = filter.getInvoiceId();

        if ( orderId != null )
        {
            criteria.equal( "orderId", orderId );
            if ( invoiceId != null )
            {
                criteria.equal( "invoiceId", invoiceId );
            }
        }

        if ( offset != null )
        {
            criteria.offset( offset );
        }

        String type = Strings.isNullOrEmpty( filter.getType() ) ? null : filter.getType();
        FormOfPayment paymentType = null;
        if ( type != null )
        {
            try
            {
                paymentType = FormOfPayment.valueOf( type.toUpperCase() );
            }
            catch ( IllegalArgumentException e )
            {
                throw ApiValidationException.prepare( "errors.validation.query.paymentType.invalid", type );
            }
        }

        if ( paymentType != null )
        {
            criteria.equal( "type", paymentType );
        }

        criteria.limit( limit == null ? 20 : limit );

        List<CommonTransaction> list = datastore.list( criteria );
        LOGGER.info( list.size() + " transactions has found." );

        return list;
    }

    private Criteria<BeneficiaryBankAccount> beneficiaryQuery( @Nonnull String iban )
    {
        checkNotNull( iban, "IBAN can't be null" );

        Criteria<BeneficiaryBankAccount> criteria = Criteria.of( BeneficiaryBankAccount.class );
        // make IBAN compact, without formatting
        criteria.equal( "iban", IBAN.valueOf( iban ).toPlainString() );

        return criteria;
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
        private final String countryCode;

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

        private final String country;

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
