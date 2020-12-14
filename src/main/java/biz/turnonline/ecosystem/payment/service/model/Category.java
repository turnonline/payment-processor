package biz.turnonline.ecosystem.payment.service.model;

import com.googlecode.objectify.annotation.Entity;
import org.ctoolkit.services.datastore.objectify.EntityLongIdentity;

import java.util.ArrayList;
import java.util.List;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Category class used to categorize incoming transactions
 *
 * @author <a href="mailto:pohorelec@turnonline.biz">Jozef Pohorelec</a>
 */
@Entity( name = "PP_Category" )
public class Category
        extends EntityLongIdentity
{
    private String color;

    private String name;

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
    protected long getModelVersion()
    {
        //10.12.2020 08:00:00 GMT+0100
        return 1607630400000L;
    }

    @Override
    public void save()
    {
        ofy().transact( () -> ofy().defer().save().entity( this ) );
    }

    @Override
    public void delete()
    {
        ofy().transact( () -> ofy().defer().delete().entity( this ) );
    }
}
