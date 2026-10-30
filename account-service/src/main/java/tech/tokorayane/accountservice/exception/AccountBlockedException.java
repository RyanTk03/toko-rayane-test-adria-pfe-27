package tech.tokorayane.accountservice.exception;

import java.util.UUID;

public class AccountBlockedException extends RuntimeException {

    public AccountBlockedException(UUID id) {
        super("Le compte est bloqué : " + id);
    }
}