package biz.turnonline.ecosystem.payment.service.model;

import biz.turnonline.ecosystem.payment.service.CodeBook;
import biz.turnonline.ecosystem.payment.service.SecretKeyConfig;
import biz.turnonline.ecosystem.payment.service.TwoWayEncryption;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.IgnoreSave;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.condition.IfNull;
import nl.garvelink.iban.IBAN;
import nl.garvelink.iban.Modulo97;
import org.ctoolkit.services.datastore.objectify.EntityLongIdentity;
import org.ctoolkit.services.storage.HasOwner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Currency;
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

    private static final long serialVersionUID = -4153441408312149984L;

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

    @Index
    private String iban;

    private String bic;

    private String currency;

    @Index
    private String country;

    @Index
    private PaymentGate paymentGate;

    @Index
    private String merchantId;

    private String secretKey;

    private String notificationEmail;

    private boolean primary;

    private boolean gateEnabled;

    @IgnoreSave( IfNull.class )
    private Map<String, String> extIds;

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
     * Returns the localized bank account label, the bank name (etc.) taken from the bank code code-book.
     *
     * @return the bank account description
     */
    public String getLocalizedLabel( @Nullable Locale locale )
    {
        LocalAccount owner = getOwner();
        if ( owner == null )
        {
            throw new IllegalArgumentException();
        }

        Map<String, BankCode> codes = codeBook.getBankCodes( owner, locale, country );

        BankCode bankCode = codes.get( this.getBankCode() );
        if ( bankCode == null )
        {
            logger.warn( "No BankCode has found for " + this );
        }

        return ( bankCode == null || BankAccount.TRUST_PAY_BANK_CODE.equals( bankCode.getCode() ) )
                ? null : bankCode.getLabel();
    }

    /**
     * Returns the external identification of the debtor's bank account for synchronized via API, if any.
     *
     * @return the external ID or {@code null} if not synchronized yet
     */
    public String getExternalId()
    {
        if ( extIds == null || Strings.isNullOrEmpty( bankCode ) )
        {
            return null;
        }

        return extIds.get( bankCode );
    }

    /**
     * Returns the external identification of the bank account for specified bank synchronized via API, if any.
     *
     * @param code the bank code, taken from the code-book
     * @return the external ID or {@code null} if not synchronized yet
     */
    public String getExternalId( @Nullable String code )
    {
        if ( extIds == null || Strings.isNullOrEmpty( code ) )
        {
            return null;
        }

        return extIds.get( code );
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
     * The user defined name of the bank account.
     */
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    /**
     * The optional bank account number prefix.
     */
    public String getPrefix()
    {
        return prefix;
    }

    public void setPrefix( String prefix )
    {
        this.prefix = prefix;
    }

    /**
     * The bank account number.
     */
    public String getAccountNumber()
    {
        return accountNumber;
    }

    public void setAccountNumber( String accountNumber )
    {
        this.accountNumber = accountNumber;
    }

    /**
     * The country specific numeric bank code, taken from the code-book.
     */
    public String getBankCode()
    {
        return bankCode;
    }

    public void setBankCode( String bankCode )
    {
        this.bankCode = bankCode;
    }

    /**
     * Returns the international bank account number as formatted string.
     */
    public String getIbanString()
    {
        IBAN iban = getIBAN();
        return iban == null ? null : iban.toString();
    }

    /**
     * The international bank account number.
     */
    public IBAN getIBAN()
    {
        return Strings.isNullOrEmpty( iban ) ? null : IBAN.valueOf( iban );
    }

    /**
     * Validates the given IBAN and sets the value if pass.
     *
     * @param iban the IBAN to be set
     * @throws IllegalArgumentException if IBAN validation fails
     */
    public void setIban( @Nonnull String iban )
    {
        if ( !Modulo97.verifyCheckDigits( iban ) )
        {
            throw new IllegalArgumentException( "Invalid IBAN: " + iban );
        }

        // IBAN stored as a compact string to be easily searchable
        this.iban = IBAN.valueOf( iban ).toPlainString();
    }

    /**
     * The international Bank Identifier Code (BIC/ISO 9362),
     * a normalized code - also known as Business Identifier Code, Bank International Code and SWIFT code.
     */
    public String getBic()
    {
        return bic;
    }

    public void setBic( String bic )
    {
        this.bic = bic;
    }

    /**
     * The bank account currency. An alphabetic code based on the ISO 4217.
     */
    public String getCurrency()
    {
        return currency;
    }

    /**
     * Sets the currency alphabetic code based on the ISO 4217.
     *
     * @param currency the currency alphabetic code
     * @throws IllegalArgumentException if <code>currency</code> is not a supported ISO 4217 code
     */
    public void setCurrency( String currency )
    {
        Currency validCurrency = Currency.getInstance( currency );
        this.currency = validCurrency.getCurrencyCode();
    }

    /**
     * Returns the country of the bank where bank account has been opened.
     *
     * @return the country code
     */
    public String getCountry()
    {
        return country;
    }

    /**
     * Sets the country of the bank where bank account has been opened.
     *
     * @param country the country code to be set
     */
    public void setCountry( String country )
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
     * Returns the boolean indication whether this bank account represents a Revolut bank account.
     *
     * @return true if this bank account is Revolut account
     */
    public boolean isRevolut()
    {
        return "REVO".equalsIgnoreCase( bankCode );
    }

    /**
     * Returns the boolean indication whether this bank account is valid to be debited via API
     * for specified bank identified by bank code.
     * <p>
     * To be ready it must have a non null value for following properties:
     * <ul>
     *     <li>{@link #getCurrency()}</li>
     *     <li>{@link #getIBAN()}</li>
     *     <li>{@link #getExternalId()}</li>
     * </ul>
     *
     * @return true if the bank account is ready
     */
    public boolean isDebtorReadyFor()
    {
        return true;
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

        if ( this.country != null )
        {
            this.country = this.country.toUpperCase();
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
                .add( "bic", bic )
                .add( "country", country )
                .add( "paymentGate", paymentGate )
                .add( "merchantId", merchantId )
                .add( "secretKey", sKey )
                .add( "notificationEmail", notificationEmail )
                .add( "primary", primary )
                .add( "gateEnabled", gateEnabled )
                .add( "externalIds", extIds )
                .toString();
    }

    @Override
    public int compareTo( @Nonnull BankAccount bankAccount )
    {
        return ComparisonChain.start()
                .compare( this.name, bankAccount.getName(), Ordering.natural().nullsLast() )
                .compare( this.bankCode, bankAccount.getBankCode(), Ordering.natural().nullsLast() )
                .compare( this.accountNumber, bankAccount.getAccountNumber(), Ordering.natural().nullsLast() )
                .compare( this.prefix, bankAccount.getPrefix(), Ordering.natural().nullsLast() )
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
    public String getKind()
    {
        return "BankAccount";
    }
}
