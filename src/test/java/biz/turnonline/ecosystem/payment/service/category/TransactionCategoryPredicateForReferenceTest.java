package biz.turnonline.ecosystem.payment.service.category;

import biz.turnonline.ecosystem.payment.service.model.CategoryFilter;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.TransactionReceipt;
import org.testng.annotations.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public class TransactionCategoryPredicateForReferenceTest
{
    private TransactionCategoryPredicateForReference predicate = new TransactionCategoryPredicateForReference();

    @Test
    public void testApply()
    {
        assertThat( predicate.apply( mockFilter( CategoryFilter.PropertyName.NAME ), mockTransaction() ) ).isFalse();
        assertThat( predicate.apply( mockFilter( CategoryFilter.PropertyName.AMOUNT ), mockTransaction() ) ).isFalse();
        assertThat( predicate.apply( mockFilter( CategoryFilter.PropertyName.CREDIT ), mockTransaction() ) ).isFalse();
        assertThat( predicate.apply( mockFilter( CategoryFilter.PropertyName.CURRENCY ), mockTransaction() ) ).isFalse();
        assertThat( predicate.apply( mockFilter( CategoryFilter.PropertyName.COUNTERPARTY_IBAN ), mockTransaction() ) ).isFalse();
        assertThat( predicate.apply( mockFilter( CategoryFilter.PropertyName.REFERENCE ), mockTransaction() ) ).isTrue();
    }

    @Test
    public void testResolve_LT()
    {
        assertThat( predicate.resolve( mockFilter( "/VS12345", CategoryFilter.Operation.LT ), mockTransaction() ) ).isFalse();
    }

    @Test
    public void testResolve_LTE()
    {
        assertThat( predicate.resolve( mockFilter( "/VS12345", CategoryFilter.Operation.LTE), mockTransaction() ) ).isFalse();
    }

    @Test
    public void testResolve_EQ()
    {
        assertThat( predicate.resolve( mockFilter( "/VS12345678", CategoryFilter.Operation.EQ), mockTransaction() ) ).isFalse();
        assertThat( predicate.resolve( mockFilter( "/VS12345", CategoryFilter.Operation.EQ ), mockTransaction() ) ).isTrue();
    }

    @Test
    public void testResolve_GT()
    {
        assertThat( predicate.resolve( mockFilter( "/VS12345", CategoryFilter.Operation.GT), mockTransaction() ) ).isFalse();
    }

    @Test
    public void testResolve_GTE()
    {
        assertThat( predicate.resolve( mockFilter( "/VS12345", CategoryFilter.Operation.GTE), mockTransaction() ) ).isFalse();
    }

    @Test
    public void testResolve_REGEXP()
    {
        assertThat( predicate.resolve( mockFilter( "/VS123.*", CategoryFilter.Operation.REGEXP), mockTransaction() ) ).isTrue();
        assertThat( predicate.resolve( mockFilter( "123.*", CategoryFilter.Operation.REGEXP), mockTransaction() ) ).isFalse();
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
        receipt.reference( "/VS12345" );

        return receipt;
    }
}