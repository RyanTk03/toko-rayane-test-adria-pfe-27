package tech.tokorayane.accountservice.exception;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(UUID id) {
        super("Compte introuvable : " + id);
    }
}
 