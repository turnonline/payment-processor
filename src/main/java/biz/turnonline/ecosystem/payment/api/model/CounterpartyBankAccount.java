package biz.turnonline.ecosystem.payment.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public class CounterpartyBankAccount
{
    @JsonProperty( "iban" )
    private String iban;

    @JsonProperty( "bic" )
    private String bic;

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
                .toString();
    }
}
