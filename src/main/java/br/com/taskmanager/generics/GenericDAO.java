package br.com.taskmanager.generics;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Contrato generico de persistencia (CRUD). Pode ser implementado para
 * qualquer entidade: TarefaDAO implementa GenericDAO&lt;Tarefa, Integer&gt;.
 *
 * @param <T>  tipo da entidade
 * @param <ID> tipo da chave primaria
 */
public interface GenericDAO<T, ID> {

    T inserir(T entidade);

    /** Insere varias entidades em UMA transacao (tudo ou nada). */
    List<T> inserirTodos(Collection<? extends T> entidades);

    Optional<T> buscarPorId(ID id);

    List<T> listarTodos();

    boolean atualizar(T entidade);

    boolean excluir(ID id);
}
