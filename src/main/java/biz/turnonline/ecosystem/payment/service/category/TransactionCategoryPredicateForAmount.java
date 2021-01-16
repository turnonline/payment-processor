package biz.turnonline.ecosystem.payment.service.category;

import biz.turnonline.ecosystem.payment.service.model.CategoryFilter;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;

/**
 * Transaction category predicate for amount {@link CommonTransaction#getAmount()}
 *
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public class TransactionCategoryPredicateForAmount
        implements TransactionCategoryPredicate
{
    @Override
    public boolean apply( CategoryFilter filter, CommonTransaction transaction )
    {
        return filter.getPropertyName() == CategoryFilter.PropertyName.AMOUNT;
    }

    @Override
    public boolean resolve( CategoryFilter filter, CommonTransaction transaction )
    {
        double amount = transaction.getAmount();
        double expected = Double.parseDouble( filter.getPropertyValue() );

        CategoryFilter.Operation operation = filter.getOperation();

        switch ( operation )
        {
            case LT:
                return amount < expected;
            case LTE:
                return amount <= expected;
            case GT:
                return amount > expected;
            case GTE:
                return amount >= expected;
            case EQ:
                return amount == expected;
        }

        return false;
    }
}
