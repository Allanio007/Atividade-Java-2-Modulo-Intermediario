package br.com.taskmanager.app;

import br.com.taskmanager.controller.MainController;
import br.com.taskmanager.dao.Conexao;
import br.com.taskmanager.dao.TarefaDAO;
import br.com.taskmanager.service.TarefaService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Ponto de entrada JavaFX: substitui o menu de console (classe Main da
 * primeira entrega) por uma interface grafica.
 */
public class MainApplication extends Application {

    /** Permite apontar para outro arquivo: -Dtaskmanager.db=jdbc:sqlite:outro.db */
    private static final String PROPRIEDADE_URL = "taskmanager.db";

    private Conexao conexao;

    @Override
    public void start(Stage stage) throws Exception {
        conexao = new Conexao(System.getProperty(PROPRIEDADE_URL, Conexao.URL_PADRAO));
        TarefaService servico = new TarefaService(new TarefaDAO(conexao));

        FXMLLoader loader = new FXMLLoader(
                MainApplication.class.getResource("/br/com/taskmanager/fxml/main-view.fxml"));
        // Injeta o servico no controller criado pelo FXMLLoader.
        loader.setControllerFactory(tipo -> {
            if (tipo == MainController.class) {
                return new MainController(servico);
            }
            throw new IllegalStateException("Controller desconhecido: " + tipo);
        });
        Parent raiz = loader.load();

        Scene cena = new Scene(raiz, 980, 620);
        cena.getStylesheets().add(estilo());

        stage.setTitle("TaskManager - Gerenciador de Tarefas");
        stage.setMinWidth(760);
        stage.setMinHeight(480);
        stage.setScene(cena);
        stage.show();
    }

    /** Caminho do .css compartilhado pelas telas. */
    public static String estilo() {
        return MainApplication.class.getResource("/br/com/taskmanager/css/style.css").toExternalForm();
    }

    @Override
    public void stop() {
        if (conexao != null) {
            conexao.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
