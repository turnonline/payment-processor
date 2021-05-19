package biz.turnonline.ecosystem.payment.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * The counterparty bank account.
 *
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public class CounterpartyBankAccount
{
    @JsonProperty( "iban" )
    private String iban;

    @JsonProperty( "bic" )
    private String bic;

    @JsonProperty( "name" )
    private String name;

    public String getIban()
    {
        return iban;
    }

    public void setIban( String iban )
    {
        this.iban = iban;
    }

    public String getBic()
    {
        return bic;
    }

    public void setBic( String bic )
    {
        this.bic = bic;
    }

    /**
     * The bank account beneficiary name (business name).
     **/
    @JsonProperty( "name" )
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( !( o instanceof CounterpartyBankAccount ) ) return false;
        CounterpartyBankAccount that = ( CounterpartyBankAccount ) o;
        return Objects.equals( getIban(), that.getIban() ) &&
                Objects.equals( getBic(), that.getBic() );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( getIban(), getBic() );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
                .add( "iban", iban )
                .add( "bic", bic )
                .add( "name", name )
                .toString();
    }
}
