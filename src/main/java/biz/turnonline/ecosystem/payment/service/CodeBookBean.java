package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.account.client.model.Account;
import biz.turnonline.ecosystem.payment.service.model.BankCode;
import com.googlecode.objectify.cmd.Query;
import net.sf.jsr107cache.Cache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static biz.turnonline.ecosystem.payment.service.MicroserviceModule.API_PREFIX;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Code-book service implementation.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
class CodeBookBean
        implements CodeBook
{
    private static final String CACHE_PREFIX = API_PREFIX + "::Code-book::";

    private final Cache cache;

    @Inject
    CodeBookBean( Cache cache )
    {
        this.cache = cache;
    }

    @Override
    public Map<String, BankCode> getBankCodes( @Nonnull Account account,
                                               @Nullable Locale locale,
                                               @Nullable String domicile )
    {
        checkNotNull( account );

        locale = getLocale( account, locale );
        domicile = getDomicile( account, domicile );

        String key = cacheKey( locale, domicile );
        if ( cache.containsKey( key ) )
        {
            //noinspection unchecked
            return ( Map<String, BankCode> ) cache.get( key );
        }

        String language = locale.getLanguage();

        Query<BankCode> query = ofy().transactionless().load().type( BankCode.class )
                .filter( "locale", language.toLowerCase() )
                .filter( "domicile", domicile )
                .order( "code" );

        Map<String, BankCode> result = new TreeMap<>();

        for ( BankCode item : query.list() )
        {
            result.put( item.getCode(), item );
        }

        if ( !result.isEmpty() )
        {
            cache.put( key, result );
        }

        return result;
    }

    private String cacheKey( @Nonnull Locale locale,
                             @Nonnull String domicile )
    {
        return CACHE_PREFIX + BankCode.class.getSimpleName() +
                "-" +
                locale.getLanguage().toLowerCase() +
                "-" +
                domicile;
    }
}
