package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.payment.service.model.BankCode;
import biz.turnonline.ecosystem.payment.service.model.CodeBookItem;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;

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
    /**
     * Returns the all bank codes available for specified country (defined by country).
     *
     * @param account the authenticated account as a source of default locale and country if missing
     * @param locale  the optional language to prefer in results
     * @param country the optional ISO 3166 alpha-2 country code that represents a target country
     * @return the all bank codes for specific country
     */
    Map<String, BankCode> getBankCodes( @Nonnull LocalAccount account,
                                        @Nullable Locale locale,
                                        @Nullable String country );

    /**
     * Returns the specified bank code for given country (defined by country).
     *
     * @param account the authenticated account as a source of default locale and country if missing
     * @param code    the numeric bank code assigned to concrete bank to be retrieved
     * @param locale  the optional language to prefer in results
     * @param country the optional ISO 3166 alpha-2 country code that represents a target country
     * @return the requested bank code
     */
    BankCode getBankCode( @Nonnull LocalAccount account,
                          @Nonnull String code,
                          @Nullable Locale locale,
                          @Nullable String country );

}
