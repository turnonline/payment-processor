package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.account.client.model.Account;
import biz.turnonline.ecosystem.payment.service.model.BankAccount;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public interface PaymentConfig
{
    String BANK_ACCOUNT_CODE_FORMAT = "BA_%d";

    /**
     * Returns the bank account for given ID owned by the specified owner.
     *
     * @param owner the authenticated account as an owner of the requested bank account
     * @param id    the bank account ID to be retrieved
     * @return the bank account
     * @throws WrongEntityOwner    if bank account is found but has a different owner as the authenticated account
     * @throws BankAccountNotFound if bank account is not found
     */
    BankAccount getBankAccount( @Nonnull Account owner, @Nonnull Long id );

    /**
     * Returns the list of all bank accounts that's being owned by specified owner.
     *
     * @param owner  the authenticated account as an owner of the bank accounts
     * @param offset the position of the first account to retrieve
     * @param limit  the maximum number of accounts to retrieve
     * @return the list of bank accounts
     */
    List<BankAccount> getBankAccounts( @Nonnull Account owner,
                                       @Nullable Integer offset,
                                       @Nullable Integer limit );

    /**
     * Inserts the specified bank account for given owner.
     * The value for {@link BankAccount#setCode(String)} will be autogenerated
     * with prefix {@link #BANK_ACCOUNT_CODE_FORMAT}.
     *
     * @param owner       the account as an owner of the newly added bank account
     * @param bankAccount the bank account to be inserted
     * @throws ApiValidationException if specified bank account is invalid
     */
    void insertBankAccount( @Nonnull Account owner, @Nonnull BankAccount bankAccount );

    /**
     * Updates the bank account with specified incoming changes.
     *
     * @param owner       the account as an owner of the bank account
     * @param bankAccount the bank account with incoming changes
     * @throws WrongEntityOwner if bank account is found but has a different owner as the authenticated account
     */
    void updateBankAccount( @Nonnull Account owner, @Nonnull BankAccount bankAccount );

    /**
     * Deletes the bank account specified by given ID. Only non primary bank account is being allowed to be deleted.
     *
     * @param owner the account as an owner of the bank account
     * @param id    the identification of the bank account to be deleted
     * @return the deleted bank account
     * @throws WrongEntityOwner       if bank account is found but has a different owner as the authenticated account
     * @throws BankAccountNotFound    if bank account is not found
     * @throws ApiValidationException if specified bank account is being marked as primary
     */
    BankAccount deleteBankAccount( @Nonnull Account owner, @Nonnull Long id );

    /**
     * Marks the specified bank account as primary and rest will be de-marked.
     *
     * @param owner the authenticated account as an owner of the requested bank account
     * @param id    the identification of the bank account to be marked as primary
     * @return the bank account that has been marked as primary
     * @throws WrongEntityOwner    if bank account is found but has a different owner as the authenticated account
     * @throws BankAccountNotFound if bank account is not found
     */
    BankAccount markBankAccountAsPrimary( @Nonnull Account owner, @Nonnull Long id );

    /**
     * Returns the primary bank account for specified owner,
     * or {@code null} if primary bank account does not exist.
     * <p>
     * Note: {@link BankAccount#TRUST_PAY_BANK_CODE} is not being considered as a primary bank account at all.
     *
     * @param owner the account as an owner of the bank account
     * @return the primary bank account
     */
    BankAccount getPrimaryBankAccount( @Nonnull Account owner );

    /**
     * Returns the primary bank account for specified owner and country,
     * or {@code null} if primary bank account does not exist.
     * <p>
     * Note:  {@link BankAccount#TRUST_PAY_BANK_CODE} is not being considered as a primary bank account at all.
     *
     * @param owner   the account as an owner of the bank account
     * @param country the target country
     * @return the primary bank account
     */
    BankAccount getPrimaryBankAccount( @Nonnull Account owner, @Nullable String country );

    /**
     * Returns the list of description of all alternative bank accounts except the primary one.
     *
     * @param owner   the account as an owner of the bank account
     * @param exclude the primary bank account to exclude from the list
     * @return the list of alternative bank accounts
     * @throws WrongEntityOwner if bank account is found but has a different owner as the authenticated account
     */
    List<BankAccount.Description> getAlternativeBankAccounts( @Nonnull Account owner,
                                                              @Nullable BankAccount exclude );
}
