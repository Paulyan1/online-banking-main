package com.example.onlinebanking.model;

import jakarta.persistence.*;
import lombok.*; 
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;  

    @Column(name = "account_number", nullable = false, unique = true, length = 34)
    private String accountNumber;//Account number is a unique identifier for bank accounts, so we enforce uniqueness at the database level

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    @Builder.Default
    private AccountType accountType = AccountType.CHECKING; 

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false) 
    private LocalDateTime updatedAt;


    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "CAD";

    public enum AccountType {
        CHECKING,
        SAVINGS
    }

    public enum AccountStatus {
        ACTIVE,
        FROZEN,
        CLOSED
    }

    //if the account is active, it can be used for transactions. If it's frozen or closed, it cannot be used for transactions.
    public boolean isOperational() {
        return this.status == AccountStatus.ACTIVE;
    }

    // Check if the account has sufficient funds for a transaction
    public boolean hasSufficientFunds(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }


    // Deposit the account by a specified amount
    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    // Withdraw from the account by a specified amount
    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }

    


}
