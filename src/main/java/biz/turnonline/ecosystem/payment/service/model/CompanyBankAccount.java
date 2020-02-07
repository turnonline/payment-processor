package biz.turnonline.ecosystem.payment.service.model;

import biz.turnonline.ecosystem.payment.service.CodeBook;
import biz.turnonline.ecosystem.payment.service.SecretKeyConfig;
import biz.turnonline.ecosystem.payment.service.TwoWayEncryption;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.annotation.Subclass;
import nl.garvelink.iban.IBAN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * The company bank account.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Subclass( name = "Company", index = true )
public class CompanyBankAccount
        extends BankAccount
{
    private static final Logger logger = LoggerFactory.getLogger( CompanyBankAccount.class );

    private static final long serialVersionUID = -8003937716992437433L;

    @Index
    private String code;

    @Index
    private PaymentGate paymentGate;

    @Index
    private String merchantId;

    private String secretKey;

    private String notificationEmail;

    private boolean primary;

    private boolean gateEnabled;

    @Ignore
    private String tSecretKey;

    @Inject
    public CompanyBankAccount( CodeBook codeBook )
    {
        super( codeBook );
    }

    @Override
    public String getExternalId()
    {
        return super.getExternalId();
    }

    @Override
    public void setExternalId( @Nullable String externalId )
    {
        super.setExternalId( externalId );
    }

    public String getCode()
    {
        return code;
    }

    public void setCode( String code )
    {
        this.code = code;
    }

    /**
     * Returns the international bank account number as formatted string.
     */
    public String getIbanString()
    {
        IBAN iban = getIBAN();
        return iban == null ? null : iban.toString();
    }

    public PaymentGate getPaymentGate()
    {
        return paymentGate;
    }

    public void setPaymentGate( PaymentGate paymentGate )
    {
        this.paymentGate = paymentGate;
    }

    public String getMerchantId()
    {
        return merchantId;
    }

    public void setMerchantId( String merchantId )
    {
        this.merchantId = merchantId;
    }

    public String getSecretKey()
    {
        return tSecretKey;
    }

    public void setSecretKey( String secretKey )
    {
        this.tSecretKey = secretKey;
    }

    public String getNotificationEmail()
    {
        return notificationEmail;
    }

    public void setNotificationEmail( String notificationEmail )
    {
        this.notificationEmail = notificationEmail;
    }

    /**
     * Boolean identification, whether this bank account is being marked by the user as a primary account.
     * If yes, this bank account will be used as a default account unless specified another one.
     * There is always only single primary bank account per country.
     */
    public boolean isPrimary()
    {
        return primary;
    }

    public void setPrimary( boolean primary )
    {
        this.primary = primary;
    }

    public boolean isGateEnabled()
    {
        return gateEnabled;
    }

    public void setGateEnabled( boolean gateEnabled )
    {
        this.gateEnabled = gateEnabled;
    }


    /**
     * Returns the boolean indication whether this bank account is valid to be debited via API.
     * <p>
     * To be ready it must have a non null value for following properties:
     * <ul>
     *     <li>{@link #getIBAN()}</li>
     *     <li>{@link #getCurrency()}</li>
     *     <li>{@link #getExternalId()}</li>
     *     <li>{@link #getBankCode()} ()}</li>
     * </ul>
     *
     * @return true if the bank account is ready
     */
    public boolean isDebtorReady()
    {
        return getIBAN() != null
                && !Strings.isNullOrEmpty( getCurrency() )
                && !Strings.isNullOrEmpty( getExternalId() )
                && !Strings.isNullOrEmpty( getBankCode() );
    }

    @OnSave
    void onSave()
    {
        if ( this.tSecretKey != null )
        {
            try
            {
                this.secretKey = TwoWayEncryption.encrypt( this.tSecretKey, SecretKeyConfig.TWO_WAY_HASH_SECRET_KEY );
            }
            catch ( Exception e )
            {
                logger.error( "Error has occurred during bank account secret key encryption", e );
            }
        }
    }

    @OnLoad
    private void onLoad()
    {
        // Decrypt secret key on bank account on load.
        if ( this.secretKey != null )
        {
            try
            {
                this.tSecretKey = TwoWayEncryption.decrypt( this.secretKey, SecretKeyConfig.TWO_WAY_HASH_SECRET_KEY );
            }
            catch ( Exception e )
            {
                logger.error( "Error has occurred during bank account secret key decryption", e );
            }
        }
    }

    @Override
    protected long getModelVersion()
    {
        //21.10.2017 08:00:00 GMT+0200
        return 1508565600000L;
    }

    @Override
    public String toString()
    {
        String sKey = secretKey == null ? "null" : "not null, length: " + secretKey.length();
        return MoreObjects.toStringHelper( this )
                .addValue( super.toString() )
                .add( "code", code )
                .add( "paymentGate", paymentGate )
                .add( "merchantId", merchantId )
                .add( "secretKey", secretKey )
                .add( "notificationEmail", notificationEmail )
                .add( "primary", primary )
                .add( "gateEnabled", gateEnabled )
                .add( "tSecretKey", sKey )
                .toString();
    }

    @Override
    public String getKind()
    {
        return "CompanyBankAccount";
    }
}
