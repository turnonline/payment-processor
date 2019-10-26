package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.payment.api.ApiValidationException;
import biz.turnonline.ecosystem.payment.service.model.BankAccount;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import org.ctoolkit.services.storage.EntityExecutor;
import org.ctoolkit.services.storage.HasOwner;
import org.ctoolkit.services.storage.criteria.Criteria;
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
    private static final Logger logger = LoggerFactory.getLogger( PaymentConfigBean.class );

    private final EntityExecutor datastore;

    @Inject
    PaymentConfigBean( EntityExecutor datastore )
    {
        this.datastore = datastore;
    }

    @Override
    public BankAccount getBankAccount( @Nonnull LocalAccount account, @Nonnull Long id )
    {
        String template = "{0} cannot be null";
        checkNotNull( account, template, "LocalAccount" );

        BankAccount bankAccount = loadBankAccount( checkNotNull( id, template, "Bank account ID" ) );
        return checkOwner( account, bankAccount );
    }

    private BankAccount loadBankAccount( @Nonnull Long id )
    {
        BankAccount bankAccount = ofy().load().type( BankAccount.class ).id( id ).now();
        if ( bankAccount == null )
        {
            throw new BankAccountNotFound( id );
        }
        return bankAccount;
    }

    @Override
    public void insertBankAccount( @Nonnull LocalAccount account, @Nonnull BankAccount bankAccount )
    {
        String template = "{0} cannot be null";
        checkNotNull( account, template, "LocalAccount" );

        if ( checkNotNull( bankAccount, template, BankAccount.class.getSimpleName() ).getId() != null )
        {
            String message = bankAccount.entityKey() + " should be in memory instance only, not persisted object.";
            throw new IllegalArgumentException( message );
        }

        bankAccount.setOwner( account );

        // generate code
        int codeOrder = 1;
        List<BankAccount> list = getBankAccounts( account, null, null, null );
        list.sort( new BankAccountCodeDescending() );

        if ( !list.isEmpty() )
        {
            String code = list.get( 0 ).getCode();
            if ( code != null )
            {
                codeOrder = Integer.parseInt( code.substring( 3 ) ) + 1;
            }
        }
        bankAccount.setCode( String.format( BANK_ACCOUNT_CODE_FORMAT, codeOrder ) );
        bankAccount.save();
    }

    @Override
    public List<BankAccount> getBankAccounts( @Nonnull LocalAccount account,
                                              @Nullable Integer offset,
                                              @Nullable Integer limit,
                                              @Nullable String country )
    {
        checkNotNull( account, "LocalAccount cannot be null" );
        Criteria<BankAccount> criteria = Criteria.of( BankAccount.class );

        criteria.reference( "owner", account );
        if ( country != null )
        {
            criteria.equal( "country", country.toUpperCase() );
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

        List<BankAccount> list = datastore.list( criteria );
        logger.info( list.size() + " bank accounts has been found." );

        return list;
    }

    @Override
    public void updateBankAccount( @Nonnull LocalAccount account, @Nonnull BankAccount bankAccount )
    {
        String template = "{0} cannot be null";
        checkNotNull( account, template, "LocalAccount" );

        BankAccount checkedBankAccount = checkOwner( account, checkNotNull( bankAccount, template, "BankAccount" ) );
        checkedBankAccount.save();
    }

    @Override
    public BankAccount deleteBankAccount( @Nonnull LocalAccount account, @Nonnull Long id )
    {
        String template = "{0} cannot be null";

        // TODO remove payment gate from account config; old: config.removePaymentGateway( bankAccount.getPaymentGate() );
        BankAccount bankAccount = getBankAccount(
                checkNotNull( account, template, "LocalAccount" ),
                checkNotNull( id, template, "Bank account ID" ) );

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
    public BankAccount markBankAccountAsPrimary( @Nonnull LocalAccount account, @Nonnull Long id )
    {
        String template = "{0} cannot be null";
        BankAccount bankAccount = getBankAccount(
                checkNotNull( account, template, "LocalAccount" ),
                checkNotNull( id, template, "Bank account ID" ) );

        // mark old primary bank accounts to not primary
        List<BankAccount> bankAccounts = getBankAccounts( account, null, null, null );

        Collection<BankAccount> primary = bankAccounts.stream().filter( new BankAccountPrimary() ).collect( Collectors.toList() );

        ofy().transact( () -> {
            for ( BankAccount primaryBankAccount : primary )
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
    public BankAccount getPrimaryBankAccount( @Nonnull LocalAccount account, @Nullable String country )
    {
        BankAccount primary = getInternalPrimaryBankAccount( account, country );
        if ( primary == null )
        {
            throw new BankAccountNotFound( -1L );
        }
        return primary;
    }

    BankAccount getInternalPrimaryBankAccount( @Nonnull LocalAccount account, @Nullable String country )
    {
        String template = "{0} cannot be null";
        List<BankAccount> list = getBankAccounts( checkNotNull( account, template, "LocalAccount" ), null, null, null );
        Collections.sort( list );

        country = country == null ? account.getDomicile().name() : country;

        Predicate<BankAccount> and = new BankAccountCountryPredicate( country ).and( new BankAccountPrimary() );
        Collection<BankAccount> filtered = list.stream().filter( and ).collect( Collectors.toList() );

        BankAccount bankAccount;

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
    public List<BankAccount> getAlternativeBankAccounts( @Nonnull LocalAccount account,
                                                         @Nullable Integer offset,
                                                         @Nullable Integer limit,
                                                         @Nullable Locale locale,
                                                         @Nullable String country )
    {
        String template = "{0} cannot be null";
        BankAccount exclude = getInternalPrimaryBankAccount( checkNotNull( account, template, "LocalAccount" ), country );
        country = country == null ? account.getDomicile().name() : country;
        locale = account.getLocale( locale );

        List<BankAccount> list = getBankAccounts( account, offset, limit, null );
        list.sort( new BankAccountSellerSorting( country ) );
        Iterator<BankAccount> iterator = list.iterator();

        List<BankAccount> filtered = new ArrayList<>();

        while ( iterator.hasNext() )
        {
            BankAccount next = iterator.next();

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
            logger.warn( wrongOwnerException.getMessage() );
            throw wrongOwnerException;
        }
        return entity;
    }

    /**
     * Descending sorting of the bank accounts based on its code property.
     */
    private static class BankAccountCodeDescending
            implements Comparator<BankAccount>, Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( BankAccount left, BankAccount right )
        {
            return ComparisonChain.start()
                    .compare( right.getCode(), left.getCode(), Ordering.natural().nullsLast() )
                    .result();
        }
    }

    static class BankAccountPrimary
            implements Predicate<BankAccount>
    {
        @Override
        public boolean test( @Nullable BankAccount input )
        {
            return input != null && input.isPrimary();
        }
    }

    private static class BankAccountCountryPredicate
            implements Predicate<BankAccount>
    {
        private String countryCode;

        BankAccountCountryPredicate( String countryCode )
        {
            this.countryCode = checkNotNull( countryCode );
        }

        @Override
        public boolean test( @Nullable BankAccount input )
        {
            return input != null && countryCode.equals( input.getCountry() );
        }
    }

    private static class BankAccountSellerSorting
            implements Comparator<BankAccount>, Serializable
    {
        private static final long serialVersionUID = 1L;

        private String country;

        BankAccountSellerSorting( @Nonnull String country )
        {
            this.country = checkNotNull( country );
        }

        @Override
        public int compare( BankAccount left, BankAccount right )
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
