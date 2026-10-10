package br.com.taskmanager.app;

/**
 * Classe auxiliar para executar pela IDE (botao Run) sem configurar
 * --module-path: como ela nao estende Application, a JVM nao exige que o
 * JavaFX esteja no module path.
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        MainApplication.main(args);
    }
}
