package br.com.taskmanager.generics;

import br.com.taskmanager.model.ITarefa;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Metodos genericos estaticos que demonstram os tres tipos de curinga.
 */
public final class UtilitariosGenericos {

    private UtilitariosGenericos() {
    }

    /**
     * Curinga ilimitado {@code ?}: so precisamos do tamanho, o tipo do
     * elemento nao importa.
     */
    public static int contar(Collection<?> colecao) {
        return colecao == null ? 0 : colecao.size();
    }

    /**
     * {@code ? extends ITarefa}: aceita List&lt;Tarefa&gt;,
     * List&lt;TarefaPrioritaria&gt; ou List&lt;ITarefa&gt;.
     */
    public static long contarConcluidas(Collection<? extends ITarefa> tarefas) {
        return tarefas.stream().filter(ITarefa::isConcluida).count();
    }

    /** Percentual de conclusao (0 a 100), arredondado. */
    public static int percentualConcluido(Collection<? extends ITarefa> tarefas) {
        if (tarefas.isEmpty()) {
            return 0;
        }
        return (int) Math.round(contarConcluidas(tarefas) * 100.0 / tarefas.size());
    }

    /**
     * {@code ? super T}: copia elementos de uma origem de T para um destino
     * que aceita T ou qualquer supertipo de T.
     */
    public static <T> void copiar(Collection<? extends T> origem, Collection<? super T> destino) {
        destino.addAll(origem);
    }

    /** Metodo generico com dois parametros de tipo. */
    public static <T, R> List<R> mapear(Collection<? extends T> origem,
                                        Function<? super T, ? extends R> funcao) {
        return origem.stream().map(funcao).collect(Collectors.toList());
    }
}
