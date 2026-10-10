package br.com.taskmanager.util;

import br.com.taskmanager.exceptions.EntradaInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Validacao de dados")
class ValidadorTarefaTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void tituloVazioENulo(String titulo) {
        EntradaInvalidaException ex = assertThrows(EntradaInvalidaException.class,
                () -> ValidadorTarefa.validarTitulo(titulo));
        assertEquals("O campo título é obrigatório.", ex.getMessage());
    }

    @Test
    void tituloERecortado() {
        assertEquals("Estudar", ValidadorTarefa.validarTitulo("   Estudar  "));
    }

    @Test
    void tituloNoLimiteEAceitoEAcimaERecusado() {
        String cem = "a".repeat(100);
        assertEquals(cem, ValidadorTarefa.validarTitulo(cem));
        assertThrows(EntradaInvalidaException.class,
                () -> ValidadorTarefa.validarTitulo("a".repeat(101)));
    }

    @Test
    void descricaoEOpcional() {
        assertEquals("", ValidadorTarefa.validarDescricao(null));
        assertEquals("", ValidadorTarefa.validarDescricao("   "));
        assertEquals("texto", ValidadorTarefa.validarDescricao(" texto "));
    }

    @Test
    void descricaoMuitoLongaERecusada() {
        assertThrows(EntradaInvalidaException.class,
                () -> ValidadorTarefa.validarDescricao("x".repeat(ValidadorTarefa.DESCRICAO_MAX + 1)));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void prioridadesValidas(int p) {
        assertEquals(p, ValidadorTarefa.validarPrioridade(p));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 6, 100})
    void prioridadesInvalidas(int p) {
        assertThrows(EntradaInvalidaException.class, () -> ValidadorTarefa.validarPrioridade(p));
    }

    @Test
    void prioridadeNulaEInvalida() {
        assertThrows(EntradaInvalidaException.class, () -> ValidadorTarefa.validarPrioridade(null));
    }
}
