package biz.turnonline.ecosystem.payment.service.model;

import biz.turnonline.ecosystem.account.client.model.Account;
import biz.turnonline.ecosystem.account.client.model.Domicile;
import biz.turnonline.ecosystem.payment.service.CodeBook;
import biz.turnonline.ecosystem.payment.service.SecretKeyConfig;
import biz.turnonline.ecosystem.payment.service.TwoWayEncryption;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.OnSave;
import org.ctoolkit.services.storage.HasOwner;
import org.ctoolkit.services.storage.appengine.objectify.EntityLongIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Locale;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * The bank account datastore entity.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Entity( name = "PP_BankAccount" )
public class BankAccount
        extends EntityLongIdentity
        implements Comparable<BankAccount>, HasOwner<LocalAccount>
{
    public static final String TRUST_PAY_BANK_CODE = "9952";

    private static final Logger logger = LoggerFactory.getLogger( BankAccount.class );

    private static final long serialVersionUID = 3672525651272337328L;

    private final CodeBook codeBook;

    @Index
    private Ref<LocalAccount> owner;

    @Index
    private String code;

    @Index
    private String name;

    private String prefix;

    private String accountNumber;

    private String bankCode;

    private String iban;

    private String swift;

    private Domicile country;

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
    public BankAccount( CodeBook codeBook )
    {
        this.codeBook = codeBook;
    }

    /**
     * <p>Format bank account specified in bank account fields.</p>
     * <i>Formatted results:</i>
     * <table border="1">
     * <tr>
     * <th>Prefix</th><th>Bank account number</th><th>Bank code</th><th>Result</th>
     * </tr>
     * <tr>
     * <td>''</td><td>''</td><td>''</td><td>''</td>
     * </tr>
     * <tr>
     * <td>''</td><td>''</td><td>1111</td><td>''</td>
     * </tr>
     * <tr>
     * <td>''</td><td>123456</td><td>1111</td><td>123456/1111</td>
     * </tr>
     * <tr>
     * <td>000001</td><td>123456</td><td>1111</td><td>000001-123456/1111</td>
     * </tr>
     * </table>
     *
     * @return formatted bank account
     */
    public String getFormattedBankAccount()
    {
        StringBuilder sb = new StringBuilder();
        if ( this.getAccountNumber() == null )
        {
            return sb.toString();
        }

        String prefix = this.getPrefix();
        String bankAccountNumber = this.getAccountNumber();
        String bankCode = this.getBankCode();

        if ( prefix != null && !prefix.trim().isEmpty() )
        {
            sb.append( prefix ).append( "-" );
        }

        sb.append( bankAccountNumber );

        if ( bankCode != null && !bankCode.trim().isEmpty() )
        {
            sb.append( "/" ).append( bankCode );
        }

        return sb.toString();
    }

    /**
     * Returns the localized bank account description, the bank name (etc.) taken from the bank code code-book.
     *
     * @return the bank account description
     */
    public BankAccount.Description getLocalizedDescription( @Nullable Locale locale )
    {
        LocalAccount owner = getOwner();
        if ( owner == null )
        {
            throw new IllegalArgumentException();
        }

        Account account = owner.getAccount();
        String domicile = country == null ? null : country.name();

        Map<String, BankCode> codes = codeBook.getBankCodes( account, locale, domicile );

        BankAccount.Description description = null;
        BankCode bankCode = codes.get( this.getBankCode() );
        if ( bankCode == null )
        {
            logger.warn( "No BankCode has found for " + this );
        }
        else
        {
            if ( !BankAccount.TRUST_PAY_BANK_CODE.equals( bankCode.getCode() ) )
            {
                description = new BankAccount.Description( bankCode.getLabel(),
                        getFormattedBankAccount(),
                        getIban(),
                        getSwift() );
            }

        }

        return description;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode( String code )
    {
        this.code = code;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getPrefix()
    {
        return prefix;
    }

    public void setPrefix( String prefix )
    {
        this.prefix = prefix;
    }

    public String getAccountNumber()
    {
        return accountNumber;
    }

    public void setAccountNumber( String accountNumber )
    {
        this.accountNumber = accountNumber;
    }

    public String getBankCode()
    {
        return bankCode;
    }

    public void setBankCode( String bankCode )
    {
        this.bankCode = bankCode;
    }

    public String getIban()
    {
        return iban;
    }

    public void setIban( String iban )
    {
        this.iban = iban;
    }

    public String getSwift()
    {
        return swift;
    }

    public void setSwift( String swift )
    {
        this.swift = swift;
    }

    /**
     * Returns the country code where is the bank account located
     *
     * @return the country code
     */
    public Domicile getCountry()
    {
        return country;
    }

    /**
     * Sets the country code where is the bank account located
     *
     * @param country the country code to be set
     */
    public void setCountry( Domicile country )
    {
        this.country = country;
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
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( !( o instanceof BankAccount ) ) return false;

        BankAccount that = ( BankAccount ) o;

        if ( super.getId() != null ? !super.getId().equals( that.getId() ) : that.getId() != null ) return false;
        if ( accountNumber != null ? !accountNumber.equals( that.accountNumber ) : that.accountNumber != null )
            return false;
        if ( prefix != null ? !prefix.equals( that.prefix ) : that.prefix != null )
            return false;
        //noinspection RedundantIfStatement
        return !( bankCode != null ? !bankCode.equals( that.bankCode ) : that.bankCode != null );

    }

    @Override
    public int hashCode()
    {
        int result = super.getId() != null ? super.getId().hashCode() : 0;
        result = 31 * result + ( prefix != null ? prefix.hashCode() : 0 );
        result = 31 * result + ( accountNumber != null ? accountNumber.hashCode() : 0 );
        result = 31 * result + ( bankCode != null ? bankCode.hashCode() : 0 );
        return result;
    }

    @Override
    public String toString()
    {
        String sKey = secretKey == null ? "null" : "not null, length: " + secretKey.length();
        return MoreObjects.toStringHelper( getKind() )
                .add( "owner", owner )
                .add( "name", name )
                .add( "prefix", prefix )
                .add( "accountNumber", accountNumber )
                .add( "bankCode", bankCode )
                .add( "iban", iban )
                .add( "swift", swift )
                .add( "country", country )
                .add( "paymentGate", paymentGate )
                .add( "merchantId", merchantId )
                .add( "secretKey", sKey )
                .add( "notificationEmail", notificationEmail )
                .add( "primary", primary )
                .add( "gateEnabled", gateEnabled )
                .toString();
    }

    @Override
    public int compareTo( @Nonnull BankAccount bankAccount )
    {
        return ComparisonChain.start()
                .compare( this.name, bankAccount.getName() )
                .compare( this.bankCode, bankAccount.getBankCode() )
                .compare( this.accountNumber, bankAccount.getAccountNumber() )
                .compare( this.prefix, bankAccount.getPrefix() )
                .result();
    }

    @Override
    public void save()
    {
        ofy().transact( () -> {
            ofy().save().entity( BankAccount.this ).now();
        } );
    }

    @Override
    public void delete()
    {
        ofy().transact( () -> {
            ofy().delete().entity( BankAccount.this ).now();
        } );
    }

    @Override
    public LocalAccount getOwner()
    {
        return fromRef( owner, null );
    }

    @Override
    public void setOwner( @Nonnull LocalAccount owner )
    {
        checkNotNull( owner );
        this.owner = Ref.create( owner );
    }

    @Override
    public boolean checkOwner( @Nonnull LocalAccount checked )
    {
        checkNotNull( checked );
        return owner != null && owner.get() != null && checked.equals( owner.get() );
    }

    @Override
    public String getKind()
    {
        return "BankAccount";
    }

    public static class Description
    {
        private String bankName;

        private String fullBankAccountNumber;

        private String iban;

        private String swift;

        public Description( String bankName, String fullBankAccountNumber, String iban, String swift )
        {
            this.bankName = bankName;
            this.fullBankAccountNumber = fullBankAccountNumber;
            this.iban = iban;
            this.swift = swift;
        }

        public String getBankName()
        {
            return bankName;
        }

        public String getFullBankAccountNumber()
        {
            return fullBankAccountNumber;
        }

        public String getIban()
        {
            return iban;
        }

        public String getSwift()
        {
            return swift;
        }

        @Override
        public String toString()
        {
            return MoreObjects.toStringHelper( this )
                    .add( "bankName", bankName )
                    .add( "fullBankAccountNumber", fullBankAccountNumber )
                    .add( "iban", iban )
                    .add( "swift", swift )
                    .toString();
        }
    }
}
