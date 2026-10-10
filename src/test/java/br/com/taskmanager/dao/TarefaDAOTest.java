package br.com.taskmanager.dao;

import br.com.taskmanager.exceptions.PersistenciaException;
import br.com.taskmanager.model.Tarefa;
import br.com.taskmanager.model.TarefaPrioritaria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa o DAO com SQLite EM MEMORIA: cada teste recebe um banco novo e vazio
 * (@BeforeEach) que e descartado ao fechar a conexao (@AfterEach).
 */
@DisplayName("DAO com SQLite em memoria")
class TarefaDAOTest {

    private Conexao conexao;
    private TarefaDAO dao;

    @BeforeEach
    void setUp() {
        conexao = new Conexao(Conexao.URL_MEMORIA);
        dao = new TarefaDAO(conexao);
    }

    @AfterEach
    void tearDown() {
        conexao.close();
    }

    @Test
    void bancoComecaVazio() {
        assertTrue(dao.listarTodos().isEmpty());
    }

    @Test
    void inserirGeraIdAutoincremento() {
        Tarefa a = dao.inserir(new Tarefa("A", "desc"));
        Tarefa b = dao.inserir(new Tarefa("B", "desc"));

        assertNotNull(a.getId());
        assertEquals(a.getId() + 1, b.getId());
        assertEquals(2, dao.listarTodos().size());
    }

    @Test
    void buscarPorIdReconstroiTodosOsCampos() {
        Tarefa original = dao.inserir(new Tarefa("Estudar JDBC", "PreparedStatement"));

        Tarefa lida = dao.buscarPorId(original.getId()).orElseThrow();

        assertEquals(original.getId(), lida.getId());
        assertEquals("Estudar JDBC", lida.getTitulo());
        assertEquals("PreparedStatement", lida.getDescricao());
        assertFalse(lida.isConcluida());
        assertEquals(original.getDataCriacao(), lida.getDataCriacao());
        assertNull(lida.getPrioridade());
    }

    @Test
    @DisplayName("Polimorfismo: prioridade preenchida volta como TarefaPrioritaria")
    void tarefaPrioritariaVoltaComOTipoCerto() {
        Tarefa salva = dao.inserir(new TarefaPrioritaria("Prova", "Unidade 5", 2));

        Tarefa lida = dao.buscarPorId(salva.getId()).orElseThrow();

        assertInstanceOf(TarefaPrioritaria.class, lida);
        assertEquals(2, lida.getPrioridade());
    }

    @Test
    void buscarIdInexistenteRetornaVazio() {
        assertEquals(Optional.empty(), dao.buscarPorId(999));
    }

    @Test
    void listarOrdenaAbertasPrimeiroEPorPrioridade() {
        Tarefa feita = new Tarefa("Feita", "");
        feita.concluir();
        dao.inserir(feita);
        dao.inserir(new Tarefa("Normal", ""));
        dao.inserir(new TarefaPrioritaria("P3", "", 3));
        dao.inserir(new TarefaPrioritaria("P1", "", 1));

        List<String> ordem = dao.listarTodos().stream().map(Tarefa::getTitulo).toList();

        assertEquals(List.of("P1", "P3", "Normal", "Feita"), ordem);
    }

    @Test
    void atualizarAlteraOsDados() {
        Tarefa t = dao.inserir(new Tarefa("Antigo", "x"));
        t.setTitulo("Novo");
        t.setDescricao("y");
        t.concluir();

        assertTrue(dao.atualizar(t));

        Tarefa lida = dao.buscarPorId(t.getId()).orElseThrow();
        assertEquals("Novo", lida.getTitulo());
        assertEquals("y", lida.getDescricao());
        assertTrue(lida.isConcluida());
    }

    @Test
    void atualizarPodeTrocarOTipoDaTarefa() {
        Tarefa t = dao.inserir(new Tarefa("A", ""));
        Tarefa agoraPrioritaria = new TarefaPrioritaria(t.getId(), "A", "", false, t.getDataCriacao(), 1);

        dao.atualizar(agoraPrioritaria);

        assertInstanceOf(TarefaPrioritaria.class, dao.buscarPorId(t.getId()).orElseThrow());
    }

