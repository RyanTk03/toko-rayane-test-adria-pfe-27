package tech.tokorayane.accountservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import tech.tokorayane.accountservice.domain.Account;
import tech.tokorayane.accountservice.domain.AccountStatus;
import tech.tokorayane.accountservice.repository.AccountRepository;

@SpringBootApplication
@Slf4j
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(AccountRepository accountRepository) {
        return args -> {
            Account a1 = accountRepository.save(new Account("CUST-001", "MAD", 10_000));
            Account a2 = accountRepository.save(new Account("CUST-001", "EUR", 2_500));
            Account a3 = accountRepository.save(new Account("CUST-002", "MAD", 500));
            Account a4 = accountRepository.save(new Account("CUST-003", "USD", 8_000));

            Account blocked = new Account("CUST-004", "MAD", 1_200);
            blocked.setStatus(AccountStatus.BLOCKED);
            blocked = accountRepository.save(blocked);

            accountRepository.findAll().forEach(a ->
                    log.info("Compte {} | {} | {} | {} {}",
                            a.getId(), a.getCustomerReference(), a.getStatus(), a.getBalance(), a.getCurrency()));
        };
    }
}
