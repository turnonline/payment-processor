package biz.turnonline.ecosystem.payment.service.category;

import biz.turnonline.ecosystem.payment.service.model.CategoryFilter;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;

import java.util.regex.Pattern;

/**
 * Transaction category predicate for counterparty iban {@link CommonTransaction#getCounterparty()#getIban()}
 *
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public class TransactionCategoryPredicateForCounterpartyIban
        implements TransactionCategoryPredicate
{
    @Override
    public boolean apply( CategoryFilter filter, CommonTransaction transaction )
    {
        return filter.getPropertyName() == CategoryFilter.PropertyName.COUNTERPARTY_IBAN;
    }

    @Override
    public boolean resolve( CategoryFilter filter, CommonTransaction transaction )
    {
        if ( transaction.getCounterparty() == null )
        {
            return false;
        }

        String iban = transaction.getCounterparty().getIban();
        String expected = filter.getPropertyValue();

        CategoryFilter.Operation operation = filter.getOperation();

        switch ( operation )
        {
            case EQ:
                return iban.equals( expected );
            case REGEXP:
                return Pattern.matches( expected, iban );
        }

        return false;
    }
}
