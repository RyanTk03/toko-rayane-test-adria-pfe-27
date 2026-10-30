package tech.tokorayane.transactionservice.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.tokorayane.transactionservice.config.TransactionProperties;
import tech.tokorayane.transactionservice.domain.Transaction;
import tech.tokorayane.transactionservice.dto.BalanceUpdateRequest;
import tech.tokorayane.transactionservice.dto.CreateTransactionRequest;
import tech.tokorayane.transactionservice.dto.TransactionResponse;
import tech.tokorayane.transactionservice.exception.AccountNotFoundException;
import tech.tokorayane.transactionservice.exception.InvalidTransactionException;
import tech.tokorayane.transactionservice.exception.TransactionNotFoundException;
import tech.tokorayane.transactionservice.feign.AccountClient;
import tech.tokorayane.transactionservice.model.Account;
import tech.tokorayane.transactionservice.repository.TransactionRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final TransactionProperties properties;

    @Transactional
    public Transaction saveTransaction(CreateTransactionRequest transactionRequest) {
        BigDecimal amount = transactionRequest.amount();

        if (amount.signum() <= 0) {
            throw new InvalidTransactionException("Le montant doit être strictement positif");
        }
        if (amount.compareTo(properties.getMaxAmount()) > 0) {
            throw new InvalidTransactionException("Le montant dépasse le plafond autorisé : " + properties.getMaxAmount());
        }

        Account account = fetchAccount(transactionRequest.accountId());
        if (!"ACTIVE".equals(account.status())) {
            throw new InvalidTransactionException("Le compte n'est pas actif : " + account.id());
        }

        if (!account.currency().equalsIgnoreCase(transactionRequest.currency())) {
            throw new InvalidTransactionException("Devise incorrecte : le compte est en " + account.currency());
        }

        if (account.balance() < amount.doubleValue()) {
            throw new InvalidTransactionException("Solde insuffisant : " + account.balance() + " " + account.currency());
        }

        Transaction transaction = transactionRepository.saveAndFlush(new Transaction(
                transactionRequest.accountId(),
                amount,
                transactionRequest.currency().toUpperCase(),
                transactionRequest.merchantCategory(),
                transactionRequest.country()
        ));
        this.accountClient.updateBalance(transactionRequest.accountId(), new BalanceUpdateRequest(-amount.doubleValue()));

        return transaction;
    }

    public List<TransactionResponse> viewTransactions() {
        return transactionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    public TransactionResponse viewOneTransaction(UUID id) {
        return transactionRepository.findById(id)
                .map(TransactionResponse::from)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    private Account fetchAccount(UUID accountId) {
        try {
            return accountClient.getAccount(accountId);
        } catch (FeignException.NotFound e) {
            throw new AccountNotFoundException(accountId);
        }
    }
}