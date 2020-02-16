package biz.turnonline.ecosystem.payment.oauth;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.OnSave;
import org.ctoolkit.services.datastore.objectify.EntityStringIdentity;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * The entity to keep current details of Revolut certificate, not sensitive data.
 * The entity identification is being based on the issuer (domain based) that is unique for microservice.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Entity( name = "PP_RevolutCertDetails" )
public class RevolutCertDetails
        extends EntityStringIdentity
{
    public static final String PRIVATE_KEY_NAME = "Revolut_private_key";

    private static final long serialVersionUID = 1858841274474980291L;

    private String clientId;

    private String code;

    /**
     * The private key name, initialized with default value
     */
    private String keyName = PRIVATE_KEY_NAME;

    /**
     * Returns the certificate client ID configured by user.
     *
     * @return the client ID
     */
    public String getClientId()
    {
        return clientId;
    }

    public RevolutCertDetails setClientId( String clientId )
    {
        this.clientId = clientId;
        return this;
    }

    /**
     * Returns the authorisation code processed while OAuth redirect URI
     *
     * @return the authorisation code
     */
    public String getCode()
    {
        return code;
    }

    public RevolutCertDetails setCode( String code )
    {
        this.code = code;
        return this;
    }

    /**
     * Returns the secret manager private key name configured by user, or default one.
     *
     * @return private key name
     */
    public String getKeyName()
    {
        return keyName;
    }

    public RevolutCertDetails setKeyName( String keyName )
    {
        this.keyName = keyName;
        return this;
    }

    @OnSave
    void onSave()
    {
        if ( keyName == null )
        {
            keyName = PRIVATE_KEY_NAME;
        }
    }

    @Override
    protected long getModelVersion()
    {
        //21.10.2017 08:00:00 GMT+0200
        return 1508565600000L;
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
