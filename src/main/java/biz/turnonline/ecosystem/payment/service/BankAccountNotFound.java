package biz.turnonline.ecosystem.payment.service;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The exception thrown if requested bank account has not been found.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class BankAccountNotFound
        extends IllegalArgumentException
{
    private static final long serialVersionUID = 1L;

    private final Long bankAccountId;

    public BankAccountNotFound( @Nonnull Long bankAccountId )
    {
        this.bankAccountId = checkNotNull( bankAccountId );
    }

    public Long getBankAccountId()
    {
        return bankAccountId;
    }
}
