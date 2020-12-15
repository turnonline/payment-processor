package biz.turnonline.ecosystem.payment.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Filter used to determine category on transaction
 *
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public class CategoryFilter
{
    @JsonProperty( "propertyValue" )
    private String propertyValue;

    @JsonProperty( "propertyName" )
    private String propertyName;

    @JsonProperty( "operation" )
    private String operation;

    public String getPropertyValue()
    {
        return propertyValue;
    }

    public void setPropertyValue( String propertyValue )
    {
        this.propertyValue = propertyValue;
    }

    public String getPropertyName()
    {
        return propertyName;
    }

    public void setPropertyName( String propertyName )
    {
        this.propertyName = propertyName;
    }

    public String getOperation()
    {
        return operation;
    }

    public void setOperation( String operation )
    {
        this.operation = operation;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( !( o instanceof CategoryFilter ) ) return false;
        CategoryFilter that = ( CategoryFilter ) o;
        return Objects.equals( getPropertyValue(), that.getPropertyValue() ) &&
                Objects.equals( getPropertyName(), that.getPropertyName() ) &&
                Objects.equals( getOperation(), that.getOperation() );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( getPropertyValue(), getPropertyName(), getOperation() );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
                .add( "propertyValue", propertyValue )
                .add( "propertyName", propertyName )
                .add( "operation", operation )
                .toString();
    }
}
