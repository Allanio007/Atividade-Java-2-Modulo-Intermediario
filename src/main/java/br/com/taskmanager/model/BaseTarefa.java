package br.com.taskmanager.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Classe abstrata com os atributos comuns.
 * <p>
 * Mudanca em relacao a versao console: o id deixou de ser um UUID gerado em
 * memoria e passou a ser a chave primaria (INTEGER AUTOINCREMENT) do banco.
 * Enquanto a tarefa nao foi salva, o id e {@code null}.
 */
public abstract class BaseTarefa implements ITarefa {
    private Integer id;
    private String titulo;
    private String descricao;
    private boolean concluida;
    private final LocalDateTime dataCriacao;

    /** Construtor para uma tarefa nova (ainda nao persistida). */
    protected BaseTarefa(String titulo, String descricao) {
        this(null, titulo, descricao, false, LocalDateTime.now());
    }

    /** Construtor usado pelo DAO para reconstruir uma tarefa lida do banco. */
    protected BaseTarefa(Integer id, String titulo, String descricao,
                         boolean concluida, LocalDateTime dataCriacao) {
        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.concluida = concluida;
        // Precisao de segundos: e o que fica gravado no banco.
        this.dataCriacao = (dataCriacao == null ? LocalDateTime.now() : dataCriacao)
                .truncatedTo(ChronoUnit.SECONDS);
    }

    @Override
    public Integer getId() {
        return id;
    }

    /** Definido pelo DAO apos o INSERT (chave gerada pelo banco). */
    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    @Override
    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public boolean isConcluida() {
        return concluida;
    }

    @Override
    public void concluir() {
        this.concluida = true;
    }

    @Override
    public void reabrir() {
        this.concluida = false;
    }

    @Override
    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    protected String statusTexto() {
        return concluida ? "[x]" : "[ ]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseTarefa outra)) {
            return false;
        }
        // Tarefas ainda nao salvas so sao iguais a si mesmas.
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : System.identityHashCode(this);
    }
}
