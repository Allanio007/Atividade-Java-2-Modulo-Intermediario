package br.com.taskmanager.model;

import java.time.LocalDateTime;

/** Tarefa com prioridade de 1 (mais alta) a 5. */
public class TarefaPrioritaria extends Tarefa {
    public static final int PRIORIDADE_MIN = 1;
    public static final int PRIORIDADE_MAX = 5;

    private int prioridade;

    public TarefaPrioritaria(String titulo, String descricao, int prioridade) {
        super(titulo, descricao);
        this.prioridade = validar(prioridade);
    }

    public TarefaPrioritaria(Integer id, String titulo, String descricao, boolean concluida,
                             LocalDateTime dataCriacao, int prioridade) {
        super(id, titulo, descricao, concluida, dataCriacao);
        this.prioridade = validar(prioridade);
    }

    @Override
    public Integer getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(int prioridade) {
        this.prioridade = validar(prioridade);
    }

    private static int validar(int prioridade) {
        if (prioridade < PRIORIDADE_MIN || prioridade > PRIORIDADE_MAX) {
            throw new IllegalArgumentException("A prioridade deve estar entre 1 e 5.");
        }
        return prioridade;
    }

    @Override
    public String getTipo() {
        return "Prioritaria";
    }

    @Override
    public String resumo() {
        return String.format("%s (Prioridade: %d)", super.resumo(), prioridade);
    }
}
