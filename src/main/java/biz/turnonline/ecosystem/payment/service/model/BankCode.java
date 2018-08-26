package biz.turnonline.ecosystem.payment.service.model;

import com.googlecode.objectify.annotation.Subclass;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The bank code definition as a code-book item.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Subclass( name = "BankCode", index = true )
public class BankCode
        extends CodeBookItem
{
    private static final long serialVersionUID = -8861313492104308022L;

    public BankCode()
    {
    }

    public BankCode( String code, String label, String locale, String domicile )
    {
        super.setCode( checkNotNull( code ) );
        super.setLabel( checkNotNull( label ) );
        super.setLocale( checkNotNull( locale ) );
        super.setCountry( checkNotNull( domicile ) );
    }

    @Override
    public String toString()
    {
        return "class BankCode {\n" +
                "    code: " + toIndentedString( getCode() ) + "\n" +
                "    country: " + toIndentedString( getCountry() ) + "\n" +
                "    label: " + toIndentedString( getLabel() ) + "\n" +
                "    locale: " + toIndentedString( getLocale() ) + "\n" +
                "    version: " + toIndentedString( getVersion() ) + "\n" +
                "}";
    }
}
