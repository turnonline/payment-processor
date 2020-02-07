package biz.turnonline.ecosystem.payment.service.model;

import biz.turnonline.ecosystem.payment.service.CodeBook;
import com.googlecode.objectify.annotation.Subclass;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Beneficiary bank account as a counterparty.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Subclass( name = "Beneficiary", index = true )
public class BeneficiaryBankAccount
        extends BankAccount
{
    private static final long serialVersionUID = 4462909552919142290L;

    @Inject
    public BeneficiaryBankAccount( CodeBook codeBook )
    {
        super( codeBook );
    }

    @Override
    public String getExternalId( @Nonnull String code )
    {
        return super.getExternalId( code );
    }

    @Override
    public void setExternalId( @Nonnull String code, @Nullable String externalId )
    {
        super.setExternalId( code, externalId );
    }

    @Override
    public String getKind()
    {
        return "BeneficiaryBankAccount";
    }
}
