package biz.turnonline.ecosystem.payment.service.category;

import biz.turnonline.ecosystem.payment.service.model.CategoryFilter;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.TransactionReceipt;

import java.util.regex.Pattern;

/**
 * Transaction category predicate for name {@link TransactionReceipt#getReference()} ()}
 *
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public class TransactionCategoryPredicateForReference
        implements TransactionCategoryPredicate
{
    @Override
    public boolean apply( CategoryFilter filter, CommonTransaction transaction )
    {
        return filter.getPropertyName() == CategoryFilter.PropertyName.REFERENCE &&
                transaction instanceof TransactionReceipt;
    }

    @Override
    public boolean resolve( CategoryFilter filter, CommonTransaction transaction )
    {
        String name = transaction.getReference();
        String expected = filter.getPropertyValue();

        CategoryFilter.Operation operation = filter.getOperation();

        switch ( operation )
        {
            case EQ:
                return expected.equals( name );
            case REGEXP:
                return Pattern.matches( expected, name );
        }

        return false;
    }
}
