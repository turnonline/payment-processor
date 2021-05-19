package biz.turnonline.ecosystem.payment.service.model;

import com.google.common.base.MoreObjects;

import java.io.Serializable;
import java.util.Objects;

/**
 * Counterparty bank account
 *
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public class CounterpartyBankAccount
        implements Serializable
{
    private static final long serialVersionUID = -1562498048029592821L;

    private String iban;

    private String bic;

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
