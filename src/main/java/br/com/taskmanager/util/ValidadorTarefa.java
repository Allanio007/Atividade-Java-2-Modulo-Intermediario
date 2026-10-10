package br.com.taskmanager.util;

import br.com.taskmanager.exceptions.EntradaInvalidaException;
import br.com.taskmanager.model.TarefaPrioritaria;

/**
 * Regras de validacao dos dados de uma tarefa, isoladas para serem
 * reaproveitadas pela tela e pelo servico e testadas com JUnit.
 */
public final class ValidadorTarefa {
    public static final int TITULO_MAX = 100;   // titulo VARCHAR(100)
    public static final int DESCRICAO_MAX = 1000;

    private ValidadorTarefa() {
    }

    public static String validarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            throw new EntradaInvalidaException("O campo título é obrigatório.");
        }
        String limpo = titulo.trim();
        if (limpo.length() > TITULO_MAX) {
            throw new EntradaInvalidaException(
                    "O título deve ter no máximo " + TITULO_MAX + " caracteres.");
        }
        return limpo;
    }

    /** A descricao e opcional; nulo vira texto vazio. */
    public static String validarDescricao(String descricao) {
        String limpa = descricao == null ? "" : descricao.trim();
        if (limpa.length() > DESCRICAO_MAX) {
            throw new EntradaInvalidaException(
                    "A descrição deve ter no máximo " + DESCRICAO_MAX + " caracteres.");
        }
        return limpa;
    }

    public static int validarPrioridade(Integer prioridade) {
        if (prioridade == null
                || prioridade < TarefaPrioritaria.PRIORIDADE_MIN
                || prioridade > TarefaPrioritaria.PRIORIDADE_MAX) {
            throw new EntradaInvalidaException("A prioridade deve estar entre 1 e 5.");
        }
        return prioridade;
    }
}
