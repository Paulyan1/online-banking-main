package com.example.onlinebanking.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBalanceRequest {
    private String accountNumber;
    private BigDecimal amount;
}