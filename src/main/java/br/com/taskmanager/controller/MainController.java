package br.com.taskmanager.controller;

import br.com.taskmanager.app.MainApplication;
import br.com.taskmanager.exceptions.EntradaInvalidaException;
import br.com.taskmanager.exceptions.PersistenciaException;
import br.com.taskmanager.model.Tarefa;
import br.com.taskmanager.service.TarefaService;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controller da Tela Principal (main-view.fxml).
 * Recebe o servico pelo construtor (ver controllerFactory em MainApplication).
 */
public class MainController {
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final PseudoClass CONCLUIDA = PseudoClass.getPseudoClass("concluida");

    private final TarefaService servico;

    @FXML private Label lblResumo;
    @FXML private ProgressBar barraProgresso;
    @FXML private TextField txtBusca;
    @FXML private CheckBox chkOcultarConcluidas;
    @FXML private TableView<Tarefa> tabela;
    @FXML private TableColumn<Tarefa, Boolean> colConcluida;
    @FXML private TableColumn<Tarefa, String> colTitulo;
    @FXML private TableColumn<Tarefa, String> colDescricao;
    @FXML private TableColumn<Tarefa, Integer> colPrioridade;
    @FXML private TableColumn<Tarefa, LocalDateTime> colCriacao;
    @FXML private Button btnEditar;
    @FXML private Button btnStatus;
    @FXML private Button btnRemover;
    @FXML private Label lblStatus;

    public MainController(TarefaService servico) {
        this.servico = servico;
    }

