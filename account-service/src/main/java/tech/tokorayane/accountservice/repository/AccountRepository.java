package tech.tokorayane.accountservice.repository;

import tech.tokorayane.accountservice.domain.Account;
import tech.tokorayane.accountservice.domain.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByCustomerReference(String customerReference);

    List<Account> findByStatus(AccountStatus status);
}