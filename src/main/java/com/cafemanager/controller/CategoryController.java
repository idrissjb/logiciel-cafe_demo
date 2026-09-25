package com.cafemanager.controller;

import com.cafemanager.model.Category;
import com.cafemanager.service.CatalogService;
import com.cafemanager.ui.Emoji;
import com.cafemanager.ui.Dialogs;
import com.cafemanager.ui.EmojiPicker;
import com.cafemanager.ui.Screen;
import com.cafemanager.ui.SwitchControl;
import com.cafemanager.ui.Toast;
import com.cafemanager.util.BusinessException;
import com.cafemanager.util.DataAccessException;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.util.List;

/** Administration des catégories : liste triée par ordre d'affichage + formulaire. */
public class CategoryController implements Screen {

    @FXML private Label countLabel;
    @FXML private TableView<Category> table;
    @FXML private Label formTitle;
    @FXML private TextField nameField;
    @FXML private TextField iconField;
    @FXML private StackPane emojiHolder;
    @FXML private Spinner<Integer> orderSpinner;
    @FXML private SwitchControl activeSwitch;
    @FXML private Button deleteButton;

    private final CatalogService catalog = new CatalogService();
    private Category editing;
    private boolean loading;

    @FXML
    private void initialize() {
        orderSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
        // Valide la saisie manuelle dès la perte du focus
        orderSpinner.focusedProperty().addListener((o, was, is) -> {
            if (!is) {
                try {
                    orderSpinner.getValueFactory().setValue(Integer.parseInt(orderSpinner.getEditor().getText().trim()));
                } catch (NumberFormatException e) {
                    orderSpinner.getEditor().setText(String.valueOf(orderSpinner.getValue()));
                }
            }
        });
        buildColumns();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Aucune catégorie"));
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (!loading && b != null) {
                edit(b);
            }
        });
        emojiHolder.getChildren().add(new EmojiPicker(e -> iconField.setText(e)));
    }

    @Override
    public void onShow() {
        reload(null);
        edit(null);
    }

    private void buildColumns() {
        TableColumn<Category, String> icon = new TableColumn<>("Icône");
        icon.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getIcon()));
        icon.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                Label l = new Label(s == null ? "" : s);
                l.getStyleClass().add("emoji");
                l.setStyle("-fx-font-size: 22px;");
                setGraphic(l);
            }
        });
        icon.setPrefWidth(70);

        TableColumn<Category, String> name = new TableColumn<>("Nom");
        name.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        name.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty ? null : s);
                setStyle(empty ? "" : "-fx-font-weight: bold;");
            }
        });
        name.setPrefWidth(240);

        TableColumn<Category, Number> order = new TableColumn<>("Ordre");
        order.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getDisplayOrder()));
        order.setPrefWidth(80);

        TableColumn<Category, Category> state = new TableColumn<>("État");
        state.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        state.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Category cat, boolean empty) {
                super.updateItem(cat, empty);
                if (empty || cat == null) {
                    setGraphic(null);
                    return;
                }
                Label pill = new Label(cat.isActive() ? "Active" : "Inactive");
                pill.getStyleClass().addAll("pill", cat.isActive() ? "pill-ok" : "pill-off");
                setGraphic(pill);
            }
        });
        state.setPrefWidth(110);

        TableColumn<Category, Category> actions = new TableColumn<>("Actions");
        actions.setSortable(false);
        actions.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        actions.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Category cat, boolean empty) {
                super.updateItem(cat, empty);
                if (empty || cat == null) {
                    setGraphic(null);
                    return;
                }
                Button up = new Button("▲");
                up.getStyleClass().addAll("row-btn", "pressable");
                up.setTooltip(new Tooltip("Monter"));
                up.setOnAction(e -> move(cat, -1));
                Button down = new Button("▼");
                down.getStyleClass().addAll("row-btn", "pressable");
                down.setTooltip(new Tooltip("Descendre"));
                down.setOnAction(e -> move(cat, +1));
                Button del = Emoji.button("🗑");
                del.getStyleClass().addAll("row-btn", "row-btn-danger", "pressable");
                del.setTooltip(new Tooltip("Supprimer"));
                del.setOnAction(e -> confirmDelete(cat));
                HBox box = new HBox(8, up, down, del);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        actions.setPrefWidth(160);
        table.getColumns().addAll(List.of(icon, name, order, state, actions));
    }

    private void reload(Long selectId) {
        loading = true;
        List<Category> list = catalog.allCategories();
        table.getItems().setAll(list);
        long active = list.stream().filter(Category::isActive).count();
        countLabel.setText(list.size() + " catégories · " + active + " actives");
        if (selectId != null) {
            list.stream().filter(c -> c.getId() == selectId).findFirst()
                    .ifPresent(c -> table.getSelectionModel().select(c));
        }
        loading = false;
    }

    /** Échange l'ordre d'affichage avec la catégorie voisine et renumérote proprement de 1 à n. */
    private void move(Category cat, int delta) {
        List<Category> list = new java.util.ArrayList<>(catalog.allCategories());
        int i = -1;
        for (int k = 0; k < list.size(); k++) {
            if (list.get(k).getId() == cat.getId()) {
                i = k;
            }
        }
        int j = i + delta;
        if (i < 0 || j < 0 || j >= list.size()) {
            return;
        }
        java.util.Collections.swap(list, i, j);
        try {
            for (int k = 0; k < list.size(); k++) {
                Category c = list.get(k);
                if (c.getDisplayOrder() != k + 1) {
                    c.setDisplayOrder(k + 1);
                    catalog.saveCategory(c);
                }
            }
            reload(editing == null ? null : editing.getId());
            if (editing != null) {
                orderSpinner.getValueFactory().setValue(
                        table.getItems().stream().filter(c -> c.getId() == editing.getId()).findFirst()
                                .map(Category::getDisplayOrder).orElse(1));
            }
        } catch (BusinessException | DataAccessException e) {
            Toast.error(e.getMessage());
        }
    }

    // ================================================================ formulaire

    @FXML
    private void onNew() {
        table.getSelectionModel().clearSelection();
        edit(null);
        nameField.requestFocus();
    }

    private void edit(Category c) {
        editing = c;
        formTitle.setText(c == null ? "Nouvelle catégorie" : "Modifier la catégorie");
        nameField.setText(c == null ? "" : c.getName());
        iconField.setText(c == null ? "🍽" : (c.getIcon() == null ? "" : c.getIcon()));
        int next = table.getItems().stream().mapToInt(Category::getDisplayOrder).max().orElse(0) + 1;
        orderSpinner.getValueFactory().setValue(c == null ? next : Math.max(1, c.getDisplayOrder()));
        activeSwitch.setSelected(c == null || c.isActive());
        deleteButton.setVisible(c != null);
        deleteButton.setManaged(c != null);
    }

    @FXML
    private void onToggleEmoji() {
        boolean show = !emojiHolder.isVisible();
        emojiHolder.setVisible(show);
        emojiHolder.setManaged(show);
    }

    @FXML
    private void onSave() {
        orderSpinner.getValueFactory().setValue(parseOrder());
        Category c = editing == null ? new Category() : editing;
        c.setName(nameField.getText());
        c.setIcon(iconField.getText() == null || iconField.getText().isBlank() ? "🍽" : iconField.getText().trim());
        c.setDisplayOrder(orderSpinner.getValue());
        c.setActive(activeSwitch.isSelected());
        try {
            Category saved = catalog.saveCategory(c);
            Toast.success("Catégorie « " + saved.getName() + " » enregistrée");
            reload(saved.getId());
            edit(table.getItems().stream().filter(x -> x.getId() == saved.getId()).findFirst().orElse(saved));
        } catch (BusinessException e) {
            Toast.warning(e.getMessage());
        } catch (DataAccessException e) {
            Toast.error(e.getMessage());
        }
    }

    private int parseOrder() {
        try {
            return Math.max(1, Integer.parseInt(orderSpinner.getEditor().getText().trim()));
        } catch (NumberFormatException e) {
            return orderSpinner.getValue() == null ? 1 : orderSpinner.getValue();
        }
    }

    @FXML
    private void onDelete() {
        if (editing != null) {
            confirmDelete(editing);
        }
    }

    private void confirmDelete(Category c) {
        Dialogs.confirm("Supprimer la catégorie",
                "La catégorie « " + c.getName() + " » sera supprimée définitivement.",
                "SUPPRIMER", true, () -> {
                    try {
                        catalog.deleteCategory(c);
                        Toast.info("Catégorie supprimée");
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
