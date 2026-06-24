package com.example.onlinebanking.controller;

import com.example.onlinebanking.dto.AccountDto.*;
import com.example.onlinebanking.dto.UpdateBalanceRequest;
import com.example.onlinebanking.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;



@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {
    
    private final AccountService accountService;

    @GetMapping("/me")
    public ResponseEntity<List<AccountSummary>> getMyAccounts(
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(accountService.getMyAccounts(getSubject(jwt)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(accountService.getAccountById(id, getSubject(jwt)));
    }

    @GetMapping("/by-number/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
    }

    
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal Jwt jwt) {
            log.info("Creating account request: {}", request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(request, getSubject(jwt)));
    }

    // Endpoint to check if the account has sufficient balance for a transaction
    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> checkBalance(
            @RequestParam String accountNumber,
            @RequestParam(defaultValue = "0") BigDecimal amount) {

        return ResponseEntity.ok(accountService.checkBalance(accountNumber, amount));
    }

    // Endpoint to update the account balance (e.g., for deposits or withdrawals)
    @PatchMapping("/balance")
    public ResponseEntity<Void> updateBalance(@RequestBody UpdateBalanceRequest request) {
        accountService.updateBalance(request.getAccountNumber(), request.getAmount());
        return ResponseEntity.noContent().build();
    }

    // Helper method to extract user ID from JWT
    private String getSubject(Jwt jwt) {
        return jwt != null ? jwt.getSubject() : "local-test-user";
    }
}
