package biz.turnonline.ecosystem.payment.service;

/**
 * Indication of a business flow error, do not retry task execution.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class NoRetryException
        extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public NoRetryException()
    {
    }

    public NoRetryException( String message )
    {
        super( message );
    }

    public NoRetryException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public NoRetryException( Throwable cause )
    {
        super( cause );
    }
}
