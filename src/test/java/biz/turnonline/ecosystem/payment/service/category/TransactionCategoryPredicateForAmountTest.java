package biz.turnonline.ecosystem.payment.service.category;

import biz.turnonline.ecosystem.payment.service.model.CategoryFilter;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.TransactionReceipt;
import org.testng.annotations.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public class TransactionCategoryPredicateForAmountTest
{

    private TransactionCategoryPredicateForAmount predicate = new TransactionCategoryPredicateForAmount();

    @Test
    public void testApply()
    {
        assertThat( predicate.apply( mockFilter( CategoryFilter.PropertyName.NAME ), null ) ).isFalse();
        assertThat( predicate.apply( mockFilter( CategoryFilter.PropertyName.AMOUNT ), null ) ).isTrue();
        assertThat( predicate.apply( mockFilter( CategoryFilter.PropertyName.CREDIT ), null ) ).isFalse();
        assertThat( predicate.apply( mockFilter( CategoryFilter.PropertyName.CURRENCY ), null ) ).isFalse();
    }

    @Test
    public void testResolve_LT()
    {
        assertThat( predicate.resolve( mockFilter( "9", CategoryFilter.Operation.LT ), mockTransaction() ) ).isFalse();
        assertThat( predicate.resolve( mockFilter( "10", CategoryFilter.Operation.LT ), mockTransaction() ) ).isFalse();
        assertThat( predicate.resolve( mockFilter( "11", CategoryFilter.Operation.LT ), mockTransaction() ) ).isTrue();
    }

    @Test
    public void testResolve_LTE()
    {
        assertThat( predicate.resolve( mockFilter( "9", CategoryFilter.Operation.LTE), mockTransaction() ) ).isFalse();
        assertThat( predicate.resolve( mockFilter( "10", CategoryFilter.Operation.LTE ), mockTransaction() ) ).isTrue();
        assertThat( predicate.resolve( mockFilter( "11", CategoryFilter.Operation.LTE ), mockTransaction() ) ).isTrue();
    }

    @Test
    public void testResolve_EQ()
    {
        assertThat( predicate.resolve( mockFilter( "9", CategoryFilter.Operation.EQ), mockTransaction() ) ).isFalse();
        assertThat( predicate.resolve( mockFilter( "10", CategoryFilter.Operation.EQ ), mockTransaction() ) ).isTrue();
        assertThat( predicate.resolve( mockFilter( "11", CategoryFilter.Operation.EQ ), mockTransaction() ) ).isFalse();
    }

    @Test
    public void testResolve_GT()
    {
        assertThat( predicate.resolve( mockFilter( "9", CategoryFilter.Operation.GT), mockTransaction() ) ).isTrue();
        assertThat( predicate.resolve( mockFilter( "10", CategoryFilter.Operation.GT ), mockTransaction() ) ).isFalse();
        assertThat( predicate.resolve( mockFilter( "11", CategoryFilter.Operation.GT ), mockTransaction() ) ).isFalse();
    }

    @Test
    public void testResolve_GTE()
    {
        assertThat( predicate.resolve( mockFilter( "9", CategoryFilter.Operation.GTE), mockTransaction() ) ).isTrue();
        assertThat( predicate.resolve( mockFilter( "10", CategoryFilter.Operation.GTE ), mockTransaction() ) ).isTrue();
        assertThat( predicate.resolve( mockFilter( "11", CategoryFilter.Operation.GTE ), mockTransaction() ) ).isFalse();
    }

    @Test
    public void testResolve_REGEXP()
    {
        assertThat( predicate.resolve( mockFilter( "9", CategoryFilter.Operation.REGEXP), mockTransaction() ) ).isFalse();
    }

    private CategoryFilter mockFilter( CategoryFilter.PropertyName propertyName )
    {
        CategoryFilter filter = new CategoryFilter();
        filter.setPropertyName( propertyName );
        return filter;
    }

    private CategoryFilter mockFilter( String value, CategoryFilter.Operation operation )
    {
        CategoryFilter filter = new CategoryFilter();
        filter.setPropertyValue( value );
        filter.setOperation( operation );
        return filter;
    }

    private CommonTransaction mockTransaction()
    {
        TransactionReceipt receipt = new TransactionReceipt( "1" );
        receipt.amount( 10D );
        return receipt;
    }
}