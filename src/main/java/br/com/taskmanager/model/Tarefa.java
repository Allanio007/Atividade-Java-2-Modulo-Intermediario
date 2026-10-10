package br.com.taskmanager.model;

import java.time.LocalDateTime;

/** Tarefa comum. */
public class Tarefa extends BaseTarefa {

    public Tarefa(String titulo, String descricao) {
        super(titulo, descricao);
    }

    public Tarefa(Integer id, String titulo, String descricao,
                  boolean concluida, LocalDateTime dataCriacao) {
        super(id, titulo, descricao, concluida, dataCriacao);
    }

    /** Tarefas comuns nao tem prioridade; a tabela mostra vazio. */
    public Integer getPrioridade() {
        return null;
    }

    public String getTipo() {
        return "Normal";
    }

    @Override
    public String resumo() {
        return String.format("%s %s - %s", statusTexto(), getTitulo(), getDescricao());
    }

    @Override
    public String toString() {
        return resumo();
    }
}
