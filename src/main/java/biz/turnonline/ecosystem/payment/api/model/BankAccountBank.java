package biz.turnonline.ecosystem.payment.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;


@javax.annotation.Generated( value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2018-08-25T07:08:13.381Z" )
public class BankAccountBank
{

    private String code = null;

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
     * The country of the bank where bank account has been opened. The ISO 3166 alpha-2 country code. It’s case insensitive.  Note: Currently supported only SK and CZ.
     **/
    public BankAccountBank country( String country )
    {
        this.country = country;
        return this;
    }


    @ApiModelProperty( value = "The country of the bank where bank account has been opened. The ISO 3166 alpha-2 country code. It’s case insensitive.  Note: Currently supported only SK and CZ." )
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
                Objects.equals( country, bankAccountBank.country );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( code, country );
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "class BankAccountBank {\n" );

        sb.append( "    code: " ).append( toIndentedString( code ) ).append( "\n" );
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

