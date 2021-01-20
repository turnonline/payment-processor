package biz.turnonline.ecosystem.payment.service.category;

import biz.turnonline.ecosystem.payment.service.model.CategoryFilter;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.CounterpartyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.TransactionReceipt;
import org.testng.annotations.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public class TransactionCategoryPredicateForCounterpartyIbanTest
{
    private TransactionCategoryPredicateForCounterpartyIban predicate = new TransactionCategoryPredicateForCounterpartyIban();

    @Test
    public void testApply()
    {
        assertThat( predicate.apply( mockFilter( CategoryFilter.PropertyName.NAME ), null ) ).isFalse();
        assertThat( predicate.apply( mockFilter( CategoryFilter.PropertyName.AMOUNT ), null ) ).isFalse();
        assertThat( predicate.apply( mockFilter( CategoryFilter.PropertyName.CREDIT ), null ) ).isFalse();
        assertThat( predicate.apply( mockFilter( CategoryFilter.PropertyName.CURRENCY ), null ) ).isFalse();
        assertThat( predicate.apply( mockFilter( CategoryFilter.PropertyName.COUNTERPARTY_IBAN ), null ) ).isTrue();
        assertThat( predicate.apply( mockFilter( CategoryFilter.PropertyName.REFERENCE ), null ) ).isFalse();
    }

    @Test
    public void testResolve_LT()
    {
        assertThat( predicate.resolve( mockFilter( "SK1234567890", CategoryFilter.Operation.LT ), mockTransaction() ) ).isFalse();
    }

    @Test
    public void testResolve_LTE()
    {
        assertThat( predicate.resolve( mockFilter( "SK1234567890", CategoryFilter.Operation.LTE), mockTransaction() ) ).isFalse();
    }

    @Test
    public void testResolve_EQ()
    {
        assertThat( predicate.resolve( mockFilter( "SK1234567891", CategoryFilter.Operation.EQ), mockTransaction() ) ).isFalse();
        assertThat( predicate.resolve( mockFilter( "SK1234567890", CategoryFilter.Operation.EQ ), mockTransaction() ) ).isTrue();
    }

    @Test
    public void testResolve_GT()
    {
        assertThat( predicate.resolve( mockFilter( "SK1234567890", CategoryFilter.Operation.GT), mockTransaction() ) ).isFalse();
    }

    @Test
    public void testResolve_GTE()
    {
        assertThat( predicate.resolve( mockFilter( "SK1234567890", CategoryFilter.Operation.GTE), mockTransaction() ) ).isFalse();
    }

    @Test
    public void testResolve_REGEXP()
    {
        assertThat( predicate.resolve( mockFilter( "SK.*", CategoryFilter.Operation.REGEXP), mockTransaction() ) ).isTrue();
        assertThat( predicate.resolve( mockFilter( "CZ.*", CategoryFilter.Operation.REGEXP), mockTransaction() ) ).isFalse();
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

        CounterpartyBankAccount counterparty = new CounterpartyBankAccount();
        counterparty.setIban( "SK1234567890" );
        receipt.setCounterparty( counterparty );

        return receipt;
    }
}