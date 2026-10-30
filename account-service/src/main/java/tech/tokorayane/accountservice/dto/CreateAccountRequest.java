package tech.tokorayane.accountservice.dto;

public record CreateAccountRequest(
        String customerReference,
        String currency,
        double initialBalance
) {
}