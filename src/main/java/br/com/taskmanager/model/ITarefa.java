package br.com.taskmanager.model;

import java.time.LocalDateTime;

/**
 * Contrato comum a todas as tarefas (mantido da versao console,
 * agora com id numerico vindo do banco e possibilidade de reabrir).
 */
public interface ITarefa {
    Integer getId();

    String getTitulo();

    String getDescricao();

    boolean isConcluida();

    void concluir();

    void reabrir();

    LocalDateTime getDataCriacao();

    String resumo();
}
