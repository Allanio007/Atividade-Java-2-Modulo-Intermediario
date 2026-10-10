package br.com.taskmanager.exceptions;

/**
 * Falha de acesso ao banco. Envolve a SQLException (checada) para que as
 * camadas de servico e de tela nao dependam de java.sql.
 */
public class PersistenciaException extends RuntimeException {
    public PersistenciaException(String message, Throwable cause) {
        super(message, cause);
    }
}
