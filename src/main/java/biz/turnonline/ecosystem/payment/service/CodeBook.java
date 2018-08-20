package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.account.client.model.Account;
import biz.turnonline.ecosystem.account.client.model.AccountBusiness;
import biz.turnonline.ecosystem.payment.service.model.BankCode;
import biz.turnonline.ecosystem.payment.service.model.CodeBookItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

/**
 * The product billing related code-books.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 * @see CodeBookItem
 */
public interface CodeBook
{
    Locale DEFAULT_LOCALE = new Locale( "en" );

    String DEFAULT_DOMICILE = "SK";

    Logger logger = LoggerFactory.getLogger( CodeBook.class );


    /**
     * Returns the all bank codes available for specified country (defined by domicile).
     *
     * @param account  the authenticated account as a source of default locale and domicile if missing
     * @param locale   the optional language to prefer in results
     * @param domicile the optional ISO 3166 alpha-2 country code that represents a target domicile
     * @return the all bank codes for specific country
     */
    Map<String, BankCode> getBankCodes( @Nonnull Account account,
                                        @Nullable Locale locale,
                                        @Nullable String domicile );

    /**
     * Returns the final domicile with optional preference. Always returns a value.
     * If none of the values has been found a {@link #DEFAULT_DOMICILE} will be returned.
     *
     * @param account  the authenticated account as a source of default domicile if missing
     * @param domicile the optional (however preferred) ISO 3166 alpha-2 country code that represents a target domicile
     * @return the final domicile
     */
    default String getDomicile( @Nonnull Account account, @Nullable String domicile )
    {
        if ( domicile == null )
        {
            AccountBusiness business = account.getBusiness();
            if ( business != null )
            {
                domicile = business.getDomicile();
                logger.info( "Account business domicile has been applied: " + domicile );
            }
            else
            {
                domicile = DEFAULT_DOMICILE;
                logger.warn( "Using default domicile: " + domicile );
            }
        }
        return domicile.toUpperCase();
    }

    /**
     * Returns the final locale with optional preference. Always returns a value.
     * If none of the values has been found a {@link #DEFAULT_LOCALE} will be returned.
     *
     * @param account the authenticated account as a source of default locale if missing
     * @param locale  the optional however preferred language
     * @return the final locale
     */
    default Locale getLocale( @Nonnull Account account, @Nullable Locale locale )
    {
        if ( locale == null )
        {
            if ( account.getLocale() != null )
            {
                locale = new Locale( account.getLocale() );
                logger.info( "Final locale: " + locale );
            }
            else
            {
                locale = DEFAULT_LOCALE;
                logger.warn( "Using service default locale: " + locale );
            }
        }
        return locale;
    }
}
