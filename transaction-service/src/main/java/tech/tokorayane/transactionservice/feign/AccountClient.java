package tech.tokorayane.transactionservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import tech.tokorayane.transactionservice.dto.BalanceUpdateRequest;
import tech.tokorayane.transactionservice.model.Account;

import java.util.UUID;

@FeignClient(name = "account-service", url = "${account-service.url}")
public interface AccountClient {

    @GetMapping("/api/accounts/{id}")
    Account getAccount(@PathVariable("id") UUID id);

    @PatchMapping("/api/accounts/{id}/balance")
    Account updateBalance(@PathVariable("id") UUID id, @RequestBody BalanceUpdateRequest request);
}
