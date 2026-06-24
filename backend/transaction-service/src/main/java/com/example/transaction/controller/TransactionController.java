package com.example.transaction.controller;

import com.example.transaction.dto.TransactionDto.*;
import com.example.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transfer(request, getSubject(jwt)));
    }

    @GetMapping("/history")
    public ResponseEntity<PagedTransactionResponse> getMyTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(
                transactionService.getMyTransactions(getSubject(jwt), page, size));
    }

    @GetMapping("/history/{account}")
    public ResponseEntity<PagedTransactionResponse> getTransactionByAccount(
            @PathVariable String account,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                transactionService.getTransactionByAccount(account, page, size));
    }

    // Helper method to extract user ID from JWT
    private String getSubject(Jwt jwt) {
        return jwt != null ? jwt.getSubject() : "local-test-user";
    }
}
