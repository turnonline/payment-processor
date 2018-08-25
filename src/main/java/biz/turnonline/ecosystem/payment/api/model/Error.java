package biz.turnonline.ecosystem.payment.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;


@javax.annotation.Generated( value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2018-08-25T07:08:13.381Z" )
public class Error
{

    private Integer code = null;

    private String message = null;

    /**
     * The HTTP status code.
     **/
    public Error code( Integer code )
    {
        this.code = code;
        return this;
    }


    @ApiModelProperty( required = true, value = "The HTTP status code." )
    @JsonProperty( "code" )
    public Integer getCode()
    {
        return code;
    }

    public void setCode( Integer code )
    {
        this.code = code;
    }

    /**
     * The error message.
     **/
    public Error message( String message )
    {
        this.message = message;
        return this;
    }


    @ApiModelProperty( required = true, value = "The error message." )
    @JsonProperty( "message" )
    public String getMessage()
    {
        return message;
    }

    public void setMessage( String message )
    {
        this.message = message;
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
        Error error = ( Error ) o;
        return Objects.equals( code, error.code ) &&
                Objects.equals( message, error.message );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( code, message );
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "class Error {\n" );

        sb.append( "    code: " ).append( toIndentedString( code ) ).append( "\n" );
        sb.append( "    message: " ).append( toIndentedString( message ) ).append( "\n" );
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

