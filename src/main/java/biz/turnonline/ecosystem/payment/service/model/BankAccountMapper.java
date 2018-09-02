package biz.turnonline.ecosystem.payment.service.model;

import biz.turnonline.ecosystem.account.client.model.Account;
import biz.turnonline.ecosystem.payment.api.model.BankAccountBank;
import biz.turnonline.ecosystem.payment.service.ApiValidationException;
import biz.turnonline.ecosystem.payment.service.CodeBook;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.Optional;

/**
 * Mapper: {@link BankAccount} back and forth {@link biz.turnonline.ecosystem.payment.api.model.BankAccount}.
 * It supports patch semantics for direction from API to Backend, however
 * following properties are being ignored as they are managed solely by the backend service.
 * <ul>
 * <li>id</li>k
 * <li>formatted</li>
 * <li>bank.label</li>
 * </ul>
 * In order to make property 'bank.label' locale sensitive for direction Backend to API,
 * make sure the context property is being set: {@code context.setProperty( HttpHeaders.ACCEPT_LANGUAGE, language );}.
 * <p>
 * The direction API to Backend performs validation and might throw {@link ApiValidationException}.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
class BankAccountMapper
        extends CustomMapper<BankAccount, biz.turnonline.ecosystem.payment.api.model.BankAccount>
{
    private final CodeBook codeBook;

    @Inject
    BankAccountMapper( CodeBook codeBook )
    {
        this.codeBook = codeBook;
    }

    @Override
    public void mapAtoB( BankAccount source,
                         biz.turnonline.ecosystem.payment.api.model.BankAccount bankAccount,
                         MappingContext context )
    {
        bankAccount.setId( source.getId() );
        bankAccount.setName( source.getName() );
        bankAccount.setPrefix( source.getPrefix() );
        bankAccount.setAccountNumber( source.getAccountNumber() );
        bankAccount.setIban( source.getIban() );
        bankAccount.setBic( source.getBic() );
        bankAccount.setCurrency( source.getCurrency() );
        bankAccount.setPrimary( source.isPrimary() );

        String formatted = source.getFormattedBankAccount();
        bankAccount.setFormatted( Strings.isNullOrEmpty( formatted ) ? null : formatted );

        String bankCode = source.getBankCode();
        if ( bankCode != null )
        {
            Locale locale = ( Locale ) context.getProperty( HttpHeaders.ACCEPT_LANGUAGE );

            BankAccountBank bank = new BankAccountBank();
            bank.setCode( bankCode );
            bank.setLabel( source.getLocalizedLabel( locale ) );
            bank.setCountry( source.getCountry() );

            bankAccount.setBank( bank );
        }
    }

    @Override
    public void mapBtoA( biz.turnonline.ecosystem.payment.api.model.BankAccount source,
                         BankAccount backend,
                         MappingContext context )
    {
        Optional<String> sValue;
        BankAccountBank bank = source.getBank();

        if ( bank != null )
        {
            String code = bank.getCode();
            if ( code == null )
            {
                String key = "errors.validation.mandatory.property.missing";
                throw ApiValidationException.prepare( key, "bank.code" );
            }

            Account account = ( Account ) context.getProperty( Account.class );
            if ( account == null )
            {
                String message = "Authenticated account is mandatory, expected as a MappingContext property with key: "
                        + Account.class;
                throw new IllegalArgumentException( message );
            }

            String country = bank.getCountry();
            BankCode bankCode = codeBook.getBankCode( account, code, null, country );
            if ( bankCode == null )
            {
                if ( country == null )
                {
                    String key = "errors.validation.bankAccount.code";
                    throw ApiValidationException.prepare( key, code );
                }
                else
                {
                    String key = "errors.validation.bankAccount.code.country";
                    throw ApiValidationException.prepare( key, code, country );
                }
            }

            backend.setBankCode( bankCode.getCode() );

            Optional<String> countryValue = Optional.ofNullable( country );
            countryValue.ifPresent( backend::setCountry );

            if ( !countryValue.isPresent() )
            {
                backend.setCountry( bankCode.getCountry() );
            }
        }

        try
        {
            sValue = Optional.ofNullable( source.getCurrency() );
            sValue.ifPresent( backend::setCurrency );
        }
        catch ( IllegalArgumentException e )
        {
            throw ApiValidationException.prepare( "errors.validation.currency", source.getCurrency() );
        }

        sValue = Optional.ofNullable( source.getName() );
        sValue.ifPresent( backend::setName );

        sValue = Optional.ofNullable( source.getPrefix() );
        sValue.ifPresent( backend::setPrefix );

        sValue = Optional.ofNullable( source.getAccountNumber() );
        sValue.ifPresent( backend::setAccountNumber );

        sValue = Optional.ofNullable( source.getIban() );
        sValue.ifPresent( backend::setIban );

        sValue = Optional.ofNullable( source.getBic() );
        sValue.ifPresent( backend::setBic );

        Optional<Boolean> bValue = Optional.ofNullable( source.getPrimary() );
        bValue.ifPresent( backend::setPrimary );
    }
}
