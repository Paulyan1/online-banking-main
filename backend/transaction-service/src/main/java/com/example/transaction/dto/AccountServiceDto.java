package com.example.transaction.dto;

import lombok.*;

import java.math.BigDecimal;


public class AccountServiceDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BalanceResponse {
        private Long accountId;
        private String accountNumber;
        private BigDecimal balance;
        private String currency;
        private boolean sufficient;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateBalanceRequest {
        private String accountNumber;
        private BigDecimal amount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountResponse {
        private Long id;
        private String accountNumber;
        private BigDecimal balance;
        private String currency;
        private String status;
    }
}