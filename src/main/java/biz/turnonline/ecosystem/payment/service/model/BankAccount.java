package biz.turnonline.ecosystem.payment.service.model;

import biz.turnonline.ecosystem.payment.service.CodeBook;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.IgnoreSave;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.condition.IfNull;
import nl.garvelink.iban.IBAN;
import nl.garvelink.iban.IBANFields;
import nl.garvelink.iban.Modulo97;
import org.ctoolkit.services.datastore.objectify.EntityLongIdentity;
import org.ctoolkit.services.storage.HasOwner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Common class for all bank account entity types.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Entity( name = "PP_BankAccount" )
public abstract class BankAccount
        extends EntityLongIdentity
        implements Comparable<BankAccount>, HasOwner<LocalAccount>
{
    private static final Logger logger = LoggerFactory.getLogger( BankAccount.class );

    private static final long serialVersionUID = 3735811829765087171L;

    private final CodeBook codeBook;

    @Index
    private Ref<LocalAccount> owner;

    @Index
    private String name;

    private String branch;

    private String bankCode;

    @Index
    private String iban;

    private String bic;

    private String currency;

    @Index
    private String country;

    @IgnoreSave( IfNull.class )
    private Map<String, String> extIds;

    public BankAccount( CodeBook codeBook )
    {
        this.codeBook = codeBook;
        this.extIds = new HashMap<>();
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

        return ( bankCode == null || PaymentConfig.TRUST_PAY_BANK_CODE.equals( bankCode.getCode() ) )
                ? null : bankCode.getLabel();
    }

    /**
     * Returns the external identification of the debtor's bank account within the same bank.
     *
     * @return the external ID or {@code null} if not updated yet
     */
    protected String getExternalId()
    {
        return getExternalId( bankCode );
    }

    /**
     * Sets the external identification of the debtor's bank account within the same bank.
     *
     * @param externalId identification of this bank account within the bank represented by the bank code
     */
    protected void setExternalId( @Nullable String externalId )
    {
        setExternalId( bankCode, externalId );
    }

    /**
     * Returns the external identification of the bank account for specified bank synchronized via API, if any.
     *
     * @param code the bank code, taken from the code-book
     * @return the external ID or {@code null} if not synchronized yet
     */
    protected String getExternalId( @Nonnull String code )
    {
        if ( extIds == null )
        {
            return null;
        }

        return extIds.get( checkNotNull( code, "Bank code can't be null" ) );
    }

    /**
     * Sets the external identification for the specified bank once synchronized in to bank system.
     *
     * @param code       the bank code, taken from the code-book
     * @param externalId identification of this bank account within the bank represented by the bank code
     */
    protected void setExternalId( @Nonnull String code, @Nullable String externalId )
    {
        checkNotNull( code, "Bank code can't be null" );
        extIds.put( code, externalId );
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
     * The bank branch identification.
     */
    public String getBranch()
    {
        return branch;
    }

    public void setBranch( String branch )
    {
        this.branch = branch;
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
     * @param input the IBAN to be set
     * @throws IllegalArgumentException if IBAN validation fails
     */
    public void setIban( @Nonnull String input )
    {
        if ( !Modulo97.verifyCheckDigits( input ) )
        {
            throw new IllegalArgumentException( "Invalid IBAN: " + input );
        }

        IBAN valueOfIBAN = IBAN.valueOf( input );

        country = valueOfIBAN.getCountryCode();
        bankCode = IBANFields.getBankIdentifier( valueOfIBAN ).orElse( null );
        branch = IBANFields.getBranchIdentifier( valueOfIBAN ).orElse( null );

        // IBAN stored as a compact string to be easily searchable
        iban = valueOfIBAN.toPlainString();
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

    @OnSave
    void onSave()
    {
        if ( this.country != null )
        {
            this.country = this.country.toUpperCase();
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
        return Objects.equals( owner, that.owner ) &&
                Objects.equals( iban, that.iban ) &&
                Objects.equals( currency, that.currency );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( owner, iban, currency );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( getKind() )
                .add( "owner", owner )
                .add( "name", name )
                .add( "branch", branch )
                .add( "bankCode", bankCode )
                .add( "iban", iban )
                .add( "bic", bic )
                .add( "currency", currency )
                .add( "country", country )
                .add( "externalIds", extIds )
                .toString();
    }

    @Override
    public int compareTo( @Nonnull BankAccount bankAccount )
    {
        return ComparisonChain.start()
                .compare( this.name, bankAccount.getName(), Ordering.natural().nullsLast() )
                .compare( this.bankCode, bankAccount.getBankCode(), Ordering.natural().nullsLast() )
                .compare( this.branch, bankAccount.getBranch(), Ordering.natural().nullsLast() )
                .result();
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
}
