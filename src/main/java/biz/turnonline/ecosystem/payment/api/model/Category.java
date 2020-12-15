package biz.turnonline.ecosystem.payment.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Category is used to categorize incoming transactions
 *
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public class Category
{
    @JsonProperty( "color" )
    private String color;

    @JsonProperty( "name" )
    private String name;

    @JsonProperty( "propagate" )
    private boolean propagate;

    private List<CategoryFilter> filters = new ArrayList<>();

    public String getColor()
    {
        return color;
    }

    public void setColor( String color )
    {
        this.color = color;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public boolean isPropagate()
    {
        return propagate;
    }

    public void setPropagate( boolean propagate )
    {
        this.propagate = propagate;
    }

    public List<CategoryFilter> getFilters()
    {
        return filters;
    }

    public void setFilters( List<CategoryFilter> filters )
    {
        this.filters = filters;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( !( o instanceof Category ) ) return false;
        Category category = ( Category ) o;
        return isPropagate() == category.isPropagate() &&
                Objects.equals( getColor(), category.getColor() ) &&
                Objects.equals( getName(), category.getName() ) &&
                Objects.equals( getFilters(), category.getFilters() );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( getColor(), getName(), isPropagate(), getFilters() );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
                .add( "color", color )
                .add( "name", name )
                .add( "propagate", propagate )
                .add( "filters", filters )
                .toString();
    }
}
