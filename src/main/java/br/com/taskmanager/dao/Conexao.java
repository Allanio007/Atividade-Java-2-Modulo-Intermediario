package br.com.taskmanager.dao;

import br.com.taskmanager.exceptions.PersistenciaException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centraliza o acesso ao banco SQLite.
 * <p>
 * Mantem uma unica conexao aberta durante a vida da aplicacao, o que e o
 * recomendado para SQLite em aplicacao desktop e e obrigatorio para o banco
 * em memoria usado nos testes ({@code jdbc:sqlite::memory:} so existe
 * enquanto a conexao que o criou estiver aberta).
 */
public class Conexao implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(Conexao.class.getName());

    /** Arquivo do banco criado na pasta onde a aplicacao e executada. */
    public static final String URL_PADRAO = "jdbc:sqlite:taskmanager.db";
    /** Banco volatil, usado nos testes JUnit. */
    public static final String URL_MEMORIA = "jdbc:sqlite::memory:";

    private static final String SQL_CRIAR_TABELA = """
            CREATE TABLE IF NOT EXISTS tarefas (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                titulo       VARCHAR(100) NOT NULL,
                descricao    TEXT,
                concluida    BOOLEAN NOT NULL DEFAULT 0,
                prioridade   INTEGER CHECK (prioridade IS NULL OR prioridade BETWEEN 1 AND 5),
                data_criacao TEXT NOT NULL
            )""";

    private final String url;
    private Connection conexao;

    public Conexao() {
        this(URL_PADRAO);
    }

    public Conexao(String url) {
        this.url = url;
    }

    /** Abre a conexao (na primeira chamada) com DriverManager e garante a tabela. */
    public synchronized Connection getConnection() {
        try {
            if (conexao == null || conexao.isClosed()) {
                conexao = DriverManager.getConnection(url);
                criarTabela(conexao);
                LOG.info(() -> "Conectado ao banco: " + url);
            }
            return conexao;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Falha ao conectar em " + url, e);
            throw new PersistenciaException("Nao foi possivel conectar ao banco de dados.", e);
        }
    }

    private void criarTabela(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute(SQL_CRIAR_TABELA);
        }
    }

    public String getUrl() {
        return url;
    }

    @Override
    public synchronized void close() {
        if (conexao != null) {
            try {
                conexao.close();
            } catch (SQLException e) {
                LOG.log(Level.WARNING, "Erro ao fechar conexao", e);
            } finally {
                conexao = null;
            }
        }
    }
}
