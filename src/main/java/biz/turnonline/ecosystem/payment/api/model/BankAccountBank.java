package biz.turnonline.ecosystem.payment.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;


@javax.annotation.Generated( value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2018-08-26T12:15:58.121Z" )
public class BankAccountBank
{

    private String code = null;

    private String label = null;

    private String country = null;

    /**
     * The country specific numeric bank code, taken from the code-book.
     **/
    public BankAccountBank code( String code )
    {
        this.code = code;
        return this;
    }


    @ApiModelProperty( required = true, value = "The country specific numeric bank code, taken from the code-book." )
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
     * The localized name of the bank, taken from the code-book and based on either default or specified language.   The value will be managed by the service once Accept-Language header will be provided while bank account getting.
     **/
    public BankAccountBank label( String label )
    {
        this.label = label;
        return this;
    }


    @ApiModelProperty( value = "The localized name of the bank, taken from the code-book and based on either default or specified language.   The value will be managed by the service once Accept-Language header will be provided while bank account getting." )
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
     * The country of the bank where bank account has been opened. The missing value will be taken from the codebook if that combination is being found. The ISO 3166 alpha-2 country code. It’s case insensitive.  Note: Currently supported only SK and CZ.
     **/
    public BankAccountBank country( String country )
    {
        this.country = country;
        return this;
    }


    @ApiModelProperty( value = "The country of the bank where bank account has been opened. The missing value will be taken from the codebook if that combination is being found. The ISO 3166 alpha-2 country code. It’s case insensitive.  Note: Currently supported only SK and CZ." )
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
        BankAccountBank bankAccountBank = ( BankAccountBank ) o;
        return Objects.equals( code, bankAccountBank.code ) &&
                Objects.equals( label, bankAccountBank.label ) &&
                Objects.equals( country, bankAccountBank.country );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( code, label, country );
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "class BankAccountBank {\n" );

        sb.append( "    code: " ).append( toIndentedString( code ) ).append( "\n" );
        sb.append( "    label: " ).append( toIndentedString( label ) ).append( "\n" );
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

