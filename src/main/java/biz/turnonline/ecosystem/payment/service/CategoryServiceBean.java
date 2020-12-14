package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.payment.service.category.TransactionCategoryPredicate;
import biz.turnonline.ecosystem.payment.service.category.TransactionCategoryPredicateForAmount;
import biz.turnonline.ecosystem.payment.service.category.TransactionCategoryPredicateForCredit;
import biz.turnonline.ecosystem.payment.service.category.TransactionCategoryPredicateForCurrency;
import biz.turnonline.ecosystem.payment.service.category.TransactionCategoryPredicateForName;
import biz.turnonline.ecosystem.payment.service.model.Category;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.TransactionCategory;
import ma.glasnost.orika.MapperFacade;
import org.ctoolkit.services.storage.EntityExecutor;
import org.ctoolkit.services.storage.criteria.Criteria;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Implementation of {@link CategoryService}
 *
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
// TODO: cache categories
@Singleton
public class CategoryServiceBean
        implements CategoryService
{
    private final MapperFacade mapper;

    private final EntityExecutor datastore;

    private List<TransactionCategoryPredicate> predicates = new ArrayList<>();

    @Inject
    CategoryServiceBean( MapperFacade mapper, EntityExecutor datastore )
    {
        this.mapper = mapper;
        this.datastore = datastore;

        predicates.add( new TransactionCategoryPredicateForName() );
        predicates.add( new TransactionCategoryPredicateForAmount() );
        predicates.add( new TransactionCategoryPredicateForCurrency() );
        predicates.add( new TransactionCategoryPredicateForCredit() );
    }

    @Override
    public List<Category> getCategories()
    {
        return datastore.list( Criteria.of( Category.class ) );
    }

    @Override
    public Category getById( @Nonnull Long categoryId )
    {
        return ofy().load().type( Category.class ).id( categoryId ).now();
    }

    @Override
    public Category create( @Nonnull Category category )
    {
        category.save();
        return category;
    }

    @Override
    public Category update( @Nonnull Category category )
    {
        category.save();
        return category;
    }

    @Override
    public void delete( @Nonnull Category category )
    {
        category.delete();
    }

    @Override
    public List<TransactionCategory> resolveCategories( CommonTransaction transaction )
    {
        return getCategories().stream()
                .filter( category -> filterCategory( category, transaction ) )
                .map( category -> mapper.map( category, TransactionCategory.class ) )
                .collect( Collectors.toList() );
    }

    private boolean filterCategory( Category category, CommonTransaction transaction )
    {
        return category.getFilters().stream()
                .filter( filter ->
                        predicates.stream()
                                .filter( predicate -> predicate.apply( filter, transaction ) )
                                .filter( predicate -> predicate.resolve( filter, transaction ) )
                                .map( predicate -> true )
                                .findAny()
                                .orElse( false )
                )
                .map( predicate -> true )
                .findAny()
                .orElse( false );
    }
}
