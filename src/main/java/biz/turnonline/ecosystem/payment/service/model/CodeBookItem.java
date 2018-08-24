package biz.turnonline.ecosystem.payment.service.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.io.Serializable;
import java.util.Objects;

/**
 * The code-book single item entry.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Entity( name = "CB_CodeBook" )
public abstract class CodeBookItem
        implements Serializable
{
    private static final long serialVersionUID = 6723123114542730421L;

    @Id
    private Long id;

    @Index
    private String code;

    @Index
    private String domicile;

    private String label;

    @Index
    private String locale;

    @Index
    private Integer version;

    public Long getId()
    {
        return id;
    }

    /**
     * The item's unique code for selected language.
     **/
    public String getCode()
    {
        return code;
    }

    protected void setCode( String code )
    {
        this.code = code;
    }

    /**
     * The ISO 3166 alpha-2 country code. The supported list is limited.
     **/
    public String getDomicile()
    {
        return domicile;
    }

    protected void setDomicile( String domicile )
    {
        this.domicile = domicile;
    }

    /**
     * The codebook language sensitive value.
     **/
    public String getLabel()
    {
        return label;
    }

    protected void setLabel( String label )
    {
        this.label = label;
    }

    /**
     * The label language. ISO 639 alpha-2 or alpha-3 language code.
     **/
    public String getLocale()
    {
        return locale;
    }

    protected void setLocale( String locale )
    {
        this.locale = locale;
    }

    /**
     * The codebook item version.
     **/
    public Integer getVersion()
    {
        return version;
    }

    protected void setVersion( Integer version )
    {
        this.version = version;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }
        CodeBookItem country = ( CodeBookItem ) o;
        return Objects.equals( code, country.code ) &&
                Objects.equals( locale, country.locale ) &&
                Objects.equals( version, country.version );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( code, locale, version );
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    String toIndentedString( Object o )
    {
        if ( o == null )
        {
            return "null";
        }
        return o.toString().replace( "\n", "\n    " );
    }
}
