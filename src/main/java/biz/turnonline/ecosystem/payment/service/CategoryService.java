package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.payment.service.model.Category;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.TransactionCategory;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Transaction category service API
 *
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public interface CategoryService
{
    /**
     * Return all categories
     *
     * @return {@link Category}
     */
    List<Category> getCategories();

    /**
     * Get {@link Category} for specified id or return <code>null</code>
     *
     * @param categoryId id of {@link Category}
     * @return {@link Category}
     */
    Category getById( @Nonnull Long categoryId );

    /**
     * Create new {@link Category}
     *
     * @param category {@link Category}
     * @return persisted {@link Category}
     */
    Category create( @Nonnull Category category );

    /**
     * Update existing category
     *
     * @param category {@link Category}
     * @return updated {@link Category}
     */
    Category update( @Nonnull Category category );

    /**
     * Delete category
     *
     * @param category {@link Category}
     */
    void delete( @Nonnull Category category );

    /**
     * Apply categories for {@link CommonTransaction}
     *
     * @param transaction {@link CommonTransaction}
     * @return list of applied {@link Category}
     */
    List<TransactionCategory> resolveCategories( CommonTransaction transaction );
}
