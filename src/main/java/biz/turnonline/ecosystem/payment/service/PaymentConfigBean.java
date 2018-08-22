package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.account.client.model.Account;
import biz.turnonline.ecosystem.account.client.model.Domicile;
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

    private final CodeBook codeBook;

    private final EntityExecutor datastore;

    private final LocalAccountProvider accProvider;

    @Inject
    PaymentConfigBean( CodeBook codeBook,
                       EntityExecutor datastore,
                       LocalAccountProvider accProvider )
    {
        this.codeBook = codeBook;
        this.datastore = datastore;
        this.accProvider = accProvider;
    }

    @Override
    public BankAccount getBankAccount( @Nonnull Account account, @Nonnull Long id )
    {
        String errorTemplate = "{0} cannot be null";
        checkNotNull( account, errorTemplate, Account.class.getSimpleName() );
        checkNotNull( id, errorTemplate, "Bank account ID" );

        BankAccount bankAccount = loadBankAccount( id );

        LocalAccount owner = accProvider.getAssociatedLightAccount( account );
        return checkOwner( owner, bankAccount );
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
    public void insertBankAccount( @Nonnull Account account, @Nonnull BankAccount bankAccount )
    {
        String errorTemplate = "{0} cannot be null";
        checkNotNull( account, errorTemplate, Account.class.getSimpleName() );
        checkNotNull( bankAccount, errorTemplate, BankAccount.class.getSimpleName() );

        if ( bankAccount.getId() != null )
        {
            String message = bankAccount.entityKey() + " should be in memory instance only, not persisted object.";
            throw new IllegalArgumentException( message );
        }

        LocalAccount owner = accProvider.getAssociatedLightAccount( account );
        bankAccount.setOwner( owner );

        // generate code
        int codeOrder = 1;
        List<BankAccount> list = getBankAccounts( account );
        list.sort( new BankAccountCodeDescending() );

        if ( !list.isEmpty() )
        {
            String code = list.get( 0 ).getCode();
            if ( code != null )
            {
                codeOrder = Integer.valueOf( code.substring( 3 ) ) + 1;
            }
        }
        bankAccount.setCode( String.format( BANK_ACCOUNT_CODE_FORMAT, codeOrder ) );
        bankAccount.save();
    }

    @Override
    public List<BankAccount> getBankAccounts( @Nonnull Account account )
    {
        checkNotNull( account, "{0} cannot be null", Account.class.getSimpleName() );

        Criteria<BankAccount> criteria = Criteria.of( BankAccount.class );

        LocalAccount owner = accProvider.getAssociatedLightAccount( account );
        criteria.reference( "owner", owner );

        List<BankAccount> list = datastore.list( criteria );
        logger.info( list.size() + " bank accounts has been found." );

        return list;
    }

    @Override
    public void updateBankAccount( @Nonnull Account account, @Nonnull BankAccount bankAccount )
    {
        String errorTemplate = "{0} cannot be null";
        checkNotNull( account, errorTemplate, Account.class.getSimpleName() );
        checkNotNull( bankAccount, errorTemplate, BankAccount.class.getSimpleName() );

        LocalAccount owner = accProvider.getAssociatedLightAccount( account );
        BankAccount checkedBankAccount = checkOwner( owner, bankAccount );
        checkedBankAccount.save();
    }

    @Override
    public BankAccount deleteBankAccount( @Nonnull Account account, @Nonnull Long id )
    {
        String errorTemplate = "{0} cannot be null";
        checkNotNull( account, errorTemplate, Account.class.getSimpleName() );
        checkNotNull( id, errorTemplate, "Bank account ID" );

        // TODO remove payment gate from account config; old: config.removePaymentGateway( bankAccount.getPaymentGate() );
        BankAccount bankAccount = getBankAccount( account, id );
        if ( bankAccount.isPrimary() )
        {
            String key = "errors.validation.bankAccount.deletion.primary";
            throw ApiValidationException.prepare( key, bankAccount.getCode() );
        }
        else
        {
            bankAccount.delete();
        }

        return bankAccount;
    }

    @Override
    public BankAccount markBankAccountAsPrimary( @Nonnull Account account, @Nonnull Long id )
    {
        String errorTemplate = "{0} cannot be null";
        checkNotNull( account, errorTemplate, Account.class.getSimpleName() );
        checkNotNull( id, errorTemplate, "Bank account ID" );

        BankAccount bankAccount = getBankAccount( account, id );

        // mark old primary bank accounts to not primary
        List<BankAccount> bankAccounts = getBankAccounts( account );

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
    public BankAccount getPrimaryBankAccount( @Nonnull Account account )
    {
        checkNotNull( account, "{0} cannot be null", Account.class.getSimpleName() );
        return getPrimaryBankAccount( account, null );
    }

    @Override
    public BankAccount getPrimaryBankAccount( @Nonnull Account account, @Nullable String country )
    {
        checkNotNull( account, "{0} cannot be null", Account.class.getSimpleName() );

        List<BankAccount> list = getBankAccounts( account );
        Collections.sort( list );

        Collection<BankAccount> filtered;
        BankAccount bankAccount;

        Predicate<BankAccount> and;
        if ( country == null )
        {
            country = codeBook.getDomicile( account, country );
            // first strict predicates (incl. primary filter)
            and = new BankAccountPrimary();
        }
        else
        {
            and = new BankAccountCountryPredicate( country ).and( new BankAccountPrimary() );
        }

        filtered = list.stream().filter( and ).collect( Collectors.toList() );

        if ( filtered.isEmpty() )
        {
            // less strict predicates (no primary filter)
            BankAccountCountryPredicate predicate = new BankAccountCountryPredicate( country );
            bankAccount = list.stream().filter( predicate ).findFirst().orElse( null );
        }
        else
        {
            bankAccount = filtered.iterator().next();
        }

        if ( bankAccount == null && !list.isEmpty() )
        {
            // no match yet, thus return any bank account marked as primary
            bankAccount = list.stream().filter( new BankAccountPrimary() ).findFirst().orElse( null );
        }

        return bankAccount;
    }

    @Override
    public List<BankAccount.Description> getAlternativeBankAccounts( @Nonnull Account account,
                                                                     @Nullable BankAccount exclude )
    {
        checkNotNull( account, "{0} cannot be null", Account.class.getSimpleName() );

        Domicile domicile = Domicile.valueOf( codeBook.getDomicile( account, null ) );

        List<BankAccount> list = getBankAccounts( account );
        list.sort( new BankAccountSellerSorting( domicile ) );
        Iterator<BankAccount> iterator = list.iterator();

        List<BankAccount.Description> holders = new ArrayList<>();

        while ( iterator.hasNext() )
        {
            BankAccount next = iterator.next();

            if ( exclude == null || !exclude.equals( next ) )
            {
                BankAccount.Description description = next.getLocalizedDescription( domicile.getLocale() );
                if ( description != null )
                {
                    holders.add( description );
                }
            }
        }

        for ( BankAccount.Description next : holders )
        {
            logger.debug( next.toString() );
        }

        return holders;
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
        checkNotNull( entity );

        LocalAccount owner = entity.getOwner();
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
                    .compare( right.getCode(), left.getCode(), Ordering.natural().nullsFirst() )
                    .result();
        }
    }

    static class BankAccountPrimary
            implements Predicate<BankAccount>
    {
        @Override
        public boolean test( @Nullable BankAccount input )
        {
            return input != null
                    && input.isPrimary()
                    && !BankAccount.TRUST_PAY_BANK_CODE.equals( input.getBankCode() );
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
            return input != null
                    && !BankAccount.TRUST_PAY_BANK_CODE.equals( input.getBankCode() )
                    && input.getCountry().name().equals( countryCode );
        }
    }

    private static class BankAccountSellerSorting
            implements Comparator<BankAccount>, Serializable
    {
        private static final long serialVersionUID = 1L;

        private Domicile domicile;

        BankAccountSellerSorting( Domicile domicile )
        {
            this.domicile = domicile;
        }

        @Override
        public int compare( BankAccount left, BankAccount right )
        {
            return ComparisonChain.start()
                    .compare( left.getCountry(), domicile )
                    .compare( left, right )
                    .result();
        }
    }
}
