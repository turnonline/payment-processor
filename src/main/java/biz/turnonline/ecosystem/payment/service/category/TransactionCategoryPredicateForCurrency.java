package biz.turnonline.ecosystem.payment.service.category;

import biz.turnonline.ecosystem.payment.service.model.CategoryFilter;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;

/**
 * Transaction category predicate for currency {@link CommonTransaction#getCurrency()}
 *
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public class TransactionCategoryPredicateForCurrency
        implements TransactionCategoryPredicate
{
    @Override
    public boolean apply( CategoryFilter filter, CommonTransaction transaction )
    {
        return filter.getPropertyName() == CategoryFilter.PropertyName.CURRENCY;
    }

    @Override
    public boolean resolve( CategoryFilter filter, CommonTransaction transaction )
    {
        String name = transaction.getCurrency();
        String expected = filter.getPropertyValue();

        CategoryFilter.Operation operation = filter.getOperation();

        if ( operation == CategoryFilter.Operation.EQ )
        {
            return expected.equals( name );
        }

        return false;
    }
}
