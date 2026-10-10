package br.com.taskmanager.dao;

import br.com.taskmanager.exceptions.PersistenciaException;
import br.com.taskmanager.generics.GenericDAO;
import br.com.taskmanager.model.Tarefa;
import br.com.taskmanager.model.TarefaPrioritaria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Isola toda a persistencia das tarefas (padrao DAO).
 * <ul>
 *   <li>Todas as consultas usam PreparedStatement com parametros "?", o que
 *       evita SQL Injection (o texto digitado nunca e concatenado no SQL).</li>
 *   <li>Escritas rodam em transacao: commit() no sucesso, rollback() na falha.</li>
 *   <li>Uma tarefa prioritaria e gravada com a coluna prioridade preenchida;
 *       uma tarefa normal, com prioridade NULL.</li>
 * </ul>
 */
public class TarefaDAO implements GenericDAO<Tarefa, Integer> {
    private static final Logger LOG = Logger.getLogger(TarefaDAO.class.getName());

    private static final String SQL_INSERIR =
            "INSERT INTO tarefas (titulo, descricao, concluida, prioridade, data_criacao) "
                    + "VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_ULTIMO_ID = "SELECT last_insert_rowid()";
    private static final String SQL_BUSCAR_POR_ID =
            "SELECT id, titulo, descricao, concluida, prioridade, data_criacao "
                    + "FROM tarefas WHERE id = ?";
    private static final String SQL_LISTAR =
            "SELECT id, titulo, descricao, concluida, prioridade, data_criacao "
                    + "FROM tarefas ORDER BY concluida, COALESCE(prioridade, 99), id";
    private static final String SQL_BUSCAR_POR_TITULO =
            "SELECT id, titulo, descricao, concluida, prioridade, data_criacao "
                    + "FROM tarefas WHERE LOWER(titulo) LIKE ? ORDER BY id";
    private static final String SQL_ATUALIZAR =
            "UPDATE tarefas SET titulo = ?, descricao = ?, concluida = ?, prioridade = ? "
                    + "WHERE id = ?";
    private static final String SQL_ATUALIZAR_STATUS =
            "UPDATE tarefas SET concluida = ? WHERE id = ?";
    private static final String SQL_EXCLUIR = "DELETE FROM tarefas WHERE id = ?";

    private final Conexao conexao;

    public TarefaDAO(Conexao conexao) {
        this.conexao = conexao;
    }

    // ------------------------------------------------------------ escrita

    @Override
    public Tarefa inserir(Tarefa tarefa) {
        return emTransacao("inserir tarefa", c -> {
            inserirSemCommit(c, tarefa);
            return tarefa;
        });
    }

    @Override
    public List<Tarefa> inserirTodos(Collection<? extends Tarefa> tarefas) {
        // Se qualquer insercao falhar, o rollback desfaz TODAS as anteriores.
        try {
            return emTransacao("inserir lote de tarefas", c -> {
                List<Tarefa> inseridas = new ArrayList<>();
                for (Tarefa t : tarefas) {
                    inserirSemCommit(c, t);
                    inseridas.add(t);
                }
                return inseridas;
            });
        } catch (PersistenciaException e) {
            // Os ids gerados antes do rollback nao existem mais no banco.
            tarefas.forEach(t -> t.setId(null));
            throw e;
        }
    }

