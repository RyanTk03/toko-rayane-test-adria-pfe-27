package tech.tokorayane.accountservice.exception;

import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(UUID id) {
        super("Solde insuffisant pour le compte : " + id);
    }
}