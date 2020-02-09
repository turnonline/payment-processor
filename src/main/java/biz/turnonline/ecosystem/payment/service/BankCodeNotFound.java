package biz.turnonline.ecosystem.payment.service;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The exception thrown if requested bank code has not been found.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class BankCodeNotFound
        extends IllegalArgumentException
{
    private static final long serialVersionUID = 1L;

    private final String bankCode;

    public BankCodeNotFound( @Nonnull String bankCode )
    {
        this.bankCode = checkNotNull( bankCode );
    }

    public String getBankCode()
    {
        return bankCode;
    }
}
