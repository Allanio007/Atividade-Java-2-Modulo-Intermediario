package br.com.taskmanager.controller;

import br.com.taskmanager.exceptions.EntradaInvalidaException;
import br.com.taskmanager.exceptions.PersistenciaException;
import br.com.taskmanager.model.Tarefa;
import br.com.taskmanager.service.TarefaService;
import br.com.taskmanager.util.ValidadorTarefa;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * Controller da Tela de Cadastro/Edicao (tarefa-form.fxml).
 * Se receber uma tarefa, entra em modo edicao; senao, cadastra uma nova.
 */
public class TarefaFormController {
    private static final String CAMPO_INVALIDO = "campo-invalido";

    @FXML private Label lblTituloTela;
    @FXML private TextField txtTitulo;
    @FXML private Label lblContador;
    @FXML private TextArea txtDescricao;
    @FXML private CheckBox chkPrioritaria;
    @FXML private ComboBox<Integer> cmbPrioridade;
    @FXML private Label lblStatusRotulo;
    @FXML private CheckBox chkConcluida;
    @FXML private Label lblErro;

    private TarefaService servico;
    private Tarefa emEdicao;
    private Tarefa tarefaSalva;

    @FXML
    private void initialize() {
        cmbPrioridade.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        cmbPrioridade.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer p) {
                return p == null ? "" : p + " - " + MainController.rotuloPrioridade(p);
            }

            @Override
            public Integer fromString(String texto) {
                return texto == null || texto.isBlank() ? null : Integer.valueOf(texto.substring(0, 1));
            }
        });
        cmbPrioridade.setDisable(true);

        txtTitulo.textProperty().addListener((obs, antigo, novo) -> {
            int n = novo == null ? 0 : novo.length();
            lblContador.setText(n + "/" + ValidadorTarefa.TITULO_MAX);
            lblContador.getStyleClass().remove("contador-excedido");
            if (n > ValidadorTarefa.TITULO_MAX) {
                lblContador.getStyleClass().add("contador-excedido");
            }
            txtTitulo.getStyleClass().remove(CAMPO_INVALIDO);
            if (lblErro.isVisible()) {
                esconderErro();
            }
        });
    }

    /** Recebe o servico e, opcionalmente, a tarefa a editar. */
    public void configurar(TarefaService servico, Tarefa tarefa) {
        this.servico = servico;
        this.emEdicao = tarefa;

        boolean edicao = tarefa != null;
        lblTituloTela.setText(edicao ? "Editar tarefa" : "Nova tarefa");
        // O status so faz sentido na edicao; no cadastro a linha some do layout.
        for (javafx.scene.Node n : new javafx.scene.Node[]{lblStatusRotulo, chkConcluida}) {
            n.setVisible(edicao);
            n.setManaged(edicao);
        }

        if (edicao) {
            txtTitulo.setText(tarefa.getTitulo());
            txtDescricao.setText(tarefa.getDescricao());
            chkConcluida.setSelected(tarefa.isConcluida());
            if (tarefa.getPrioridade() != null) {
                chkPrioritaria.setSelected(true);
                cmbPrioridade.setDisable(false);
                cmbPrioridade.setValue(tarefa.getPrioridade());
            }
        }
    }

    @FXML
    private void alternarPrioridade() {
        boolean prioritaria = chkPrioritaria.isSelected();
        cmbPrioridade.setDisable(!prioritaria);
        if (prioritaria && cmbPrioridade.getValue() == null) {
            cmbPrioridade.setValue(3);
        }
    }

    @FXML
    private void salvarTarefa() {
        esconderErro();
        Integer prioridade = chkPrioritaria.isSelected() ? cmbPrioridade.getValue() : null;
        try {
            if (chkPrioritaria.isSelected() && prioridade == null) {
                throw new EntradaInvalidaException("Escolha a prioridade (1 a 5).");
            }
            if (emEdicao == null) {
                tarefaSalva = servico.criar(txtTitulo.getText(), txtDescricao.getText(), prioridade);
            } else {
                tarefaSalva = servico.editar(emEdicao.getId(), txtTitulo.getText(),
                        txtDescricao.getText(), prioridade, chkConcluida.isSelected());
            }
            fechar();
        } catch (EntradaInvalidaException ex) {
            if (ex.getMessage().contains("título")) {
                txtTitulo.getStyleClass().add(CAMPO_INVALIDO);
                txtTitulo.requestFocus();
            }
            mostrarErro(ex.getMessage());
        } catch (PersistenciaException ex) {
            mostrarErro("Erro ao salvar no banco: " + ex.getMessage());
        }
    }

    @FXML
    private void cancelar() {
        tarefaSalva = null;
        fechar();
    }

    /** Tarefa gravada, ou null se o usuario cancelou. */
    public Tarefa getTarefaSalva() {
        return tarefaSalva;
    }

    private void mostrarErro(String mensagem) {
        lblErro.setText(mensagem);
        lblErro.setManaged(true);
        lblErro.setVisible(true);
        lblErro.getScene().getWindow().sizeToScene();
    }

    private void esconderErro() {
        lblErro.setManaged(false);
        lblErro.setVisible(false);
        if (lblErro.getScene() != null && lblErro.getScene().getWindow() != null) {
            lblErro.getScene().getWindow().sizeToScene();
        }
    }

    private void fechar() {
        ((Stage) txtTitulo.getScene().getWindow()).close();
    }
}
