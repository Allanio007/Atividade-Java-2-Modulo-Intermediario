package br.com.taskmanager.service;

import br.com.taskmanager.dao.Conexao;
import br.com.taskmanager.dao.TarefaDAO;
import br.com.taskmanager.exceptions.EntradaInvalidaException;
import br.com.taskmanager.model.Tarefa;
import br.com.taskmanager.model.TarefaPrioritaria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes das regras de negocio. Inclui os 3 testes da versao console,
 * adaptados para usar o id da tarefa em vez do indice.
 */
@DisplayName("Servico: regras de negocio do TaskManager")
class TarefaServiceTest {

    private Conexao conexao;
    private TarefaService servico;

    @BeforeEach
    void setUp() {
        conexao = new Conexao(Conexao.URL_MEMORIA);
        servico = new TarefaService(new TarefaDAO(conexao));
    }

    @AfterEach
    void tearDown() {
        conexao.close();
    }

    // --- testes herdados da primeira entrega -----------------------------

    @Test
    void deveAdicionarTarefaComum() {
        Tarefa tarefa = servico.adicionarTarefa("Estudar Java", "Revisar POO");
        assertNotNull(tarefa.getId());
        assertEquals(1, servico.listarTarefas().size());
    }

    @Test
    void deveConcluirTarefaPorId() {
        Tarefa t = servico.adicionarTarefa("Atividade", "Fazer modulo 2");
        boolean concluiu = servico.concluirTarefa(t.getId());
        assertTrue(concluiu);
        assertTrue(servico.listarTarefas().get(0).isConcluida());
    }

    @Test
    void deveLancarExcecaoParaIdInvalido() {
        servico.adicionarTarefa("A", "B");
        assertThrows(EntradaInvalidaException.class, () -> servico.removerTarefa(99));
    }

    // --- criacao ---------------------------------------------------------

    @Test
    @DisplayName("Criacao: dados sao validados, recortados e persistidos")
    void criarTarefaPersisteNoBanco() {
        servico.criar("  Ler artigo  ", "  sobre JDBC ", null);

        // Um novo servico sobre a mesma conexao le os dados do banco.
        TarefaService outroServico = new TarefaService(new TarefaDAO(conexao));
        Tarefa lida = outroServico.listarTarefas().get(0);
        assertEquals("Ler artigo", lida.getTitulo());
        assertEquals("sobre JDBC", lida.getDescricao());
    }

    @Test
    void criarComPrioridadeGeraTarefaPrioritaria() {
        Tarefa t = servico.criar("Prova", "", 1);
        assertInstanceOf(TarefaPrioritaria.class, t);
        assertEquals(1, t.getPrioridade());
    }

    @Test
    @DisplayName("Validacao: dados invalidos nao chegam ao banco")
    void dadosInvalidosNaoSaoGravados() {
        assertThrows(EntradaInvalidaException.class, () -> servico.criar("", "x", null));
        assertThrows(EntradaInvalidaException.class, () -> servico.criar("   ", "x", null));
        assertThrows(EntradaInvalidaException.class, () -> servico.criar("ok", "x", 7));
        assertThrows(EntradaInvalidaException.class, () -> servico.criar("a".repeat(101), "", null));

        assertEquals(0, servico.totalTarefas());
    }

    // --- conclusao -------------------------------------------------------

    @Test
    @DisplayName("Conclusao: concluir duas vezes retorna false e persiste o status")
    void concluirTarefa() {
        Tarefa t = servico.adicionarTarefa("A", "");

        assertTrue(servico.concluirTarefa(t.getId()));
        assertFalse(servico.concluirTarefa(t.getId()), "ja estava concluida");

        TarefaService releitura = new TarefaService(new TarefaDAO(conexao));
        assertTrue(releitura.buscar(t.getId()).isConcluida());
    }

    @Test
    void reabrirTarefa() {
        Tarefa t = servico.adicionarTarefa("A", "");
        assertFalse(servico.reabrirTarefa(t.getId()), "ja estava aberta");

        servico.concluirTarefa(t.getId());
        assertTrue(servico.reabrirTarefa(t.getId()));
        assertFalse(servico.buscar(t.getId()).isConcluida());
    }

    // --- edicao ----------------------------------------------------------

    @Test
    void editarAlteraTituloDescricaoEStatus() {
        Tarefa t = servico.adicionarTarefa("Antigo", "x");

        servico.editar(t.getId(), "Novo", "y", null, true);

        Tarefa editada = servico.buscar(t.getId());
        assertEquals("Novo", editada.getTitulo());
        assertEquals("y", editada.getDescricao());
        assertTrue(editada.isConcluida());
    }

    @Test
    @DisplayName("Edicao pode trocar o tipo mantendo id e data de criacao")
    void editarTrocaTipo() {
        Tarefa t = servico.adicionarTarefa("A", "");

        Tarefa prioritaria = servico.editar(t.getId(), "A", "", 2, false);
        assertInstanceOf(TarefaPrioritaria.class, prioritaria);
        assertEquals(t.getId(), prioritaria.getId());
        assertEquals(t.getDataCriacao(), prioritaria.getDataCriacao());

        Tarefa normal = servico.editar(t.getId(), "A", "", null, false);
        assertFalse(normal instanceof TarefaPrioritaria);
        assertEquals(1, servico.totalTarefas());
    }

    @Test
    void editarComTituloVazioNaoAlteraNada() {
        Tarefa t = servico.adicionarTarefa("Original", "");

        assertThrows(EntradaInvalidaException.class,
                () -> servico.editar(t.getId(), " ", "", null, false));

        assertEquals("Original", servico.buscar(t.getId()).getTitulo());
    }

    // --- remocao e consulta ---------------------------------------------

    @Test
    void removerTarefa() {
        Tarefa a = servico.adicionarTarefa("A", "");
        servico.adicionarTarefa("B", "");

        Tarefa removida = servico.removerTarefa(a.getId());

        assertEquals("A", removida.getTitulo());
        assertEquals(1, servico.totalTarefas());
        assertEquals(1, new TarefaService(new TarefaDAO(conexao)).totalTarefas());
    }

    @Test
    void filtrarPorTextoEOcultarConcluidas() {
        Tarefa java = servico.adicionarTarefa("Estudar Java", "JavaFX");
        servico.adicionarTarefa("Mercado", "comprar cafe");
        servico.concluirTarefa(java.getId());

        assertEquals(1, servico.filtrar("java", false).size());
        assertEquals(1, servico.filtrar("CAFE", false).size(), "busca tambem na descricao");
        assertEquals(0, servico.filtrar("java", true).size());
        assertEquals(1, servico.filtrar("", true).size());
        assertEquals(2, servico.filtrar(null, false).size());
    }

    @Test
    void estatisticas() {
        Tarefa a = servico.adicionarTarefa("A", "");
        servico.adicionarTarefa("B", "");
        servico.adicionarTarefa("C", "");
        servico.adicionarTarefa("D", "");
        servico.concluirTarefa(a.getId());

        assertEquals(4, servico.totalTarefas());
        assertEquals(1, servico.totalConcluidas());
        assertEquals(25, servico.percentualConcluido());
    }
}
