/*
 * Payment Processor
 * TurnOnline.biz Ecosystem Payment Processor
 *
 * OpenAPI spec version: 1.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package biz.turnonline.ecosystem.payment.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * BankOnboard
 */
public class BankOnboard
{
    @JsonProperty( "clientId" )
    private String clientId = null;

    public BankOnboard clientId( String clientId )
    {
        this.clientId = clientId;
        return this;
    }

    /**
     * Identification of the app within the bank.
     **/
    @JsonProperty( "clientId" )
    public String getClientId()
    {
        return clientId;
    }

    public void setClientId( String clientId )
    {
        this.clientId = clientId;
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
        BankOnboard bankOnboard = ( BankOnboard ) o;
        return Objects.equals( this.clientId, bankOnboard.clientId );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( clientId );
    }

    @Override
    public String toString()
    {
        return "class BankOnboard {\n" +
                "    clientId: " + toIndentedString( clientId ) + "\n" +
                "}";
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
