package biz.turnonline.ecosystem.payment.service.category;

import biz.turnonline.ecosystem.payment.service.model.CategoryFilter;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;

/**
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public interface TransactionCategoryPredicate
{
    boolean apply( CategoryFilter filter, CommonTransaction transaction);

    boolean resolve( CategoryFilter filter, CommonTransaction transaction );
}
