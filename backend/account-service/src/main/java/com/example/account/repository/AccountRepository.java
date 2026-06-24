package com.example.onlinebanking.repository;


import com.example.onlinebanking.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;  

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    @Query("SELECT a FROM Account a WHERE a.userId = :userId")
    List<Account> findByUserId(@Param("userId") String userId);

    Optional<Account> findByAccountNumber(String accountNumber);//Spring Data JPA can generate this query automatically based on the method name

    boolean existsByAccountNumber(String accountNumber);//Check if an account with the given account number already exists (used for validation)

    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.userId = :userId")
    Optional<Account> findByIdAndUserId(@Param("id") Long id, @Param("userId") String userId);



    @Query("SELECT a FROM Account a WHERE a.userId = :userId AND a.status = 'ACTIVE'")
    List<Account> findActiveAccountsByUserId(@Param("userId") String userId);

}