    @Test
    void atualizarIdInexistenteRetornaFalse() {
        Tarefa fantasma = new Tarefa(42, "X", "", false, LocalDateTime.now());
        assertFalse(dao.atualizar(fantasma));
    }

    @Test
    void atualizarStatus() {
        Tarefa t = dao.inserir(new Tarefa("A", ""));

        assertTrue(dao.atualizarStatus(t.getId(), true));
        assertTrue(dao.buscarPorId(t.getId()).orElseThrow().isConcluida());

        assertTrue(dao.atualizarStatus(t.getId(), false));
        assertFalse(dao.buscarPorId(t.getId()).orElseThrow().isConcluida());
    }

    @Test
    void excluir() {
        Tarefa t = dao.inserir(new Tarefa("A", ""));

        assertTrue(dao.excluir(t.getId()));
        assertFalse(dao.excluir(t.getId()), "segunda exclusao nao encontra a linha");
        assertTrue(dao.buscarPorId(t.getId()).isEmpty());
    }

    @Test
    void operacoesSemIdSaoRecusadas() {
        Tarefa naoSalva = new Tarefa("A", "");
        assertThrows(IllegalArgumentException.class, () -> dao.atualizar(naoSalva));
        assertThrows(IllegalArgumentException.class, () -> dao.excluir(null));
    }

    @Test
    void buscarPorTituloIgnoraMaiusculas() {
        dao.inserir(new Tarefa("Estudar Java", ""));
        dao.inserir(new Tarefa("Lavar louca", ""));

        assertEquals(1, dao.buscarPorTitulo("JAVA").size());
        assertEquals(2, dao.buscarPorTitulo("").size());
    }

    @Test
    @DisplayName("PreparedStatement impede SQL Injection")
    void sqlInjectionNaoFunciona() {
        dao.inserir(new Tarefa("Tarefa secreta", ""));

        // Se o texto fosse concatenado no SQL, este filtro retornaria todas as linhas.
        assertTrue(dao.buscarPorTitulo("' OR '1'='1").isEmpty());

        // Texto malicioso e gravado literalmente e a tabela continua intacta.
        Tarefa maliciosa = dao.inserir(new Tarefa("x'); DROP TABLE tarefas; --", ""));
        assertEquals(2, dao.listarTodos().size());
        assertEquals("x'); DROP TABLE tarefas; --",
                dao.buscarPorId(maliciosa.getId()).orElseThrow().getTitulo());
    }

    @Test
    @DisplayName("Transacao: falha no meio do lote faz rollback de tudo")
    void inserirTodosFazRollback() {
        Tarefa ok1 = new Tarefa("OK 1", "");
        Tarefa ok2 = new Tarefa("OK 2", "");
        Tarefa invalida = new Tarefa(null, "titulo NOT NULL viola a restricao");

        assertThrows(PersistenciaException.class, () -> dao.inserirTodos(List.of(ok1, ok2, invalida)));

        assertTrue(dao.listarTodos().isEmpty(), "nenhuma linha do lote pode ter ficado gravada");
        assertNull(ok1.getId(), "ids gerados antes do rollback sao descartados");
        assertNull(ok2.getId());
    }

    @Test
    void inserirTodosComSucessoFazCommit() {
        List<Tarefa> inseridas = dao.inserirTodos(List.of(
                new Tarefa("A", ""), new TarefaPrioritaria("B", "", 1)));

        assertEquals(2, inseridas.size());
        assertEquals(2, dao.listarTodos().size());
    }

    @Test
    void conexaoComUrlInvalidaLancaPersistenciaException() {
        Conexao ruim = new Conexao("jdbc:banco-que-nao-existe:teste");
        assertThrows(PersistenciaException.class, ruim::getConnection);
    }
}
