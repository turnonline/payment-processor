package biz.turnonline.ecosystem.payment.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A container object which may or may not contain a non-null value with always specified default target value.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class Defaults<T, D>
{
    private final T input;

    private final D defaults;

    private final D target;

    private Defaults( @Nullable T input, @Nullable D target, @Nonnull D defaults )
    {
        this.input = input;
        this.target = target;
        this.defaults = Objects.requireNonNull( defaults );
    }

    /**
     * Returns instantiated {@code Defaults} with specified parameters.
     *
     * @param input    the input value to be applied or null if not present
     * @param target   the target value if any
     * @param defaults the default value to be set if input and target are {@code null}
     * @param <T>      the type of the input value
     * @param <D>      the type of the target value
     * @return instantiated defaults
     */
    public static <T, D> Defaults<T, D> of( @Nullable T input, @Nullable D target, @Nonnull D defaults )
    {
        return new Defaults<>( input, target, defaults );
    }

    /**
     * If an input value is present, invoke the specified consumer with the value.
     *
     * @param consumer  the consumer of the input value, block to be executed if an input value is present
     * @param converter the function to be used to convert input type in to expected target value
     * @param errorKey  the error message key to be rendered if converter fails
     * @throws ApiValidationException if converter function fails
     */
    public void ifPresentOrDefault( Consumer<? super D> consumer, Function<T, D> converter, String errorKey )
    {
        if ( input == null && target == null )
        {
            consumer.accept( defaults );
        }

        if ( input != null )
        {
            D applied;
            try
            {
                applied = converter.apply( input );
            }
            catch ( Exception e )
            {
                throw ApiValidationException.prepare( errorKey, input );
            }
            consumer.accept( applied );
        }
    }

    /**
     * If an input value is present, invoke the specified consumer with the value.
     * Use this method only if <T> and <D> are same type.
     *
     * @param consumer the consumer of the input value, block to be executed if an input value is present
     * @throws ClassCastException if <T> and <D> are of different type
     */
    @SuppressWarnings( "unchecked" )
    public void ifPresentOrDefault( Consumer<? super D> consumer )
    {
        ifPresentOrDefault( consumer, input -> ( D ) input, null );
    }
}
