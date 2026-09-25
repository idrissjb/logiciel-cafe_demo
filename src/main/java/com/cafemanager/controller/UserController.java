package com.cafemanager.controller;

import com.cafemanager.model.Role;
import com.cafemanager.model.User;
import com.cafemanager.service.UserService;
import com.cafemanager.ui.Emoji;
import com.cafemanager.ui.Dialogs;
import com.cafemanager.ui.Screen;
import com.cafemanager.ui.SwitchControl;
import com.cafemanager.ui.Toast;
import com.cafemanager.util.BusinessException;
import com.cafemanager.util.DataAccessException;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import java.util.List;

/** Administration des comptes : administrateur, caissier, serveur. */
public class UserController implements Screen {

    @FXML private Label countLabel;
    @FXML private TableView<User> table;
    @FXML private Label formTitle;
    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private ComboBox<Role> roleBox;
    @FXML private Label roleHint;
    @FXML private Label passwordLabel;
    @FXML private PasswordField passwordField;
    @FXML private Label passwordHint;
    @FXML private SwitchControl activeSwitch;
    @FXML private Button deleteButton;

    private final UserService users = new UserService();
    private User editing;
    private boolean loading;

    @FXML
    private void initialize() {
        roleBox.getItems().setAll(Role.values());
        roleBox.valueProperty().addListener((o, a, b) -> roleHint.setText(describe(b)));
        buildColumns();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Aucun utilisateur"));
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (!loading && b != null) {
                edit(b);
            }
        });
    }

    @Override
    public void onShow() {
        reload(null);
        edit(null);
    }

    private static String describe(Role r) {
        if (r == null) {
            return "";
        }
        return switch (r) {
            case ADMINISTRATEUR -> "Accès complet : caisse, historique, dashboard, catalogue, utilisateurs et paramètres.";
            case CAISSIER -> "Caisse, tables, encaissement et historique des tickets (réimpression).";
            case SERVEUR -> "Prise de commande et tables. L'encaissement est réservé, sauf réglage contraire dans les paramètres.";
        };
    }

    private void buildColumns() {
        TableColumn<User, User> name = new TableColumn<>("Nom");
        name.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        name.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) {
                    setGraphic(null);
                    return;
                }
                Label n = new Label(u.getFullName());
                n.setStyle("-fx-font-weight: bold;");
                Label sub = new Label("@" + u.getUsername());
                sub.getStyleClass().add("form-hint");
                javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(1, n, sub);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        name.setPrefWidth(240);

        TableColumn<User, User> role = new TableColumn<>("Rôle");
        role.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        role.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) {
                    setGraphic(null);
                    return;
                }
                Label pill = new Label(u.getRole().getLabel());
                pill.getStyleClass().addAll("pill", u.getRole().isAdmin() ? "pill-accent"
                        : u.getRole() == Role.CAISSIER ? "pill-info" : "pill-warn");
                setGraphic(pill);
            }
        });
        role.setPrefWidth(150);

        TableColumn<User, User> state = new TableColumn<>("État");
        state.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        state.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) {
                    setGraphic(null);
                    return;
                }
                Label pill = new Label(u.isActive() ? "Actif" : "Désactivé");
                pill.getStyleClass().addAll("pill", u.isActive() ? "pill-ok" : "pill-off");
                setGraphic(pill);
            }
        });
        state.setPrefWidth(110);

        TableColumn<User, User> actions = new TableColumn<>("Actions");
        actions.setSortable(false);
        actions.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        actions.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) {
                    setGraphic(null);
                    return;
                }
                Button del = Emoji.button("🗑");
                del.getStyleClass().addAll("row-btn", "row-btn-danger", "pressable");
                del.setTooltip(new Tooltip("Supprimer"));
                del.setOnAction(e -> confirmDelete(u));
                HBox box = new HBox(8, del);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        actions.setPrefWidth(90);
        table.getColumns().addAll(List.of(name, role, state, actions));
    }

    private void reload(Long selectId) {
        loading = true;
        List<User> list = users.findAll();
        table.getItems().setAll(list);
        long active = list.stream().filter(User::isActive).count();
        countLabel.setText(list.size() + " comptes · " + active + " actifs");
        if (selectId != null) {
            list.stream().filter(u -> u.getId() == selectId).findFirst()
                    .ifPresent(u -> table.getSelectionModel().select(u));
        }
        loading = false;
    }

    @FXML
    private void onNew() {
        table.getSelectionModel().clearSelection();
        edit(null);
        usernameField.requestFocus();
    }

    private void edit(User u) {
        editing = u;
        formTitle.setText(u == null ? "Nouvel utilisateur" : "Modifier l'utilisateur");
        usernameField.setText(u == null ? "" : u.getUsername());
        fullNameField.setText(u == null ? "" : u.getFullName());
        roleBox.setValue(u == null ? Role.SERVEUR : u.getRole());
        passwordField.clear();
        passwordLabel.setText(u == null ? "MOT DE PASSE" : "NOUVEAU MOT DE PASSE");
        passwordHint.setText(u == null ? "Au moins 4 caractères." : "Laissez vide pour conserver le mot de passe actuel.");
        activeSwitch.setSelected(u == null || u.isActive());
        deleteButton.setVisible(u != null);
        deleteButton.setManaged(u != null);
    }

    @FXML
    private void onSave() {
        User u = editing == null ? new User() : copy(editing);
        u.setUsername(usernameField.getText());
        u.setFullName(fullNameField.getText());
        u.setRole(roleBox.getValue() == null ? Role.SERVEUR : roleBox.getValue());
        u.setActive(activeSwitch.isSelected());
        try {
            User saved = users.save(u, passwordField.getText());
            Toast.success("Utilisateur « " + saved.getFullName() + " » enregistré");
            reload(saved.getId());
            edit(table.getItems().stream().filter(x -> x.getId() == saved.getId()).findFirst().orElse(saved));
        } catch (BusinessException e) {
            Toast.warning(e.getMessage());
        } catch (DataAccessException e) {
            Toast.error(e.getMessage());
        }
    }

    /** Copie de travail : l'objet du tableau n'est modifié qu'après une sauvegarde réussie. */
    private static User copy(User s) {
        User u = new User();
        u.setId(s.getId());
        u.setUsername(s.getUsername());
        u.setFullName(s.getFullName());
        u.setRole(s.getRole());
        u.setActive(s.isActive());
        u.setPasswordHash(s.getPasswordHash());
        u.setSalt(s.getSalt());
        return u;
    }

    @FXML
    private void onDelete() {
        if (editing != null) {
            confirmDelete(editing);
        }
    }

    private void confirmDelete(User u) {
        Dialogs.confirm("Supprimer l'utilisateur",
                "Le compte « " + u.getFullName() + " » sera supprimé. Ses anciens tickets sont conservés.",
                "SUPPRIMER", true, () -> {
                    try {
                        users.delete(u);
                        Toast.info("Utilisateur supprimé");
                        reload(null);
                        edit(null);
                    } catch (BusinessException e) {
                        Toast.warning(e.getMessage());
                    } catch (DataAccessException e) {
                        Toast.error(e.getMessage());
                    }
                });
    }
}
