package tech.tokorayane.accountservice.service;

import tech.tokorayane.accountservice.domain.Account;
import tech.tokorayane.accountservice.domain.AccountStatus;
import tech.tokorayane.accountservice.dto.AccountResponse;
import tech.tokorayane.accountservice.dto.CreateAccountRequest;
import tech.tokorayane.accountservice.exception.AccountBlockedException;
import tech.tokorayane.accountservice.exception.AccountNotFoundException;
import tech.tokorayane.accountservice.exception.InsufficientFundsException;
import tech.tokorayane.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        Account account = new Account(
                request.customerReference(),
                request.currency().toUpperCase(),
                request.initialBalance()
        );
        return AccountResponse.from(accountRepository.save(account));
    }

    public AccountResponse getById(UUID id) {
        return AccountResponse.from(findOrThrow(id));
    }

    public double getBalance(UUID id) {
        return findOrThrow(id).getBalance();
    }

    public List<AccountResponse> findAll() {
        return accountRepository.findAll().stream()
                .map(AccountResponse::from)
                .toList();
    }

    public List<AccountResponse> findByCustomerReference(String customerReference) {
        return accountRepository.findByCustomerReference(customerReference).stream()
                .map(AccountResponse::from)
                .toList();
    }

    /**
     * delta > 0 : crédit, delta < 0 : débit.
     */
    @Transactional
    public AccountResponse updateBalance(UUID id, double delta) {
        Account account = findOrThrow(id);

        if (!account.isActive()) {
            throw new AccountBlockedException(id);
        }

        double newBalance = account.getBalance() + delta;
        if (newBalance < 0) {
            throw new InsufficientFundsException(id);
        }

        account.setBalance(newBalance);
        return AccountResponse.from(account);
    }

    @Transactional
    public AccountResponse changeStatus(UUID id, AccountStatus status) {
        Account account = findOrThrow(id);
        account.setStatus(status);
        return AccountResponse.from(account);
    }

    private Account findOrThrow(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}