package tech.tokorayane.accountservice.dto;

import tech.tokorayane.accountservice.domain.Account;
import tech.tokorayane.accountservice.domain.AccountStatus;

import java.util.UUID;

public record AccountResponse(
        UUID id,
        String customerReference,
        AccountStatus status,
        String currency,
        double balance
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCustomerReference(),
                account.getStatus(),
                account.getCurrency(),
                account.getBalance()
        );
    }
}
