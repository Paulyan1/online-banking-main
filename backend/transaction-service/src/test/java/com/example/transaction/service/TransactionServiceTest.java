package com.example.transaction.service;

import com.example.transaction.dto.AccountServiceDto;
import com.example.transaction.dto.TransactionDto.TransactionResponse;
import com.example.transaction.dto.TransactionDto.TransferRequest;
import com.example.transaction.model.Transaction;
import com.example.transaction.repository.TransactionRepository;
import com.example.transaction.service.TransactionService.InsufficientBalanceException;
import com.example.transaction.service.TransactionService.InvalidTransactionException;
import com.example.transaction.service.TransactionService.TransactionFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Transfer orchestration. The account-service client, transaction repository, and event
 * publisher are mocked, so these tests do not need PostgreSQL, RabbitMQ, SQS, or Keycloak.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final String SOURCE = "1000001";
    private static final String TARGET = "1000002";
    private static final BigDecimal AMOUNT = new BigDecimal("150.00");
    private static final String USER_ID = "user-1";

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private TransactionEventPublisher eventPublisher;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void transfer_shouldComplete_whenBalanceIsSufficient() {
        when(accountServiceClient.checkBalance(SOURCE, AMOUNT)).thenReturn(balance(true, "400.00"));
        when(accountServiceClient.getAccount(TARGET)).thenReturn(targetAccount());
        List<Transaction.TransactionStatus> savedStatuses = new ArrayList<>();
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            savedStatuses.add(saved.getStatus());
            saved.setId(99L);
            return saved;
        });

        TransactionResponse response = transactionService.transfer(transferRequest(SOURCE, TARGET), USER_ID);

        assertThat(savedStatuses).containsExactly(
                Transaction.TransactionStatus.PENDING,
                Transaction.TransactionStatus.COMPLETED);
        assertThat(response.getStatus()).isEqualTo(Transaction.TransactionStatus.COMPLETED);
        assertThat(response.getAmount()).isEqualByComparingTo(AMOUNT);
        assertThat(response.getCurrency()).isEqualTo("CAD");
        assertThat(response.getReference()).matches("TXN-\\d{8}-\\d{6}");

        ArgumentCaptor<Transaction> stored = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(stored.capture());
        Transaction completed = stored.getValue();
        assertThat(completed.getSourceAccount()).isEqualTo(SOURCE);
        assertThat(completed.getTargetAccount()).isEqualTo(TARGET);
        assertThat(completed.getSourceAccountId()).isEqualTo("10");
        assertThat(completed.getTargetAccountId()).isEqualTo("20");
        assertThat(completed.getInitiatedBy()).isEqualTo(USER_ID);
        assertThat(completed.getDescription()).isEqualTo("Invoice 42");
        assertThat(completed.getCompletedAt()).isNotNull();

        verify(accountServiceClient).updateBalance(SOURCE, AMOUNT.negate());
        verify(accountServiceClient).updateBalance(TARGET, AMOUNT);
        verify(eventPublisher).publishTransactionCompleted(completed);
    }

    @Test
    void transfer_shouldFail_whenInsufficientBalance() {
        when(accountServiceClient.checkBalance(SOURCE, AMOUNT)).thenReturn(balance(false, "10.00"));

        assertThatThrownBy(() -> transactionService.transfer(transferRequest(SOURCE, TARGET), USER_ID))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance")
                .hasMessageContaining(SOURCE);

        verify(accountServiceClient, never()).getAccount(any());
        verify(accountServiceClient, never()).updateBalance(any(), any());
        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void transfer_shouldFail_whenSourceAndTargetAreTheSame() {
        assertThatThrownBy(() -> transactionService.transfer(transferRequest(SOURCE, SOURCE), USER_ID))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("cannot be the same");

        verifyNoInteractions(accountServiceClient, transactionRepository, eventPublisher);
    }

    @Test
    void transfer_shouldFail_whenTargetAccountNotFound() {
        when(accountServiceClient.checkBalance(SOURCE, AMOUNT)).thenReturn(balance(true, "400.00"));
        when(accountServiceClient.getAccount(TARGET))
                .thenThrow(new AccountServiceClient.AccountServiceException("Failed to get account: 404 Not Found"));

        assertThatThrownBy(() -> transactionService.transfer(transferRequest(SOURCE, TARGET), USER_ID))
                .isInstanceOf(AccountServiceClient.AccountServiceException.class)
                .hasMessageContaining("Failed to get account");

        verify(accountServiceClient, never()).updateBalance(any(), any());
        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void transfer_shouldMarkTransactionFailed_whenBalanceUpdateFails() {
        when(accountServiceClient.checkBalance(SOURCE, AMOUNT)).thenReturn(balance(true, "400.00"));
        when(accountServiceClient.getAccount(TARGET)).thenReturn(targetAccount());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });
        doThrow(new AccountServiceClient.AccountServiceException("Failed to update balance: timeout"))
                .when(accountServiceClient).updateBalance(eq(SOURCE), eq(AMOUNT.negate()));

        assertThatThrownBy(() -> transactionService.transfer(transferRequest(SOURCE, TARGET), USER_ID))
                .isInstanceOf(TransactionFailedException.class)
                .hasMessageContaining("Transfer failed")
                .hasMessageContaining("Failed to update balance");

        ArgumentCaptor<Transaction> stored = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(stored.capture());
        Transaction failed = stored.getValue();
        assertThat(failed.getStatus()).isEqualTo(Transaction.TransactionStatus.FAILED);
        assertThat(failed.getFailureReason()).contains("Failed to update balance");
        assertThat(failed.getReference()).startsWith("TXN-");

        verify(accountServiceClient, never()).updateBalance(eq(TARGET), any());
        verify(eventPublisher, never()).publishTransactionCompleted(any());
    }

    private TransferRequest transferRequest(String source, String target) {
        return TransferRequest.builder()
                .sourceAccount(source)
                .targetAccount(target)
                .amount(AMOUNT)
                .currency("CAD")
                .description("Invoice 42")
                .build();
    }

    private AccountServiceDto.BalanceResponse balance(boolean sufficient, String available) {
        return AccountServiceDto.BalanceResponse.builder()
                .accountId(10L)
                .accountNumber(SOURCE)
                .balance(new BigDecimal(available))
                .currency("CAD")
                .sufficient(sufficient)
                .build();
    }

    private AccountServiceDto.AccountResponse targetAccount() {
        AccountServiceDto.AccountResponse response = new AccountServiceDto.AccountResponse();
        response.setId(20L);
        response.setAccountNumber(TARGET);
        response.setBalance(new BigDecimal("5.00"));
        response.setCurrency("CAD");
        response.setStatus("ACTIVE");
        return response;
    }
}
