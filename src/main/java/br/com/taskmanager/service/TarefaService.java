package br.com.taskmanager.service;

import br.com.taskmanager.dao.TarefaDAO;
import br.com.taskmanager.exceptions.EntradaInvalidaException;
import br.com.taskmanager.generics.ListaGenerica;
import br.com.taskmanager.generics.UtilitariosGenericos;
import br.com.taskmanager.model.ITarefa;
import br.com.taskmanager.model.Tarefa;
import br.com.taskmanager.model.TarefaPrioritaria;
import br.com.taskmanager.util.ValidadorTarefa;

import java.util.List;
import java.util.function.Predicate;

/**
 * Regras de negocio do TaskManager (evolucao da classe TaskManager da
 * versao console).
 * <p>
 * Diferencas principais:
 * <ul>
 *   <li>as tarefas sao persistidas pelo {@link TarefaDAO} (JDBC);</li>
 *   <li>a copia em memoria usa {@link ListaGenerica}&lt;Tarefa&gt;;</li>
 *   <li>as operacoes usam o id da tarefa em vez do indice da lista;</li>
 *   <li>nova operacao: editar.</li>
 * </ul>
 */
public class TarefaService {
    private final TarefaDAO dao;
    private final ListaGenerica<Tarefa> cache = new ListaGenerica<>();

    public TarefaService(TarefaDAO dao) {
        this.dao = dao;
        recarregar();
    }

    /** Recarrega a lista em memoria a partir do banco. */
    public final void recarregar() {
        cache.substituirTodos(dao.listarTodos());
    }

    // ------------------------------------------------------------ criacao

    public Tarefa adicionarTarefa(String titulo, String descricao) {
        return salvarNova(new Tarefa(
                ValidadorTarefa.validarTitulo(titulo),
                ValidadorTarefa.validarDescricao(descricao)));
    }

    public Tarefa adicionarTarefaPrioritaria(String titulo, String descricao, Integer prioridade) {
        return salvarNova(new TarefaPrioritaria(
                ValidadorTarefa.validarTitulo(titulo),
                ValidadorTarefa.validarDescricao(descricao),
                ValidadorTarefa.validarPrioridade(prioridade)));
    }

    /** Cria normal ou prioritaria conforme a prioridade seja nula ou nao. */
    public Tarefa criar(String titulo, String descricao, Integer prioridade) {
        return prioridade == null
                ? adicionarTarefa(titulo, descricao)
                : adicionarTarefaPrioritaria(titulo, descricao, prioridade);
    }

    private Tarefa salvarNova(Tarefa tarefa) {
        dao.inserir(tarefa);
        recarregar();
        return tarefa;
    }

    // ------------------------------------------------------------ edicao

    /**
     * Edita uma tarefa existente. Se a tarefa mudar de tipo (normal para
     * prioritaria ou o contrario), um novo objeto do tipo correto e criado
     * mantendo o mesmo id e a mesma data de criacao.
     *
     * @param prioridade null para tarefa normal, 1..5 para prioritaria
     */
    public Tarefa editar(int id, String titulo, String descricao,
                         Integer prioridade, boolean concluida) {
        Tarefa atual = buscar(id);
        String t = ValidadorTarefa.validarTitulo(titulo);
        String d = ValidadorTarefa.validarDescricao(descricao);

        Tarefa editada = prioridade == null
                ? new Tarefa(id, t, d, concluida, atual.getDataCriacao())
                : new TarefaPrioritaria(id, t, d, concluida, atual.getDataCriacao(),
                ValidadorTarefa.validarPrioridade(prioridade));

        dao.atualizar(editada);
        recarregar();
        return editada;
    }

    // ------------------------------------------------------------ status

    /** @return false se a tarefa ja estava concluida (mesma regra do console) */
    public boolean concluirTarefa(int id) {
        return definirStatus(id, true);
    }

    /** @return false se a tarefa ja estava em aberto */
    public boolean reabrirTarefa(int id) {
        return definirStatus(id, false);
    }

    public boolean definirStatus(int id, boolean concluida) {
        Tarefa tarefa = buscar(id);
        if (tarefa.isConcluida() == concluida) {
            return false;
        }
        dao.atualizarStatus(id, concluida);
        if (concluida) {
            tarefa.concluir();
        } else {
            tarefa.reabrir();
        }
        return true;
    }

    // ------------------------------------------------------------ remocao

    public Tarefa removerTarefa(int id) {
        Tarefa tarefa = buscar(id);
        dao.excluir(id);
        cache.remover(tarefa);
        return tarefa;
    }

    // ------------------------------------------------------------ consulta

    public Tarefa buscar(int id) {
        return cache.buscarPrimeiro(t -> t.getId() == id)
                .orElseThrow(() -> new EntradaInvalidaException("Tarefa não encontrada (id " + id + ")."));
    }

    public List<Tarefa> listarTarefas() {
        return cache.listarTodos();
    }

    /**
     * Filtro usado pela tela. O criterio e um Predicate&lt;ITarefa&gt;
     * (supertipo) aplicado a uma ListaGenerica&lt;Tarefa&gt; gracas ao
     * {@code ? super T} de {@link ListaGenerica#filtrar}.
     */
    public List<Tarefa> filtrar(String texto, boolean ocultarConcluidas) {
        String busca = texto == null ? "" : texto.trim().toLowerCase();
        Predicate<ITarefa> criterio = t ->
                (busca.isEmpty()
                        || t.getTitulo().toLowerCase().contains(busca)
                        || t.getDescricao().toLowerCase().contains(busca))
                        && !(ocultarConcluidas && t.isConcluida());
        return cache.filtrar(criterio);
    }

    public int totalTarefas() {
        return cache.tamanho();
    }

    public long totalConcluidas() {
        return UtilitariosGenericos.contarConcluidas(cache.listarTodos());
    }

    public int percentualConcluido() {
        return UtilitariosGenericos.percentualConcluido(cache.listarTodos());
    }
}
