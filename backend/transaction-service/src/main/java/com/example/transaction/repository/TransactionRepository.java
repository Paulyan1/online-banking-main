package com.example.transaction.repository;

import com.example.transaction.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    @Query("SELECT t FROM Transaction t WHERE t.initiatedBy = :userId ORDER BY t.createdAt DESC")
    Page<Transaction> findByInitiatedBy(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.sourceAccountId = :account OR t.targetAccountId = :account ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccount(@Param("account") String account, Pageable pageable);

    Page<Transaction> findBySourceAccountOrTargetAccount(
        String sourceAccount,
        String targetAccount,
        Pageable pageable
    );

    boolean existsByReference(String reference);
}
