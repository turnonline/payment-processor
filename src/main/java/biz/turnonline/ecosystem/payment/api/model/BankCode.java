package biz.turnonline.ecosystem.payment.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;


/**
 * The brief description of the bank for concrete bank code.
 **/

@ApiModel( description = "The brief description of the bank for concrete bank code." )
@javax.annotation.Generated( value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2018-08-26T04:47:00.033Z" )
public class BankCode
{

    private String code = null;

    private String label = null;

    private String locale = null;

    private String country = null;

    /**
     * The bank code is a numeric code assigned by a central bank to concrete bank.
     **/
    public BankCode code( String code )
    {
        this.code = code;
        return this;
    }


    @ApiModelProperty( required = true, value = "The bank code is a numeric code assigned by a central bank to concrete bank." )
    @JsonProperty( "code" )
    public String getCode()
    {
        return code;
    }

    public void setCode( String code )
    {
        this.code = code;
    }

    /**
     * The localized name of the bank.
     **/
    public BankCode label( String label )
    {
        this.label = label;
        return this;
    }


    @ApiModelProperty( required = true, value = "The localized name of the bank." )
    @JsonProperty( "label" )
    public String getLabel()
    {
        return label;
    }

    public void setLabel( String label )
    {
        this.label = label;
    }

    /**
     * The label language. ISO 639 alpha-2 or alpha-3 language code.
     **/
    public BankCode locale( String locale )
    {
        this.locale = locale;
        return this;
    }


    @ApiModelProperty( required = true, value = "The label language. ISO 639 alpha-2 or alpha-3 language code." )
    @JsonProperty( "locale" )
    public String getLocale()
    {
        return locale;
    }

    public void setLocale( String locale )
    {
        this.locale = locale;
    }

    /**
     * The ISO 3166 alpha-2 country code. The country of the bank code that belongs to.
     **/
    public BankCode country( String country )
    {
        this.country = country;
        return this;
    }


    @ApiModelProperty( required = true, value = "The ISO 3166 alpha-2 country code. The country of the bank code that belongs to." )
    @JsonProperty( "country" )
    public String getCountry()
    {
        return country;
    }

    public void setCountry( String country )
    {
        this.country = country;
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
        BankCode bankCode = ( BankCode ) o;
        return Objects.equals( code, bankCode.code ) &&
                Objects.equals( label, bankCode.label ) &&
                Objects.equals( locale, bankCode.locale ) &&
                Objects.equals( country, bankCode.country );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( code, label, locale, country );
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "class BankCode {\n" );

        sb.append( "    code: " ).append( toIndentedString( code ) ).append( "\n" );
        sb.append( "    label: " ).append( toIndentedString( label ) ).append( "\n" );
        sb.append( "    locale: " ).append( toIndentedString( locale ) ).append( "\n" );
        sb.append( "    country: " ).append( toIndentedString( country ) ).append( "\n" );
        sb.append( "}" );
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString( Object o )
    {
        if ( o == null )
        {
            return "null";
        }
        return o.toString().replace( "\n", "\n    " );
    }
}

