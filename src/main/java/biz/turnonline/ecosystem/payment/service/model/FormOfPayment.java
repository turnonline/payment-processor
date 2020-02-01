package biz.turnonline.ecosystem.payment.service.model;

/**
 * The form of payment code-book enums.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public enum FormOfPayment
{
    BANK_TRANSFER,
    CASH,
    CREDIT_CARD,
    DEBIT_CARD;

    public static FormOfPayment getDefault()
    {
        return BANK_TRANSFER;
    }
}
