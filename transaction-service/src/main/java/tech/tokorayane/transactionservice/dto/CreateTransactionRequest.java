package tech.tokorayane.transactionservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionRequest(
        @NotNull UUID accountId,
        @NotNull BigDecimal amount,
        @Size(min = 3, max = 3) String currency,
        String merchantCategory,
        String country
) {
}