    /** Chamado automaticamente pelo FXMLLoader depois de injetar os @FXML. */
    @FXML
    private void initialize() {
        configurarColunas();
        configurarLinhas();

        tabela.setPlaceholder(new Label("Nenhuma tarefa por aqui. Clique em \"+ Nova tarefa\" para começar."));
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antiga, nova) -> atualizarBotoes());
        txtBusca.textProperty().addListener((obs, antigo, novo) -> atualizarTabela());

        atualizarTabela();
        // Foco inicial na tabela, para o texto de ajuda da busca aparecer.
        Platform.runLater(tabela::requestFocus);
    }

    // ------------------------------------------------------------ tabela

    private void configurarColunas() {
        colTitulo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitulo()));
        colDescricao.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescricao()));
        colPrioridade.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrioridade()));
        colCriacao.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getDataCriacao()));
        colConcluida.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().isConcluida()));

        // Coluna com CheckBox: marcar/desmarcar grava direto no banco.
        colConcluida.setCellFactory(coluna -> new TableCell<>() {
            private final CheckBox caixa = new CheckBox();

            {
                caixa.setOnAction(e -> {
                    Tarefa tarefa = getTableRow() == null ? null : getTableRow().getItem();
                    if (tarefa != null) {
                        mudarStatus(tarefa, caixa.isSelected());
                    }
                });
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(Boolean concluida, boolean vazio) {
                super.updateItem(concluida, vazio);
                if (vazio || concluida == null) {
                    setGraphic(null);
                } else {
                    caixa.setSelected(concluida);
                    setGraphic(caixa);
                }
            }
        });

        // Prioridade como "etiqueta" colorida; tarefas normais mostram um traco.
        colPrioridade.setCellFactory(coluna -> new TableCell<>() {
            @Override
            protected void updateItem(Integer prioridade, boolean vazio) {
                super.updateItem(prioridade, vazio);
                getStyleClass().removeAll("p1", "p2", "p3", "p4", "p5", "sem-prioridade");
                if (vazio) {
                    setText(null);
                } else if (prioridade == null) {
                    setText("—");
                    getStyleClass().add("sem-prioridade");
                } else {
                    setText("P" + prioridade + " · " + rotuloPrioridade(prioridade));
                    getStyleClass().add("p" + prioridade);
                }
            }
        });

        colCriacao.setCellFactory(coluna -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime data, boolean vazio) {
                super.updateItem(data, vazio);
                setText(vazio || data == null ? null : FORMATO_DATA.format(data));
            }
        });
    }

    /** Linhas concluidas ficam esmaecidas; duplo clique abre a edicao. */
    private void configurarLinhas() {
        tabela.setRowFactory(t -> {
            TableRow<Tarefa> linha = new TableRow<>() {
                @Override
                protected void updateItem(Tarefa tarefa, boolean vazio) {
                    super.updateItem(tarefa, vazio);
                    pseudoClassStateChanged(CONCLUIDA, !vazio && tarefa != null && tarefa.isConcluida());
                }
            };
            linha.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !linha.isEmpty()) {
                    abrirFormulario(linha.getItem());
                }
            });
            return linha;
        });
    }

    public static String rotuloPrioridade(int prioridade) {
        return switch (prioridade) {
            case 1 -> "Urgente";
            case 2 -> "Alta";
            case 3 -> "Média";
            case 4 -> "Baixa";
            default -> "Mínima";
        };
    }

    /** Recarrega a tabela respeitando busca e filtro, mantendo a selecao. */
    private void atualizarTabela() {
        Tarefa selecionada = tabela.getSelectionModel().getSelectedItem();
        Integer idSelecionado = selecionada == null ? null : selecionada.getId();

        List<Tarefa> visiveis = servico.filtrar(txtBusca.getText(), chkOcultarConcluidas.isSelected());
        tabela.setItems(FXCollections.observableArrayList(visiveis));
        // O TableView reaproveita celulas e pode pular o redesenho quando o
        // valor "parece" igual (mesmo id, ou nulo -> nulo). refresh() forca
        // todas as celulas e linhas a se atualizarem (status, riscado, etc.).
        tabela.refresh();
        if (idSelecionado != null) {
            selecionarPorId(idSelecionado);
        }

        int total = servico.totalTarefas();
        long feitas = servico.totalConcluidas();
        lblResumo.setText(String.format("%d %s · %d %s (%d%%)",
                total, total == 1 ? "tarefa" : "tarefas",
                feitas, feitas == 1 ? "concluída" : "concluídas",
                servico.percentualConcluido()));
        barraProgresso.setProgress(total == 0 ? 0 : (double) feitas / total);
        atualizarBotoes();
    }

    private void selecionarPorId(int id) {
        tabela.getItems().stream()
                .filter(t -> t.getId() == id)
                .findFirst()
                .ifPresent(t -> {
                    tabela.getSelectionModel().select(t);
                    tabela.scrollTo(t);
                });
    }

    private void atualizarBotoes() {
        Tarefa t = tabela.getSelectionModel().getSelectedItem();
        boolean nenhuma = t == null;
        btnEditar.setDisable(nenhuma);
        btnRemover.setDisable(nenhuma);
        btnStatus.setDisable(nenhuma);
        btnStatus.setText(!nenhuma && t.isConcluida() ? "Reabrir" : "Concluir");
    }

    // ------------------------------------------------------------ acoes

    @FXML
    private void filtrar() {
        atualizarTabela();
    }

    @FXML
    private void novaTarefa() {
        abrirFormulario(null);
    }

    @FXML
    private void editarTarefa() {
        Tarefa t = tabela.getSelectionModel().getSelectedItem();
        if (t != null) {
            abrirFormulario(t);
        }
    }

    @FXML
    private void alternarStatus() {
        Tarefa t = tabela.getSelectionModel().getSelectedItem();
        if (t != null) {
            mudarStatus(t, !t.isConcluida());
        }
    }

    private void mudarStatus(Tarefa tarefa, boolean concluida) {
        try {
            servico.definirStatus(tarefa.getId(), concluida);
            mostrarStatus(concluida ? "Tarefa concluída." : "Tarefa reaberta.");
        } catch (EntradaInvalidaException | PersistenciaException ex) {
            mostrarErro("Não foi possível alterar o status", ex.getMessage());
        }
        atualizarTabela();
    }

    /** Confirmacao ao remover (exigida no enunciado). */
    @FXML
    private void removerTarefa() {
        Tarefa t = tabela.getSelectionModel().getSelectedItem();
        if (t == null) {
            return;
        }
        ButtonType remover = new ButtonType("Remover", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION,
                "A tarefa \"" + t.getTitulo() + "\" será removida permanentemente.",
                remover, cancelar);
        alerta.setTitle("Confirmar remoção");
        alerta.getDialogPane().setPrefWidth(440);
        alerta.setHeaderText("Remover esta tarefa?");
        aplicarEstilo(alerta);
        ((Button) alerta.getDialogPane().lookupButton(remover)).getStyleClass().add("botao-perigo");
        ((Button) alerta.getDialogPane().lookupButton(remover)).setDefaultButton(false);
        ((Button) alerta.getDialogPane().lookupButton(cancelar)).setDefaultButton(true);

        Optional<ButtonType> resposta = alerta.showAndWait();
        if (resposta.isPresent() && resposta.get() == remover) {
            try {
                servico.removerTarefa(t.getId());
                mostrarStatus("Tarefa removida: " + t.getTitulo());
            } catch (EntradaInvalidaException | PersistenciaException ex) {
                mostrarErro("Não foi possível remover", ex.getMessage());
            }
            atualizarTabela();
        }
    }

    /** Abre a Tela de Cadastro/Edicao como janela modal. */
    private void abrirFormulario(Tarefa tarefa) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/br/com/taskmanager/fxml/tarefa-form.fxml"));
            Parent raiz = loader.load();
            TarefaFormController form = loader.getController();
            form.configurar(servico, tarefa);

            Stage janela = new Stage();
            janela.initOwner(tabela.getScene().getWindow());
            janela.initModality(Modality.WINDOW_MODAL);
            janela.setTitle(tarefa == null ? "Nova tarefa" : "Editar tarefa");
            janela.setResizable(false);
            Scene cena = new Scene(raiz);
            cena.getStylesheets().add(MainApplication.estilo());
            janela.setScene(cena);
            janela.showAndWait();

            if (form.getTarefaSalva() != null) {
                atualizarTabela();
                selecionarPorId(form.getTarefaSalva().getId());
                mostrarStatus(tarefa == null ? "Tarefa criada." : "Tarefa atualizada.");
            }
        } catch (IOException ex) {
            mostrarErro("Erro ao abrir o formulário", ex.getMessage());
        }
    }

    // ------------------------------------------------------------ mensagens

    private void mostrarStatus(String texto) {
        lblStatus.setText(texto);
    }

    private void mostrarErro(String titulo, String detalhe) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, detalhe, ButtonType.OK);
        alerta.setTitle("Erro");
        alerta.setHeaderText(titulo);
        aplicarEstilo(alerta);
        alerta.showAndWait();
    }

    private void aplicarEstilo(Alert alerta) {
        alerta.initOwner(tabela.getScene().getWindow());
        // Evita que o texto do dialogo seja cortado com reticencias.
        alerta.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alerta.getDialogPane().getStylesheets().add(MainApplication.estilo());
    }
}
