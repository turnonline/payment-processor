package biz.turnonline.ecosystem.payment.service.model;

import biz.turnonline.ecosystem.payment.service.CodeBook;
import com.googlecode.objectify.annotation.Subclass;

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
    private static final long serialVersionUID = -6270447065316354199L;

    @Inject
    public BeneficiaryBankAccount( CodeBook codeBook )
    {
        super( codeBook );
    }

    @Override
    public String getKind()
    {
        return "BeneficiaryBankAccount";
    }
}
