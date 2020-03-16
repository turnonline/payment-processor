/*
 * Copyright (c) 2020 TurnOnline.biz s.r.o. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package biz.turnonline.ecosystem.payment.service;

import biz.turnonline.ecosystem.billing.model.IncomingInvoice;
import biz.turnonline.ecosystem.billing.model.InvoicePayment;
import biz.turnonline.ecosystem.payment.api.ApiValidationException;
import biz.turnonline.ecosystem.payment.api.model.Certificate;
import biz.turnonline.ecosystem.payment.service.model.BeneficiaryBankAccount;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import biz.turnonline.ecosystem.payment.service.model.CompanyBankAccount;
import biz.turnonline.ecosystem.payment.service.model.LocalAccount;
import biz.turnonline.ecosystem.payment.service.model.TransactionBill;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/**
 * Payment configuration and execution.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public interface PaymentConfig
{
    String TRUST_PAY_BANK_CODE = "9952";
    String REVOLUT_BANK_CODE = "REVO";
    String TRANSACTION_TOPIC = "transactions";

    /**
     * Enables API access to bank account.
     * <p>
     * Onboard all the bank accounts in to the service via bank API (access granted by certificate).
     * Once the task to grant access has been successfully finished, the customer's bank accounts will be available.
     * </p>
     *
     * @param owner       the authenticated account as an owner of the bank account
     * @param bank        bank identification, the bank to enable API access
     * @param certificate the certificate metadata, not a sensitive data.
     * @return the certificate with all metadata populated
     * @throws ApiValidationException if specified bank code is unsupported
     * @throws BankCodeNotFound       if specified bank code not found
     */
    Certificate enableApiAccess( @Nonnull LocalAccount owner,
                                 @Nonnull String bank,
                                 @Nonnull Certificate certificate );

    /**
     * Returns the bank account for given ID owned by the specified owner.
     *
     * @param id the bank account ID to be retrieved
     * @return the bank account
     * @throws BankAccountNotFound if bank account is not found
     */
    CompanyBankAccount getBankAccount( @Nonnull Long id );

    /**
     * Returns the bank account for given external ID.
     *
     * @param externalId the external ID of the bank account to be found
     * @return the bank account or {@code null} if not found
     */
    CompanyBankAccount getBankAccount( @Nonnull String externalId );

    /**
     * Returns the list of filtered bank accounts that's being owned by specified owner.
     *
     * @param offset   the position of the first account to retrieve
     * @param limit    the maximum number of accounts to retrieve
     * @param country  the country of the bank where bank account has been opened
     * @param bankCode the result should be limited to bank identified by this code
     * @return the list of filtered bank accounts
     */
    List<CompanyBankAccount> getBankAccounts( @Nullable Integer offset,
                                              @Nullable Integer limit,
                                              @Nullable String country,
                                              @Nullable String bankCode );

    /**
     * Returns the list of bank accounts that belongs to specified bank and owner.
     *
     * @param bank  the bank code to filter out bank accounts only for this bank
     * @return the list of filtered bank accounts
     */
    List<CompanyBankAccount> getBankAccounts( @Nonnull String bank );

    /**
     * Deletes the bank account specified by given ID. Only non primary bank account is being allowed to be deleted.
     *
     * @param id    the identification of the bank account to be deleted
     * @return the deleted bank account
     * @throws BankAccountNotFound    if bank account is not found
     * @throws ApiValidationException if specified bank account is being marked as primary
     */
    CompanyBankAccount deleteBankAccount( @Nonnull Long id );

    /**
     * Marks the specified bank account as primary credit bank account and rest will be de-marked.
     *
     * @param id    the identification of the bank account to be marked as primary credit account
     * @return the credit bank account that has been marked as primary
     * @throws BankAccountNotFound    if bank account is not found
     * @throws ApiValidationException if specified bank account can't be marked as primary
     */
    CompanyBankAccount markBankAccountAsPrimary( @Nonnull Long id );

    /**
     * Returns the credit bank account to be listed as the target account for a payment on an issued invoice.
     * There might be only max single or none primary credit bank account per country.
     * The primary credit bank account will be first searched with preferred actual country,
     * then if not found without applied country filter.
     *
     * @param country the optional country (either default one {@code null} or specified),
     *                for which the primary credit bank account is being searched for
     * @return the primary credit bank account
     * @throws BankAccountNotFound if primary credit bank account is not found
     */
    CompanyBankAccount getPrimaryBankAccount( @Nullable String country );

    /**
     * Returns the bank account to be used for company debit operations.
     * The invoice payment instruction can affect which bank account to use in order to optimize the transfer cost.
     *
     * @param payment the payment instruction for the debtor, taken from the incoming invoice
     * @return the debtor bank account or {@code null} if none specified
     */
    CompanyBankAccount getDebtorBankAccount( @Nonnull InvoicePayment payment );

    /**
     * Returns the list of all alternative bank accounts except the primary one.
     *
     * @param offset  the position of the first account to retrieve
     * @param limit   the maximum number of accounts to retrieve
     * @param locale  the the preferred locale in the result
     * @param country the preferred country in the result
     * @return the list of alternative bank accounts
     */
    List<CompanyBankAccount> getAlternativeBankAccounts( @Nullable Integer offset,
                                                         @Nullable Integer limit,
                                                         @Nullable Locale locale,
                                                         @Nullable String country );

    /**
     * Creates the beneficiary bank account associated with specified debtor.
     * The method is idempotent. If the call to the method will be repeated with the same input values,
     * no new record will be created, only the existing one will be returned.
     *
     * @param iban     the iban (can be formatted)
     * @param bic      International Bank Identifier Code (BIC/ISO 9362, also known as  SWIFT code)
     * @param currency the beneficiary bank account currency
     * @return the beneficiary bank account or {@code null} if not found, IBAN is malformed or otherwise fails validation
     * @throws IllegalArgumentException if the IBAN or BIC is malformed or otherwise fails validation.
     */
    BeneficiaryBankAccount insertBeneficiary( @Nonnull String iban,
                                              @Nonnull String bic,
                                              @Nonnull String currency );

    /**
     * Returns the beneficiary bank account for specified IBAN.
     *
     * @param iban  the iban (can be formatted)
     * @return the beneficiary bank account or {@code null} if not found
     * @throws IllegalArgumentException if the IBAN is malformed or otherwise fails validation.
     */
    BeneficiaryBankAccount getBeneficiary( @Nonnull String iban );

    /**
     * Checks whether the beneficiary bank account for specified IBAN already exists.
     *
     * @param iban  the iban (can be formatted)
     * @return true if bank account record already exist
     * @throws IllegalArgumentException if the IBAN is malformed or otherwise fails validation.
     */
    boolean isBeneficiary( @Nonnull String iban );

    /**
     * Creates a new empty transaction record that is associated with given invoice (order and invoice IDs).
     * <p>
     * Idempotent, if transaction record already exist, new won't be created only the existing one will be returned.
     *
     * @param invoice the invoice as a source of the transaction identification
     * @return the newly created transaction
     */
    CommonTransaction initGetTransactionDraft( @Nonnull IncomingInvoice invoice );

    /**
     * Creates a new record of the {@link TransactionBill} for the external Id.
     * To be idempotent, first searches for transaction with specified identification, if found it will be returned.
     *
     * @param extId the external identification of the transaction
     * @return the transaction
     * @see #searchTransaction(String)
     */
    CommonTransaction initGetTransaction( @Nonnull String extId );

    /**
     * Searches a transaction for specified external Id.
     *
     * @param extId the external identification of the transaction
     * @return the transaction or {@code null} if not found
     * @throws TransactionNotFound if transaction not found
     */
    CommonTransaction searchTransaction( @Nonnull String extId );
}
