package br.com.taskmanager.exceptions;

/** Dado informado pelo usuario e invalido (mantida da versao console). */
public class EntradaInvalidaException extends RuntimeException {
    public EntradaInvalidaException(String message) {
        super(message);
    }
}
