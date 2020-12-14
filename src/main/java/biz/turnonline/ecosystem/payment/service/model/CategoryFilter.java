package biz.turnonline.ecosystem.payment.service.model;

/**
 * Filter used to determine category on transaction
 *
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public class CategoryFilter
{
    private String propertyValue;

    private PropertyName propertyName;

    private Operation operation;

    public String getPropertyValue()
    {
        return propertyValue;
    }

    public void setPropertyValue( String propertyValue )
    {
        this.propertyValue = propertyValue;
    }

    public PropertyName getPropertyName()
    {
        return propertyName;
    }

    public void setPropertyName( PropertyName propertyName )
    {
        this.propertyName = propertyName;
    }

    public Operation getOperation()
    {
        return operation;
    }

    public void setOperation( Operation operation )
    {
        this.operation = operation;
    }

    public enum PropertyName
    {
        NAME,
        AMOUNT,
        CURRENCY,
        CREDIT,
        COUNTERPARTY_IBAN
    }

    public enum Operation {
        LT,
        LTE,
        GTE,
        GT,
        EQ,
        REGEXP
    }
}