    private void inserirSemCommit(Connection c, Tarefa t) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SQL_INSERIR)) {
            ps.setString(1, t.getTitulo());
            ps.setString(2, t.getDescricao());
            ps.setBoolean(3, t.isConcluida());
            definirPrioridade(ps, 4, t);
            ps.setString(5, t.getDataCriacao().toString());
            ps.executeUpdate();
        }
        // O driver sqlite-jdbc (3.43+) nao implementa RETURN_GENERATED_KEYS;
        // last_insert_rowid() devolve o id gerado nesta mesma conexao.
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(SQL_ULTIMO_ID)) {
            if (rs.next()) {
                t.setId(rs.getInt(1));
            }
        }
    }

    @Override
    public boolean atualizar(Tarefa tarefa) {
        exigirId(tarefa.getId());
        return emTransacao("atualizar tarefa", c -> {
            try (PreparedStatement ps = c.prepareStatement(SQL_ATUALIZAR)) {
                ps.setString(1, tarefa.getTitulo());
                ps.setString(2, tarefa.getDescricao());
                ps.setBoolean(3, tarefa.isConcluida());
                definirPrioridade(ps, 4, tarefa);
                ps.setInt(5, tarefa.getId());
                return ps.executeUpdate() == 1;
            }
        });
    }

    /** Atualizacao parcial usada pelo CheckBox da tabela. */
    public boolean atualizarStatus(int id, boolean concluida) {
        return emTransacao("atualizar status", c -> {
            try (PreparedStatement ps = c.prepareStatement(SQL_ATUALIZAR_STATUS)) {
                ps.setBoolean(1, concluida);
                ps.setInt(2, id);
                return ps.executeUpdate() == 1;
            }
        });
    }

    @Override
    public boolean excluir(Integer id) {
        exigirId(id);
        return emTransacao("excluir tarefa", c -> {
            try (PreparedStatement ps = c.prepareStatement(SQL_EXCLUIR)) {
                ps.setInt(1, id);
                return ps.executeUpdate() == 1;
            }
        });
    }

    // ------------------------------------------------------------ leitura

    @Override
    public Optional<Tarefa> buscarPorId(Integer id) {
        exigirId(id);
        try (PreparedStatement ps = conexao.getConnection().prepareStatement(SQL_BUSCAR_POR_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw falha("buscar tarefa por id", e);
        }
    }

    @Override
    public List<Tarefa> listarTodos() {
        try (PreparedStatement ps = conexao.getConnection().prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            return mapearTodos(rs);
        } catch (SQLException e) {
            throw falha("listar tarefas", e);
        }
    }

    /** Busca por parte do titulo; o texto vai como parametro, nunca no SQL. */
    public List<Tarefa> buscarPorTitulo(String trecho) {
        try (PreparedStatement ps = conexao.getConnection().prepareStatement(SQL_BUSCAR_POR_TITULO)) {
            ps.setString(1, "%" + (trecho == null ? "" : trecho.toLowerCase()) + "%");
            try (ResultSet rs = ps.executeQuery()) {
                return mapearTodos(rs);
            }
        } catch (SQLException e) {
            throw falha("buscar tarefas por titulo", e);
        }
    }

    // ------------------------------------------------------------ apoio

    private List<Tarefa> mapearTodos(ResultSet rs) throws SQLException {
        List<Tarefa> lista = new ArrayList<>();
        while (rs.next()) {
            lista.add(mapear(rs));
        }
        return lista;
    }

    /** Converte uma linha do ResultSet no subtipo correto (polimorfismo). */
    private Tarefa mapear(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String titulo = rs.getString("titulo");
        String descricao = rs.getString("descricao");
        boolean concluida = rs.getBoolean("concluida");
        LocalDateTime criacao = LocalDateTime.parse(rs.getString("data_criacao"));
        int prioridade = rs.getInt("prioridade");
        if (rs.wasNull()) {
            return new Tarefa(id, titulo, descricao, concluida, criacao);
        }
        return new TarefaPrioritaria(id, titulo, descricao, concluida, criacao, prioridade);
    }

    private void definirPrioridade(PreparedStatement ps, int indice, Tarefa t) throws SQLException {
        if (t.getPrioridade() == null) {
            ps.setNull(indice, Types.INTEGER);
        } else {
            ps.setInt(indice, t.getPrioridade());
        }
    }

    private void exigirId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("A tarefa precisa ter id (ja estar salva).");
        }
    }

    /** Operacao que pode lancar SQLException (o Function padrao nao pode). */
    @FunctionalInterface
    private interface OperacaoSql<R> {
        R executar(Connection c) throws SQLException;
    }

    /**
     * Executa a operacao em transacao: desliga o auto-commit, faz commit() se
     * tudo deu certo e rollback() se houve erro, restaurando o auto-commit.
     */
    private <R> R emTransacao(String descricao, OperacaoSql<R> operacao) {
        Connection c = conexao.getConnection();
        try {
            c.setAutoCommit(false);
            R resultado = operacao.executar(c);
            c.commit();
            return resultado;
        } catch (SQLException e) {
            try {
                c.rollback();
                LOG.warning(() -> "Rollback executado em: " + descricao);
            } catch (SQLException ex) {
                e.addSuppressed(ex);
            }
            throw falha(descricao, e);
        } finally {
            try {
                c.setAutoCommit(true);
            } catch (SQLException e) {
                LOG.log(Level.WARNING, "Nao foi possivel restaurar auto-commit", e);
            }
        }
    }

    private PersistenciaException falha(String operacao, SQLException e) {
        LOG.log(Level.SEVERE, "Erro de banco ao " + operacao, e);
        return new PersistenciaException("Erro ao " + operacao + ": " + e.getMessage(), e);
    }
}
