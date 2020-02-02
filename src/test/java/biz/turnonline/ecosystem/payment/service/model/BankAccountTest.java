package biz.turnonline.ecosystem.payment.service.model;

import biz.turnonline.ecosystem.payment.service.CodeBook;
import mockit.Injectable;
import mockit.Tested;
import org.testng.annotations.Test;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * {@link BankAccount} unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class BankAccountTest
{
    @Tested
    private BankAccount tested;

    @Injectable
    private CodeBook codeBook;

    @Test
    public void setIban_DE_Societe_Generale()
    {
        tested.setIban( "DE75 5121 0800 1245126199" );

        assertWithMessage( "IBAN country" )
                .that( tested.getCountry() )
                .isEqualTo( "DE" );

        assertWithMessage( "IBAN bank code" )
                .that( tested.getBankCode() )
                .isEqualTo( "51210800" );

        assertWithMessage( "IBAN branch" )
                .that( tested.getBranch() )
                .isNull();

        assertWithMessage( "Formatted IBAN" )
                .that( tested.getIbanString() )
                .isEqualTo( "DE75 5121 0800 1245 1261 99" );
    }

    @Test
    public void setIban_SK_CSOB()
    {
        tested.setIban( "SK897500 00000000 1234 5671" );

        assertWithMessage( "IBAN country" )
                .that( tested.getCountry() )
                .isEqualTo( "SK" );

        assertWithMessage( "IBAN bank code" )
                .that( tested.getBankCode() )
                .isEqualTo( "7500" );

        assertWithMessage( "IBAN branch" )
                .that( tested.getBranch() )
                .isNull();

        assertWithMessage( "Formatted IBAN" )
                .that( tested.getIbanString() )
                .isEqualTo( "SK89 7500 0000 0000 1234 5671" );
    }

    @Test
    public void setIban_GB_Revolut()
    {
        tested.setIban( "GB35 REVO 00996912346754" );

        assertWithMessage( "IBAN country" )
                .that( tested.getCountry() )
                .isEqualTo( "GB" );

        assertWithMessage( "IBAN bank code" )
                .that( tested.getBankCode() )
                .isEqualTo( "REVO" );

        assertWithMessage( "IBAN branch" )
                .that( tested.getBranch() )
                .isEqualTo( "009969" );

        assertWithMessage( "Formatted IBAN" )
                .that( tested.getIbanString() )
                .isEqualTo( "GB35 REVO 0099 6912 3467 54" );
    }

    @Test( expectedExceptions = IllegalArgumentException.class )
    public void setIban_Invalid()
    {
        tested.setIban( "LU000019400644750000" );
    }
}