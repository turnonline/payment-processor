package biz.turnonline.ecosystem.payment.service.category;

import biz.turnonline.ecosystem.payment.service.model.CategoryFilter;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;

/**
 * Transaction category predicate for credit/debit {@link CommonTransaction#isCredit()}
 *
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public class TransactionCategoryPredicateForCredit
        implements TransactionCategoryPredicate
{
    @Override
    public boolean apply( CategoryFilter filter, CommonTransaction transaction )
    {
        return filter.getPropertyName() == CategoryFilter.PropertyName.CREDIT;
    }

    @Override
    public boolean resolve( CategoryFilter filter, CommonTransaction transaction )
    {
        boolean credit = transaction.isCredit();
        boolean expected = Boolean.parseBoolean( filter.getPropertyValue() );

        CategoryFilter.Operation operation = filter.getOperation();

        if ( operation == CategoryFilter.Operation.EQ )
        {
            return credit == expected;
        }

        return false;
    }
}
