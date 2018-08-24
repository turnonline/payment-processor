package biz.turnonline.ecosystem.payment.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;


@javax.annotation.Generated( value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2018-08-24T04:20:12.761Z" )
public class BankAccountBank
{

    private String code = null;

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
        return Objects.equals( code, bankAccountBank.code );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( code );
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "class BankAccountBank {\n" );

        sb.append( "    code: " ).append( toIndentedString( code ) ).append( "\n" );
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

