package tech.tokorayane.accountservice.web.rest;

import tech.tokorayane.accountservice.domain.AccountStatus;
import tech.tokorayane.accountservice.dto.AccountResponse;
import tech.tokorayane.accountservice.dto.BalanceUpdateRequest;
import tech.tokorayane.accountservice.dto.CreateAccountRequest;
import tech.tokorayane.accountservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountRestController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> create(@RequestBody CreateAccountRequest request) {
        AccountResponse created = accountService.create(request);
        return ResponseEntity.created(URI.create("/api/accounts/" + created.id())).body(created);
    }

    @GetMapping
    public List<AccountResponse> list(@RequestParam(required = false) String customerReference) {
        if (customerReference != null) {
            return accountService.findByCustomerReference(customerReference);
        }
        return accountService.findAll();
    }

    @GetMapping("/{id}")
    public AccountResponse getById(@PathVariable UUID id) {
        return accountService.getById(id);
    }

    @GetMapping("/{id}/balance")
    public double getBalance(@PathVariable UUID id) {
        return accountService.getBalance(id);
    }

    @PatchMapping("/{id}/balance")
    public AccountResponse updateBalance(@PathVariable UUID id,
                                         @RequestBody BalanceUpdateRequest request) {
        return accountService.updateBalance(id, request.delta());
    }

    @PatchMapping("/{id}/status")
    public AccountResponse changeStatus(@PathVariable UUID id,
                                        @RequestParam AccountStatus status) {
        return accountService.changeStatus(id, status);
    }
}
