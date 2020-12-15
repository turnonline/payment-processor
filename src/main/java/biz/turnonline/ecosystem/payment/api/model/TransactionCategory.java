package biz.turnonline.ecosystem.payment.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Transaction category
 *
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public class TransactionCategory
{
    @JsonProperty( "name" )
    private String name;

    @JsonProperty( "color" )
    private String color;

    @JsonProperty( "propagate" )
    private boolean propagate;

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getColor()
    {
        return color;
    }

    public void setColor( String color )
    {
        this.color = color;
    }

    public boolean isPropagate()
    {
        return propagate;
    }

    public void setPropagate( boolean propagate )
    {
        this.propagate = propagate;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( !( o instanceof TransactionCategory ) ) return false;
        TransactionCategory that = ( TransactionCategory ) o;
        return isPropagate() == that.isPropagate() &&
                Objects.equals( getName(), that.getName() ) &&
                Objects.equals( getColor(), that.getColor() );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( getName(), getColor(), isPropagate() );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
                .add( "name", name )
                .add( "color", color )
                .add( "propagate", propagate )
                .toString();
    }
}
