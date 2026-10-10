package br.com.taskmanager.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Modelo: Tarefa e TarefaPrioritaria")
class TarefaTest {

    @Test
    @DisplayName("Criacao de tarefa: comeca em aberto, sem id e com data de criacao")
    void deveCriarTarefaEmAberto() {
        Tarefa t = new Tarefa("Estudar JavaFX", "Ler sobre FXML");

        assertEquals("Estudar JavaFX", t.getTitulo());
        assertEquals("Ler sobre FXML", t.getDescricao());
        assertFalse(t.isConcluida());
        assertNull(t.getId(), "id so e definido pelo banco");
        assertNotNull(t.getDataCriacao());
        assertNull(t.getPrioridade());
        assertEquals("Normal", t.getTipo());
    }

    @Test
    @DisplayName("Conclusao de tarefa: concluir e reabrir alteram o status")
    void deveConcluirEReabrir() {
        Tarefa t = new Tarefa("A", "B");

        t.concluir();
        assertTrue(t.isConcluida());

        t.reabrir();
        assertFalse(t.isConcluida());
    }

    @Test
    void tarefaPrioritariaGuardaPrioridade() {
        TarefaPrioritaria t = new TarefaPrioritaria("Prova", "Estudar", 1);

        assertEquals(1, t.getPrioridade());
        assertEquals("Prioritaria", t.getTipo());
        assertEquals("[ ] Prova - Estudar (Prioridade: 1)", t.resumo());
    }

    @Test
    void prioridadeForaDaFaixaLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> new TarefaPrioritaria("A", "B", 0));
        assertThrows(IllegalArgumentException.class, () -> new TarefaPrioritaria("A", "B", 6));

        TarefaPrioritaria t = new TarefaPrioritaria("A", "B", 3);
        assertThrows(IllegalArgumentException.class, () -> t.setPrioridade(9));
        assertEquals(3, t.getPrioridade(), "valor anterior e mantido");
    }

    @Test
    void resumoMostraStatus() {
        Tarefa t = new Tarefa("Ler", "Capitulo 1");
        assertEquals("[ ] Ler - Capitulo 1", t.resumo());
        t.concluir();
        assertEquals("[x] Ler - Capitulo 1", t.resumo());
    }

    @Test
    void igualdadeUsaOId() {
        LocalDateTime agora = LocalDateTime.now();
        Tarefa a = new Tarefa(7, "A", "", false, agora);
        Tarefa mesmaId = new Tarefa(7, "Outro titulo", "", true, agora);
        Tarefa outraId = new Tarefa(8, "A", "", false, agora);

        assertEquals(a, mesmaId);
        assertEquals(a.hashCode(), mesmaId.hashCode());
        assertNotEquals(a, outraId);
        // Duas tarefas nao salvas nunca sao iguais entre si
        assertNotEquals(new Tarefa("X", ""), new Tarefa("X", ""));
    }
}
