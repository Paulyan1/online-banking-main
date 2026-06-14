package com.example.onlinebanking.dto;

import com.example.onlinebanking.model.Account;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public class AccountDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateAccountRequest {

        @NotBlank(message = "Owner name is required")
        @Size(min = 2, max = 255, message = "Owner name must be between 2 and 255 characters")
        private String ownerName;

        @NotNull(message = "Account type is required")
        private Account.AccountType accountType;

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code (e.g. EUR, USD)")
        @Builder.Default
        private String currency = "CAD";

        @DecimalMin(value = "0.00", message = "Initial deposit cannot be negative")
        @Builder.Default
        private BigDecimal initialDeposit = BigDecimal.ZERO;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AccountResponse {
        private Long id;
        private String accountNumber;
        private String ownerName;
        private BigDecimal balance;
        private String currency;
        private Account.AccountType accountType;
        private Account.AccountStatus status;
        private LocalDateTime createdAt;

        public static AccountResponse from(Account account) {
            return AccountResponse.builder()
                    .id(account.getId())
                    .accountNumber(account.getAccountNumber())
                    .ownerName(account.getOwnerName())
                    .balance(account.getBalance())
                    .currency(account.getCurrency())
                    .accountType(account.getAccountType())
                    .status(account.getStatus())
                    .createdAt(account.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AccountSummary {
        private Long id;
        private String accountNumber;
        private String ownerName;
        private BigDecimal balance;
        private String currency;
        private Account.AccountType status;

        public static AccountSummary from(Account account) {
            return AccountSummary.builder()
                    .id(account.getId())
                    .accountNumber(account.getAccountNumber())
                    .ownerName(account.getOwnerName())
                    .balance(account.getBalance())
                    .currency(account.getCurrency())
                    .status(account.getAccountType())
                    .build();
        }
    }

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
        private boolean sufficient; // true if balance >= requestedAmount, false otherwise
    }


}
