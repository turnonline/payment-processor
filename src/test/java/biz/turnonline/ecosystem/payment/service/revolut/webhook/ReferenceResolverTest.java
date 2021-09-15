package biz.turnonline.ecosystem.payment.service.revolut.webhook;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public class ReferenceResolverTest
{
    private final ReferenceResolver resolver = new ReferenceResolver();

    @Test
    public void testResolve_referenceIsNull()
    {
        assertNull( resolver.resolve( null ) );
    }

    @Test
    public void testResolve_numberBellowRange()
    {
        assertNull( resolver.resolve( "12345" ) );
    }

    @Test
    public void testResolve_numberAboveRange()
    {
        assertEquals( resolver.resolve( "123451234512345123451" ), "12345123451234512345" );
    }

    @Test
    public void testResolve_onlyCharacters()
    {
        assertNull( resolver.resolve( "Lorem ipsum" ) );
    }

    @Test
    public void testResolve_onlyNumbers()
    {
        assertEquals( resolver.resolve( "12345678" ), "12345678" );
    }

    @Test
    public void testResolve_numbersAndCharacters()
    {
        assertEquals( resolver.resolve( "faktura c. 202100009" ), "202100009" );
    }
}