package com.example.onlinebanking.service;

import com.example.onlinebanking.dto.AccountDto.BalanceResponse;
import com.example.onlinebanking.model.Account;
import com.example.onlinebanking.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Balance rules owned by account-service. Repository and event collaborators are mocked,
 * so these tests do not need PostgreSQL, RabbitMQ, or Keycloak.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final String ACCOUNT_NUMBER = "1000001";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountNumberGeneratorService accountNumberGeneratorService;

    @Mock
    private AccountEventPublisher accountEventPublisher;

    @InjectMocks
    private AccountService accountService;

    @Test
    void checkBalance_shouldReportSufficient_whenBalanceEqualsRequestedAmount() {
        Account account = accountWithBalance("80.00", Account.AccountStatus.ACTIVE);
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));

        BalanceResponse response = accountService.checkBalance(ACCOUNT_NUMBER, new BigDecimal("80.00"));

        assertThat(response.isSufficient()).isTrue();
        assertThat(response.getAccountId()).isEqualTo(11L);
        assertThat(response.getAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
        assertThat(response.getBalance()).isEqualByComparingTo("80.00");
        assertThat(response.getCurrency()).isEqualTo("CAD");
    }

    @Test
    void checkBalance_shouldReportInsufficient_whenRequestedAmountExceedsBalance() {
        Account account = accountWithBalance("80.00", Account.AccountStatus.ACTIVE);
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));

        BalanceResponse response = accountService.checkBalance(ACCOUNT_NUMBER, new BigDecimal("80.01"));

        assertThat(response.isSufficient()).isFalse();
        assertThat(response.getBalance()).isEqualByComparingTo("80.00");
    }

    @Test
    void checkBalance_shouldFail_whenAccountNotFound() {
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.checkBalance(ACCOUNT_NUMBER, new BigDecimal("10.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account not found")
                .hasMessageContaining(ACCOUNT_NUMBER);
    }

    @Test
    void updateBalance_shouldDebit_whenFundsAreSufficient() {
        Account account = accountWithBalance("100.00", Account.AccountStatus.ACTIVE);
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));

        accountService.updateBalance(ACCOUNT_NUMBER, new BigDecimal("-40.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("60.00");
        verify(accountRepository).save(account);
    }

    @Test
    void updateBalance_shouldFail_whenInsufficientFunds() {
        Account account = accountWithBalance("25.00", Account.AccountStatus.ACTIVE);
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.updateBalance(ACCOUNT_NUMBER, new BigDecimal("-40.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient funds")
                .hasMessageContaining(ACCOUNT_NUMBER);

        assertThat(account.getBalance()).isEqualByComparingTo("25.00");
        verify(accountRepository, never()).save(account);
    }

    @Test
    void updateBalance_shouldFail_whenAccountIsNotOperational() {
        Account account = accountWithBalance("500.00", Account.AccountStatus.FROZEN);
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.updateBalance(ACCOUNT_NUMBER, new BigDecimal("-10.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not operational")
                .hasMessageContaining(ACCOUNT_NUMBER);

        assertThat(account.getBalance()).isEqualByComparingTo("500.00");
        verify(accountRepository, never()).save(account);
    }

    private Account accountWithBalance(String balance, Account.AccountStatus status) {
        return Account.builder()
                .id(11L)
                .userId("user-1")
                .accountNumber(ACCOUNT_NUMBER)
                .ownerName("Paul")
                .balance(new BigDecimal(balance))
                .currency("CAD")
                .accountType(Account.AccountType.CHECKING)
                .status(status)
                .build();
    }
}
