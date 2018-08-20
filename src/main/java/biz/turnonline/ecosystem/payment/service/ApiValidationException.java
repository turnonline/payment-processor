package biz.turnonline.ecosystem.payment.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ResourceBundle;

/**
 * Thrown to indicate that API validation has failed.
 * Endpoint API will throw 400 - bad request.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class ApiValidationException
        extends IllegalArgumentException
{
    /**
     * Constructs runtime exception with validation message to be thrown to the end user.
     *
     * @param message the validation message to be exposed publicly
     */
    public ApiValidationException( String message )
    {
        super( message );
    }

    public static ApiValidationException prepare( @Nonnull String key, @Nullable Object... args )
    {
        // Path to the API related messages properties file to be used by resource bundle.
        String path = "biz/turnonline/ecosystem/payment/api-messages";
        String message = ResourceBundle.getBundle( path ).getString( key );

        return new ApiValidationException( String.format( message, args ) );
    }
}
