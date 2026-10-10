package br.com.taskmanager.generics;

import br.com.taskmanager.model.ITarefa;
import br.com.taskmanager.model.Tarefa;
import br.com.taskmanager.model.TarefaPrioritaria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Generics: ListaGenerica e UtilitariosGenericos")
class ListaGenericaTest {

    private ListaGenerica<Tarefa> lista;

    @BeforeEach
    void setUp() {
        lista = new ListaGenerica<>();
    }

    @Test
    void funcionaComQualquerTipo() {
        ListaGenerica<String> textos = new ListaGenerica<>();
        textos.adicionar("a");
        textos.adicionar("b");
        assertTrue(textos.remover("a"));
        assertEquals(List.of("b"), textos.listarTodos());
    }

    @Test
    void adicionarListarERemover() {
        Tarefa a = new Tarefa("A", "");
        Tarefa b = new TarefaPrioritaria("B", "", 2); // subtipo aceito
        lista.adicionar(a);
        lista.adicionar(b);

        assertEquals(2, lista.tamanho());
        assertTrue(lista.remover(a));
        assertFalse(lista.remover(a));
        assertEquals(List.of(b), lista.listarTodos());
    }

    @Test
    void naoAceitaNulo() {
        assertThrows(IllegalArgumentException.class, () -> lista.adicionar(null));
    }

    @Test
    void listarTodosDevolveCopiaSomenteLeitura() {
        lista.adicionar(new Tarefa("A", ""));
        List<Tarefa> copia = lista.listarTodos();
        assertThrows(UnsupportedOperationException.class, () -> copia.add(new Tarefa("B", "")));
        assertEquals(1, lista.tamanho());
    }

    @Test
    @DisplayName("? extends T: adiciona uma List<TarefaPrioritaria> numa ListaGenerica<Tarefa>")
    void adicionarTodosComExtends() {
        List<TarefaPrioritaria> prioritarias = List.of(
                new TarefaPrioritaria("P1", "", 1),
                new TarefaPrioritaria("P2", "", 2));

        lista.adicionarTodos(prioritarias);

        assertEquals(2, lista.tamanho());
    }

    @Test
    @DisplayName("? super T: copia uma ListaGenerica<Tarefa> para List<ITarefa> e List<Object>")
    void copiarParaComSuper() {
        lista.adicionar(new Tarefa("A", ""));
        lista.adicionar(new TarefaPrioritaria("B", "", 1));

        List<ITarefa> interfaces = new ArrayList<>();
        List<Object> objetos = new ArrayList<>();
        lista.copiarPara(interfaces);
        lista.copiarPara(objetos);

        assertEquals(2, interfaces.size());
        assertEquals(2, objetos.size());
    }

    @Test
    @DisplayName("? super T: Predicate<ITarefa> filtra uma ListaGenerica<Tarefa>")
    void filtrarComPredicateDoSupertipo() {
        Tarefa feita = new Tarefa("Feita", "");
        feita.concluir();
        lista.adicionar(feita);
        lista.adicionar(new Tarefa("Aberta", ""));

        Predicate<ITarefa> concluidas = ITarefa::isConcluida;
        assertEquals(List.of(feita), lista.filtrar(concluidas));
        assertTrue(lista.buscarPrimeiro(t -> t.getTitulo().equals("Aberta")).isPresent());
        assertTrue(lista.buscarPrimeiro(t -> t.getTitulo().equals("X")).isEmpty());
    }

    @Test
    void ordenarERemoverSe() {
        lista.adicionar(new Tarefa("C", ""));
        lista.adicionar(new Tarefa("A", ""));
        lista.adicionar(new Tarefa("B", ""));

        Comparator<ITarefa> porTitulo = Comparator.comparing(ITarefa::getTitulo);
        List<Tarefa> ordenadas = lista.ordenar(porTitulo);
        assertEquals("A", ordenadas.get(0).getTitulo());
        assertEquals("C", ordenadas.get(2).getTitulo());

        assertEquals(1, lista.removerSe(t -> t.getTitulo().equals("B")));
        assertEquals(2, lista.tamanho());

        lista.limpar();
        assertTrue(lista.estaVazia());
    }

    @Test
    @DisplayName("Curingas nos metodos estaticos: ?, ? extends ITarefa, ? super T")
    void utilitariosGenericos() {
        Tarefa a = new Tarefa("A", "");
        a.concluir();
        List<TarefaPrioritaria> prioritarias = List.of(new TarefaPrioritaria("P", "", 1));
        List<Tarefa> tarefas = List.of(a, new Tarefa("B", ""), new Tarefa("C", ""), new Tarefa("D", ""));

        assertEquals(3, UtilitariosGenericos.contar(Set.of(1, 2, 3)));      // ?
        assertEquals(0, UtilitariosGenericos.contar(null));
        assertEquals(1, UtilitariosGenericos.contarConcluidas(tarefas));    // ? extends
        assertEquals(0, UtilitariosGenericos.contarConcluidas(prioritarias));
        assertEquals(25, UtilitariosGenericos.percentualConcluido(tarefas));
        assertEquals(0, UtilitariosGenericos.percentualConcluido(List.of()));

        List<ITarefa> destino = new ArrayList<>();
        UtilitariosGenericos.copiar(prioritarias, destino);                // ? super
        assertEquals(1, destino.size());

        List<String> titulos = UtilitariosGenericos.mapear(tarefas, ITarefa::getTitulo);
        assertEquals(List.of("A", "B", "C", "D"), titulos);
    }
}
