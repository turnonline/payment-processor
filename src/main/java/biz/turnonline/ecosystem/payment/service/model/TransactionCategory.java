package biz.turnonline.ecosystem.payment.service.model;

import java.io.Serializable;

/**
 * Applied transaction category based on {@link CategoryFilter}
 *
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
public class TransactionCategory
        implements Serializable
{
    private static final long serialVersionUID = -3043597735466378761L;

    private String name;

    private String color;

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
}
