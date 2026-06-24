package com.example.transaction.service;

import com.example.transaction.dto.AccountServiceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;

@Service
@Slf4j
public class AccountServiceClient {

    private final WebClient webClient;

    public AccountServiceClient(@Value("${example.transaction.services.account-service-url}") String accountServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(accountServiceUrl)
                .build();
    }

    public AccountServiceDto.BalanceResponse checkBalance(String account, BigDecimal amount) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/accounts/balance")
                            .queryParam("accountNumber", account)
                            .queryParam("amount", amount)
                            .build())
                    .retrieve()
                    .bodyToMono(AccountServiceDto.BalanceResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Error calling account-service checkBalance: {}", e.getMessage());
            throw new AccountServiceException("Failed to check balance: " + e.getMessage());
        }
    }

    public void updateBalance(String account, BigDecimal amount) {
        try {
            webClient.patch()
                    .uri("/api/accounts/balance")
                    .bodyValue(new AccountServiceDto.UpdateBalanceRequest(account, amount))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Error calling account-service updateBalance: {}", e.getMessage());
            throw new AccountServiceException("Failed to update balance: " + e.getMessage());
        }
    }

    public static class AccountServiceException extends RuntimeException {
        public AccountServiceException(String message) { super(message); }
    }

    public AccountServiceDto.AccountResponse getAccount(String accountNumber) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/accounts/by-number/{accountNumber}")
                            .build(accountNumber))
                    .retrieve()
                    .bodyToMono(AccountServiceDto.AccountResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Error calling account-service getAccount: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new AccountServiceException("Failed to get account: " + e.getMessage());
        }
    }
}
