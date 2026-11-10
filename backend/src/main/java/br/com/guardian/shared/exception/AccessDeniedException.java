package br.com.guardian.shared.exception;

public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException() {
        super("Acesso negado. Você não possui permissão para realizar esta operação.");
    }
}
