package com.example.onlinebanking.service;

import com.example.onlinebanking.dto.AccountDto.*;
import com.example.onlinebanking.model.Account;
import com.example.onlinebanking.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Service Layer — contains all the business logic.
 * FUNDAMENTAL RULES:
 * - The Controller does not contain logic → it delegates to the Service.
 * - The Service knows nothing about HTTP → it works with pure Java objects.
 * - The Repository does not contain logic → it only accesses the DB.
 * @Transactional: ensures that DB operations are atomic. If something goes wrong halfway, everything is rolled back.
 * @Transactional(readOnly = true) at the class level means all methods are read-only by default (optimization). Methods that modify data must be annotated with @Transactional without readOnly.
 * Lombok annotations:
 * - @RequiredArgsConstructor: generates a constructor with all final fields (used for dependency injection
 */

@Service
@RequiredArgsConstructor //Lombok: generate constructor with all final fields (injection)
@Slf4j//Lombok: generate logger → log.info(), log.error() etc.
@Transactional(readOnly = true) //Default: all operations are read-only (optimization)
public class AccountService {
    private final AccountRepository accountRepository; //Injected by Spring
    private final AccountNumberGeneratorService accountNumberGeneratorService; //Injected by Spring

    /*
        * Retrieves all accounts of the authenticated user.
        * userId comes from the JWT — never from the request body!
    */
    public List<AccountSummary> getMyAccounts(String userId) {
        log.debug("Fetching accounts for user: {}", userId);
        return accountRepository.findByUserId(userId)
                .stream()   //for each account, convert it to AccountSummary DTO
                .map(AccountSummary::from)  // AccountSummary::from is a static method that converts Account to AccountSummary
                .toList();  //Java 16+ — more concise than collect(Collectors.toList())
    }
    /// Retrieves a specific account by ID, verifying that it belongs to the user.
    public AccountResponse getAccountById(Long accountId, String userId) {
        Account account = findAccountByIdAndUserId(accountId, userId); //Helper method to fetch account and check ownership
        return AccountResponse.from(account); //Convert to DTO for response
    }

    public AccountResponse getAccountByNumber(String accountNumber) {
    Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));

    return AccountResponse.from(account);
}

    /*
        * Creates a new bank account.
        * @Transactional without readOnly — this operation writes to the DB.
    */
    @Transactional //This method modifies the database, so we need @Transactional without readOnly
    public AccountResponse createAccount(CreateAccountRequest request, String userId) {
        log.info("Creating new {} account for user: {}", request.getAccountType(), userId);

        String accountNumber = accountNumberGeneratorService.generateUniqueAccountNumber(); //Generate unique account number

        Account account = Account.builder()
                .userId(userId)
                .accountNumber(accountNumber)
                .ownerName(request.getOwnerName())
                .balance(request.getInitialDeposit())
                .currency(request.getCurrency())
                .accountType(request.getAccountType())
                .status(Account.AccountStatus.ACTIVE)
                .build();


        Account saved = accountRepository.save(Objects.requireNonNull(account, "Account cannot be null")); //Save to DB and get the saved entity with ID
        log.info("Account created with ID: {}", saved.getId());

        return AccountResponse.from(saved);
    }

    /*
        * Checks the balance of a specific account.
        * @param accountNumber the account number to check
        * @param requestedAmount the amount to check against
        * @return the balance response
    */
    public BalanceResponse checkBalance(String accountNumber, BigDecimal requestedAmount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found with account number: " + accountNumber));

        boolean sufficient = account.getBalance().compareTo(requestedAmount) >= 0;

        return BalanceResponse.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .sufficient(sufficient)
                .build();
    }

    @Transactional
    public void updateBalance(String accountNumber, BigDecimal newBalance) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found with account number: " + accountNumber));

        if (!account.isOperational()) {
            throw new RuntimeException("Account is not operational: " + accountNumber);
        }

        if (newBalance.compareTo(BigDecimal.ZERO) >= 0) {
            account.credit(newBalance);
        } else {
            BigDecimal debitAmount = newBalance.abs();
            if (!account.hasSufficientFunds(debitAmount)) {
                throw new RuntimeException("Insufficient funds for account: " + accountNumber);
            }
            account.debit(debitAmount);
        }

        accountRepository.save(account); //Save the updated account
        log.info("Updated balance for account {}: new balance is {}", accountNumber, account.getBalance());
    }


    // Helper method to find account by ID and verify ownership
    private Account findAccountByIdAndUserId(Long accountId, String userId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + accountId + " for user: " + userId));
    }

    // Custom exception for account not found
    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(String message) {
            super(message);
        }
    }

    public static class AccountNotOperationalException extends RuntimeException {
        public AccountNotOperationalException(String message) {
            super(message);
        }
    }

    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(String message) {
            super(message);
        }
    }

}


