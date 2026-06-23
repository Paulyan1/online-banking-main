package com.example.onlinebanking.service;

import com.example.onlinebanking.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

/*
* Service to generate unique account numbers.
*
* For simplicity, we generate a random 7-digit account number.
* In production, more complex logic could be implemented
* that includes institution and branch codes, and a check digit algorithm.
*/
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountNumberGeneratorService {

    private static final String INSTITUTION_NUMBER = "999";
    private static final String TRANSIT_NUMBER = "00001";

    private final AccountRepository accountRepository;
    private final Random random = new Random();
    // Generate a unique account number by checking against the database
    public String generateUniqueAccountNumber() {
        String accountNumber;
        int attempts = 0;

        do {
            accountNumber = generateAccountNumber();
            attempts++;
            if (attempts > 10) {
                throw new RuntimeException("Unable to generate unique account number after 10 attempts");
            }
        } while (accountRepository.existsByAccountNumber(accountNumber));

        log.debug("Generated unique account number: {}", accountNumber);
        return accountNumber;
    }

    private String generateAccountNumber() {
        int number = 1000000 + random.nextInt(9000000);
        return String.valueOf(number);
    }

    public String getInstitutionNumber() {
        return INSTITUTION_NUMBER;
    }

    public String getTransitNumber() {
        return TRANSIT_NUMBER;
    }
}