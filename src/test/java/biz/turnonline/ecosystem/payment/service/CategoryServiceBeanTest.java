package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.payment.service.model.Category;
import biz.turnonline.ecosystem.payment.service.model.CategoryFilter;
import biz.turnonline.ecosystem.payment.service.model.TransactionCategory;
import biz.turnonline.ecosystem.payment.service.model.TransactionReceipt;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import mockit.Expectations;
import mockit.Mocked;
import org.ctoolkit.services.storage.EntityExecutor;
import org.ctoolkit.services.storage.criteria.Criteria;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public class CategoryServiceBeanTest
{
    private CategoryServiceBean tested;

    private MapperFacade mapperFacade;

    @Mocked
    private EntityExecutor datastore;

    @BeforeMethod
    public void setUp()
    {
        mapperFacade = new DefaultMapperFactory.Builder().build().getMapperFacade();
        this.tested = new CategoryServiceBean( mapperFacade, datastore );
    }

    @Test
    public void testResolveCategories_FilterNotMatched()
    {
        TransactionReceipt transaction = new TransactionReceipt( "1" );
        transaction.setMerchantName( "Orange" );

        new Expectations()
        {
            {
                datastore.list( withAny( Criteria.of( Category.class ) ) );
                result = Collections.singletonList( mockCategory() );
            }
        };

        List<TransactionCategory> transactionCategories = tested.resolveCategories( transaction );

        assertThat( transactionCategories.size() ).isEqualTo( 0 );
    }

    @Test
    public void testResolveCategories_FilterMatched()
    {
        TransactionReceipt transaction = new TransactionReceipt( "1" );
        transaction.setMerchantName( "Istores" );

        new Expectations()
        {
            {
                datastore.list( withAny( Criteria.of( Category.class ) ) );
                result = Collections.singletonList(mockCategory());
            }
        };

        List<TransactionCategory> transactionCategories = tested.resolveCategories( transaction );

        assertThat( transactionCategories.size() ).isEqualTo( 1 );

        TransactionCategory transactionCategory = transactionCategories.get( 0 );
        assertThat( transactionCategory.getColor() ).isEqualTo( "#fffaaa" );
        assertThat( transactionCategory.getName() ).isEqualTo( "Apple products" );
        assertThat( transactionCategory.isPropagate() ).isTrue();
    }

    private Category mockCategory()
    {
        CategoryFilter filter = new CategoryFilter();
        filter.setPropertyName( CategoryFilter.PropertyName.NAME );
        filter.setPropertyValue( "Istores" );
        filter.setOperation( CategoryFilter.Operation.EQ );

        Category category = new Category();
        category.setName( "Apple products" );
        category.setColor( "#fffaaa" );
        category.setPropagate( true );
        category.getFilters().add( filter );

        return category;
    }
}