package br.com.taskmanager.generics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Lista generica reutilizavel (exemplo pratico do enunciado).
 * <p>
 * Funciona com qualquer tipo T. No TaskManager e usada como
 * {@code ListaGenerica<Tarefa>}, guardando tanto Tarefa quanto
 * TarefaPrioritaria (polimorfismo + generics).
 *
 * @param <T> tipo dos elementos
 */
public class ListaGenerica<T> {
    private final List<T> elementos = new ArrayList<>();

    public void adicionar(T elemento) {
        if (elemento == null) {
            throw new IllegalArgumentException("Elemento nao pode ser nulo.");
        }
        elementos.add(elemento);
    }

    /**
     * {@code ? extends T}: aceita uma colecao de T OU de qualquer subtipo.
     * Ex.: em uma ListaGenerica&lt;Tarefa&gt; podemos adicionar uma
     * List&lt;TarefaPrioritaria&gt; (a colecao e so LIDA - "producer").
     */
    public void adicionarTodos(Collection<? extends T> outros) {
        for (T elemento : outros) {
            adicionar(elemento);
        }
    }

    public boolean remover(T elemento) {
        return elementos.remove(elemento);
    }

    /** Remove todos os elementos que satisfazem o criterio. */
    public int removerSe(Predicate<? super T> criterio) {
        int antes = elementos.size();
        elementos.removeIf(criterio);
        return antes - elementos.size();
    }

    /** Copia somente leitura de todos os elementos. */
    public List<T> listarTodos() {
        return Collections.unmodifiableList(new ArrayList<>(elementos));
    }

    /**
     * {@code ? super T} no Predicate: um criterio escrito para um supertipo
     * (ex.: Predicate&lt;ITarefa&gt;) tambem serve para filtrar Tarefa.
     */
    public List<T> filtrar(Predicate<? super T> criterio) {
        List<T> resultado = new ArrayList<>();
        for (T elemento : elementos) {
            if (criterio.test(elemento)) {
                resultado.add(elemento);
            }
        }
        return resultado;
    }

    public Optional<T> buscarPrimeiro(Predicate<? super T> criterio) {
        for (T elemento : elementos) {
            if (criterio.test(elemento)) {
                return Optional.of(elemento);
            }
        }
        return Optional.empty();
    }

    public List<T> ordenar(Comparator<? super T> comparador) {
        List<T> copia = new ArrayList<>(elementos);
        copia.sort(comparador);
        return copia;
    }

    /**
     * {@code ? super T}: o destino pode ser uma colecao de T ou de um
     * supertipo (ex.: List&lt;Object&gt;, List&lt;ITarefa&gt;) - a colecao e
     * so ESCRITA ("consumer"). Regra PECS: Producer Extends, Consumer Super.
     */
    public void copiarPara(Collection<? super T> destino) {
        destino.addAll(elementos);
    }

    public void paraCada(Consumer<? super T> acao) {
        elementos.forEach(acao);
    }

    public void substituirTodos(Collection<? extends T> novos) {
        elementos.clear();
        adicionarTodos(novos);
    }

    public int tamanho() {
        return elementos.size();
    }

    public boolean estaVazia() {
        return elementos.isEmpty();
    }

    public void limpar() {
        elementos.clear();
    }
}
