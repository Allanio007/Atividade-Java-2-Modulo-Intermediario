# Projeto 1 - Entrega do Modulo Intermediario (Java)

**Curso:** Capacita iRede - Residencia em TIC 20
**Tarefa:** Unidade 5, Capitulo 1 - Desenvolvimento do Projeto (Intermediario)
**Professor:** Alberson Dantas
**Projeto:** Projeto 1 - TaskManager (continuacao da 1a entrega)

## Descricao

Esta entrega evolui o TaskManager da versao console (1a entrega, repositorio
https://github.com/Allanio007/Projeto-Capacita-Java) para uma aplicacao desktop com
interface grafica.

O sistema permite criar, listar, editar, concluir/reabrir e remover tarefas. As tarefas ficam
gravadas em um banco de dados relacional (SQLite) e continuam disponiveis depois que a
aplicacao e fechada.

O modelo da 1a entrega foi reaproveitado: `ITarefa`, `BaseTarefa`, `Tarefa` e
`TarefaPrioritaria`, com heranca e polimorfismo. A classe `TaskManager` evoluiu para o
`TarefaService`.

## Checklist dos requisitos

| Requisito do enunciado | Onde esta |
|---|---|
| Classes e metodos genericos | `generics/ListaGenerica<T>`, `generics/GenericDAO<T,ID>`, `generics/UtilitariosGenericos` |
| Curingas `?`, `? extends T`, `? super T` | `ListaGenerica.adicionarTodos`, `copiarPara`, `filtrar`; `UtilitariosGenericos.contar`, `contarConcluidas`, `copiar` |
| `MainApplication extends Application` + `start(Stage)` | `app/MainApplication.java` |
| Tela principal (marcar, remover, editar) | `fxml/main-view.fxml` + `controller/MainController.java` |
| Tela de cadastro/edicao | `fxml/tarefa-form.fxml` + `controller/TarefaFormController.java` |
| Confirmacao ao remover | `MainController.removerTarefa()` (Alert de confirmacao) |
| Label, TextField, TextArea, Button, TableView, CheckBox | nas duas telas FXML |
| `setOnAction()` | CheckBox da tabela (`MainController.configurarColunas`) e `onAction` nos FXML |
| VBox, HBox, GridPane, BorderPane | `main-view.fxml` (BorderPane/VBox/HBox) e `tarefa-form.fxml` (VBox/GridPane/HBox) |
| Separacao logica/visual com `.fxml` e `@FXML` | pasta `resources/.../fxml` + controllers |
| Estilizacao com `.css` | `resources/.../css/style.css` |
| Banco relacional (SQLite) com tabela `tarefas` | `dao/Conexao.java` (`CREATE TABLE IF NOT EXISTS`) |
| Conexao com `DriverManager` | `Conexao.getConnection()` |
| CRUD com `PreparedStatement` e `ResultSet` | `dao/TarefaDAO.java` |
| `commit()` e `rollback()` | `TarefaDAO.emTransacao()` (toda escrita) e `inserirTodos()` (lote tudo-ou-nada) |
| Evitar SQL Injection | somente parametros `?`; teste `TarefaDAOTest.sqlInjectionNaoFunciona` |
| `TarefaDAO` isolando a persistencia | `dao/TarefaDAO.java` |
| Conexao gerenciada via `Conexao.java` | `dao/Conexao.java` |
| Teste de criacao de tarefa | `TarefaTest.deveCriarTarefaEmAberto`, `TarefaServiceTest.criarTarefaPersisteNoBanco` |
| Teste de conclusao de tarefa | `TarefaTest.deveConcluirEReabrir`, `TarefaServiceTest.concluirTarefa` |
| Teste do DAO com SQLite em memoria | `TarefaDAOTest` (17 testes, `jdbc:sqlite::memory:`) |
| Teste de validacao de dados | `ValidadorTarefaTest` (19 testes), `TarefaServiceTest.dadosInvalidosNaoSaoGravados` |
| `@Test`, `@BeforeEach`, `@AfterEach` | todas as classes de teste |
| Testes em pacotes separados | `src/test/java/br/com/taskmanager/{model,util,generics,dao,service}` |

**Resultado dos testes:** 65 testes, 65 aprovados.

## Como executar

Requisitos: Java 17 ou superior e Maven 3.8 ou superior.

```bash
mvn clean test      # roda os testes
mvn javafx:run      # abre a aplicacao
```

Pela IDE, execute a classe `br.com.taskmanager.app.Launcher`.

## Estrutura da entrega

- `pom.xml`: build e dependencias (JavaFX 21, sqlite-jdbc, JUnit 5).
- `src/main/`: codigo-fonte, telas FXML e CSS.
- `src/test/`: testes JUnit.
- `docs/telas/`: capturas das telas.
- `README.md`: documentacao completa (arquitetura e cada requisito explicado).

## Proxima etapa

A 3a entrega (modulo avancado) partira desta versao.
