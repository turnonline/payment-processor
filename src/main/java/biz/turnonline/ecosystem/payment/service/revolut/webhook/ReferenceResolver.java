package biz.turnonline.ecosystem.payment.service.revolut.webhook;

import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reference resolver helper for resolving reference number for {@link CommonTransaction}.
 * It matches for digits for 6-20 repetitions. If it does not match, return null.
 *
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public class ReferenceResolver
{
    private static final Pattern pattern = Pattern.compile( "\\d{6,20}" );

    public String resolve( String reference )
    {
        if ( reference == null )
        {
            return null;
        }

        Matcher matcher = pattern.matcher( reference );
        if ( matcher.find() )
        {
            MatchResult matchResult = matcher.toMatchResult();
            return matchResult.group();
        }

        return null;
    }
}
