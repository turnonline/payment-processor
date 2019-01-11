package biz.turnonline.ecosystem.payment.api;

import org.ctoolkit.services.endpoints.CacheControlFilter;

import javax.inject.Singleton;

import static biz.turnonline.ecosystem.payment.api.PaymentsApiProfile.API_NAME;
import static biz.turnonline.ecosystem.payment.api.PaymentsApiProfile.CURRENT_VERSION;


/**
 * The REST API 'Cache-Control' filter for all 'codebook'.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
public class CodeBookCacheControlFilter
        extends CacheControlFilter
{
    public static final String FILTER_PATH = "/api/" + API_NAME + "/"
            + CURRENT_VERSION + "/codebook/*";

    @Override
    public Integer getMaxAge()
    {
        // valid for 3 days
        return 259200;
    }
}
