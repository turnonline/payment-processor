package biz.turnonline.ecosystem.payment.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;


/**
 * The bank account resource.
 **/

@ApiModel( description = "The bank account resource." )
@javax.annotation.Generated( value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2018-08-26T04:47:00.033Z" )
public class BankAccount
{

    private Long id = null;

    private String name = null;

    private String prefix = null;

    private String accountNumber = null;

    private String formatted = null;

    private BankAccountBank bank = null;

    private String iban = null;

    private String bic = null;

    private Boolean primary = false;

    /**
     * The bank account identification.
     **/
    public BankAccount id( Long id )
    {
        this.id = id;
        return this;
    }


    @ApiModelProperty( value = "The bank account identification." )
    @JsonProperty( "id" )
    public Long getId()
    {
        return id;
    }

    public void setId( Long id )
    {
        this.id = id;
    }

    /**
     * The user defined name of the bank account.
     **/
    public BankAccount name( String name )
    {
        this.name = name;
        return this;
    }


    @ApiModelProperty( value = "The user defined name of the bank account." )
    @JsonProperty( "name" )
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
     **/
    public BankAccount prefix( String prefix )
    {
        this.prefix = prefix;
        return this;
    }


    @ApiModelProperty( value = "The optional bank account number prefix." )
    @JsonProperty( "prefix" )
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
     **/
    public BankAccount accountNumber( String accountNumber )
    {
        this.accountNumber = accountNumber;
        return this;
    }


    @ApiModelProperty( value = "The bank account number." )
    @JsonProperty( "accountNumber" )
    public String getAccountNumber()
    {
        return accountNumber;
    }

    public void setAccountNumber( String accountNumber )
    {
        this.accountNumber = accountNumber;
    }

    /**
     * The formatted full bank account number.
     **/
    public BankAccount formatted( String formatted )
    {
        this.formatted = formatted;
        return this;
    }


    @ApiModelProperty( value = "The formatted full bank account number." )
    @JsonProperty( "formatted" )
    public String getFormatted()
    {
        return formatted;
    }

    public void setFormatted( String formatted )
    {
        this.formatted = formatted;
    }

    /**
     * The bank account bank defined as a codebook.
     **/
    public BankAccount bank( BankAccountBank bank )
    {
        this.bank = bank;
        return this;
    }


    @ApiModelProperty( required = true, value = "The bank account bank defined as a codebook." )
    @JsonProperty( "bank" )
    public BankAccountBank getBank()
    {
        return bank;
    }

    public void setBank( BankAccountBank bank )
    {
        this.bank = bank;
    }

    /**
     * The international bank account number.
     **/
    public BankAccount iban( String iban )
    {
        this.iban = iban;
        return this;
    }


    @ApiModelProperty( value = "The international bank account number." )
    @JsonProperty( "iban" )
    public String getIban()
    {
        return iban;
    }

    public void setIban( String iban )
    {
        this.iban = iban;
    }

    /**
     * The international Bank Identifier Code (BIC/ISO 9362, a normalized code - also known as Business Identifier Code, Bank International Code and SWIFT code).
     **/
    public BankAccount bic( String bic )
    {
        this.bic = bic;
        return this;
    }


    @ApiModelProperty( value = "The international Bank Identifier Code (BIC/ISO 9362, a normalized code - also known as Business Identifier Code, Bank International Code and SWIFT code)." )
    @JsonProperty( "bic" )
    public String getBic()
    {
        return bic;
    }

    public void setBic( String bic )
    {
        this.bic = bic;
    }

    /**
     * Boolean identification, whether this bank account is being marked by the user as a primary account. If yes, this bank account will be used as a default account unless specified another one. There might be only max single or none primary bank account per country.
     **/
    public BankAccount primary( Boolean primary )
    {
        this.primary = primary;
        return this;
    }


    @ApiModelProperty( value = "Boolean identification, whether this bank account is being marked by the user as a primary account. If yes, this bank account will be used as a default account unless specified another one. There might be only max single or none primary bank account per country." )
    @JsonProperty( "primary" )
    public Boolean getPrimary()
    {
        return primary;
    }

    public void setPrimary( Boolean primary )
    {
        this.primary = primary;
    }


    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }
        BankAccount bankAccount = ( BankAccount ) o;
        return Objects.equals( id, bankAccount.id ) &&
                Objects.equals( name, bankAccount.name ) &&
                Objects.equals( prefix, bankAccount.prefix ) &&
                Objects.equals( accountNumber, bankAccount.accountNumber ) &&
                Objects.equals( formatted, bankAccount.formatted ) &&
                Objects.equals( bank, bankAccount.bank ) &&
                Objects.equals( iban, bankAccount.iban ) &&
                Objects.equals( bic, bankAccount.bic ) &&
                Objects.equals( primary, bankAccount.primary );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( id, name, prefix, accountNumber, formatted, bank, iban, bic, primary );
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "class BankAccount {\n" );

        sb.append( "    id: " ).append( toIndentedString( id ) ).append( "\n" );
        sb.append( "    name: " ).append( toIndentedString( name ) ).append( "\n" );
        sb.append( "    prefix: " ).append( toIndentedString( prefix ) ).append( "\n" );
        sb.append( "    accountNumber: " ).append( toIndentedString( accountNumber ) ).append( "\n" );
        sb.append( "    formatted: " ).append( toIndentedString( formatted ) ).append( "\n" );
        sb.append( "    bank: " ).append( toIndentedString( bank ) ).append( "\n" );
        sb.append( "    iban: " ).append( toIndentedString( iban ) ).append( "\n" );
        sb.append( "    bic: " ).append( toIndentedString( bic ) ).append( "\n" );
        sb.append( "    primary: " ).append( toIndentedString( primary ) ).append( "\n" );
        sb.append( "}" );
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString( Object o )
    {
        if ( o == null )
        {
            return "null";
        }
        return o.toString().replace( "\n", "\n    " );
    }
}
