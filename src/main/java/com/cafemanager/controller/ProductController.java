package com.cafemanager.controller;

import com.cafemanager.model.Category;
import com.cafemanager.model.Product;
import com.cafemanager.service.CatalogService;
import com.cafemanager.ui.Emoji;
import com.cafemanager.ui.Dialogs;
import com.cafemanager.ui.EmojiPicker;
import com.cafemanager.ui.IconBadge;
import com.cafemanager.ui.ProductCard;
import com.cafemanager.ui.Screen;
import com.cafemanager.ui.SwitchControl;
import com.cafemanager.ui.Modal;
import com.cafemanager.ui.Toast;
import com.cafemanager.util.BusinessException;
import com.cafemanager.util.DataAccessException;
import com.cafemanager.util.Money;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Administration des produits : liste filtrable + formulaire (nom, catégorie, prix, icône/image, état). */
public class ProductController implements Screen {

    @FXML private Label countLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<Category> categoryFilter;
    @FXML private TableView<Product> table;
    @FXML private Label formTitle;
    @FXML private TextField nameField;
    @FXML private ComboBox<Category> categoryBox;
    @FXML private TextField priceField;
    @FXML private Label currencyLabel;
    @FXML private TextField iconField;
    @FXML private StackPane emojiHolder;
    @FXML private Button clearImageButton;
    @FXML private Label imageLabel;
    @FXML private SwitchControl activeSwitch;
    @FXML private SwitchControl favoriteSwitch;
    @FXML private StackPane previewHolder;
    @FXML private Button deleteButton;

    private final CatalogService catalog = new CatalogService();
    private List<Product> all = List.of();
    private Product editing;              // null = nouveau produit
    private String imagePath;
    private boolean loading;

    @FXML
    private void initialize() {
        currencyLabel.setText(Money.currency());
        buildColumns();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Aucun produit"));
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (!loading && b != null) {
                edit(b);
            }
        });
        searchField.textProperty().addListener((o, a, b) -> applyFilter());
        Emoji.categoryCells(categoryFilter);
        Emoji.categoryCells(categoryBox);
        categoryFilter.valueProperty().addListener((o, a, b) -> applyFilter());

        emojiHolder.getChildren().add(new EmojiPicker(e -> {
            iconField.setText(e);
            imagePath = null;
            refreshImageLabel();
        }));

        nameField.textProperty().addListener((o, a, b) -> updatePreview());
        priceField.textProperty().addListener((o, a, b) -> updatePreview());
        iconField.textProperty().addListener((o, a, b) -> updatePreview());
    }

    @Override
    public void onShow() {
        currencyLabel.setText(Money.currency());
        loading = true;
        List<Category> cats = catalog.allCategories();
        Category allCats = new Category(0, "Toutes les catégories", "", 0, true);
        categoryFilter.getItems().setAll(allCats);
        categoryFilter.getItems().addAll(cats);
        categoryFilter.setValue(allCats);
        categoryBox.getItems().setAll(cats);
        loading = false;
        reload(null);
        edit(null);
    }

    // ================================================================= tableau

    private void buildColumns() {
        TableColumn<Product, Product> name = new TableColumn<>("Produit");
        name.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        name.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setGraphic(null);
                    return;
                }
                Label n = new Label(p.getName());
                n.setStyle("-fx-font-weight: bold;");
                HBox box = new HBox(12, new IconBadge(p.getIcon(), p.getImagePath(), 36), n);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        name.setPrefWidth(320);

        TableColumn<Product, String> cat = new TableColumn<>("Catégorie");
        cat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategoryName() == null ? "—" : c.getValue().getCategoryName()));
        cat.setPrefWidth(110);

        TableColumn<Product, String> price = new TableColumn<>("Prix");
        price.setCellValueFactory(c -> new SimpleStringProperty(Money.fmt(c.getValue().getPriceCents())));
        price.setPrefWidth(90);

        TableColumn<Product, Product> state = new TableColumn<>("État");
        state.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        state.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setGraphic(null);
                    return;
                }
                Label pill = new Label(p.isActive() ? "Disponible" : "Masqué");
                pill.getStyleClass().addAll("pill", p.isActive() ? "pill-ok" : "pill-off");
                HBox box = new HBox(6, pill);
                if (p.isFavorite()) {
                    Label star = new Label("⭐");
                    star.getStyleClass().add("emoji");
                    box.getChildren().add(star);
                }
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        state.setPrefWidth(130);

        TableColumn<Product, Product> actions = new TableColumn<>("Actions");
        actions.setSortable(false);
        actions.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        actions.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setGraphic(null);
                    return;
                }
                Button toggle = Emoji.button(p.isActive() ? "🚫" : "👁");
                toggle.getStyleClass().addAll("row-btn", "pressable");
                toggle.setTooltip(new Tooltip(p.isActive() ? "Masquer en caisse" : "Rendre disponible"));
                toggle.setOnAction(e -> {
                    catalog.setProductActive(p, !p.isActive());
                    reload(p.getId());
                });
                Button del = Emoji.button("🗑");
                del.getStyleClass().addAll("row-btn", "row-btn-danger", "pressable");
                del.setTooltip(new Tooltip("Supprimer"));
                del.setOnAction(e -> confirmDelete(p));
                HBox box = new HBox(8, toggle, del);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        actions.setPrefWidth(100);
        name.setMinWidth(220);
        fixWidth(cat, 110);
        fixWidth(price, 95);
        fixWidth(state, 150);
        fixWidth(actions, 100);
        table.getColumns().addAll(java.util.List.of(name, cat, price, state, actions));
    }

    /** Colonne à largeur fixe : le nom du produit récupère tout l'espace restant. */
    private static void fixWidth(TableColumn<?, ?> col, double w) {
        col.setMinWidth(w);
        col.setMaxWidth(w);
        col.setPrefWidth(w);
    }

    private void reload(Long selectId) {
        all = catalog.allProducts();
        applyFilter();
        if (selectId != null) {
            table.getItems().stream().filter(p -> p.getId() == selectId).findFirst()
                    .ifPresent(p -> table.getSelectionModel().select(p));
        }
    }

    private void applyFilter() {
        if (loading) {
            return;
        }
        String q = norm(searchField.getText());
        Category cat = categoryFilter.getValue();
        List<Product> list = all.stream()
                .filter(p -> cat == null || cat.getId() == 0 || p.getCategoryId() == cat.getId())
                .filter(p -> q.isEmpty() || norm(p.getName()).contains(q))
                .collect(Collectors.toList());
        loading = true;
        table.getItems().setAll(list);
        loading = false;
        int active = (int) all.stream().filter(Product::isActive).count();
        countLabel.setText(all.size() + " produits · " + active + " disponibles");
    }

    private static String norm(String s) {
        return s == null ? "" : Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    // ================================================================ formulaire

    @FXML
    private void onNew() {
        table.getSelectionModel().clearSelection();
        edit(null);
        nameField.requestFocus();
    }

    private void edit(Product p) {
        editing = p;
        loading = true;
        formTitle.setText(p == null ? "Nouveau produit" : "Modifier le produit");
        nameField.setText(p == null ? "" : p.getName());
        priceField.setText(p == null ? "" : Money.plain(p.getPriceCents()).replace('.', ','));
        iconField.setText(p == null ? "☕" : (p.getIcon() == null ? "" : p.getIcon()));
        imagePath = p == null ? null : p.getImagePath();
        activeSwitch.setSelected(p == null || p.isActive());
        favoriteSwitch.setSelected(p != null && p.isFavorite());
        Category cat = null;
        if (p != null) {
            cat = categoryBox.getItems().stream().filter(c -> c.getId() == p.getCategoryId()).findFirst().orElse(null);
        } else if (categoryFilter.getValue() != null && categoryFilter.getValue().getId() != 0) {
            cat = categoryBox.getItems().stream().filter(c -> c.getId() == categoryFilter.getValue().getId()).findFirst().orElse(null);
        }
        categoryBox.setValue(cat);
        deleteButton.setVisible(p != null);
        deleteButton.setManaged(p != null);
        loading = false;
        refreshImageLabel();
        updatePreview();
    }

    @FXML
    private void onToggleEmoji() {
        boolean show = !emojiHolder.isVisible();
        emojiHolder.setVisible(show);
        emojiHolder.setManaged(show);
    }

    @FXML
    private void onPickImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image pour le produit");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        File f = fc.showOpenDialog(Modal.window());
        if (f != null) {
            try {
                imagePath = catalog.importImage(f);
                refreshImageLabel();
                updatePreview();
            } catch (BusinessException e) {
                Toast.warning(e.getMessage());
            }
        }
    }

    @FXML
    private void onClearImage() {
        imagePath = null;
        refreshImageLabel();
        updatePreview();
    }

    private void refreshImageLabel() {
        boolean has = imagePath != null && !imagePath.isBlank();
        imageLabel.setText(has ? "Image : " + new File(imagePath).getName() : "Aucune image : l'icône sera utilisée.");
        clearImageButton.setDisable(!has);
    }

    private void updatePreview() {
        if (loading) {
            return;
        }
        Product preview = new Product();
        preview.setName(nameField.getText() == null || nameField.getText().isBlank() ? "Nom du produit" : nameField.getText());
        preview.setIcon(iconField.getText());
        preview.setImagePath(imagePath);
        try {
            preview.setPriceCents(Money.parse(priceField.getText()));
        } catch (RuntimeException e) {
            preview.setPriceCents(0);
        }
        ProductCard card = new ProductCard(preview, p -> { });
        card.setPrefSize(168, 168);
        card.setMinSize(168, 168);
        card.setMaxSize(168, 168);
        previewHolder.getChildren().setAll(card);
    }

    @FXML
    private void onSave() {
        Product p = editing == null ? new Product() : editing;
        Category cat = categoryBox.getValue();
        long price;
        try {
            price = Money.parse(priceField.getText());
        } catch (RuntimeException e) {
            Toast.warning("Prix invalide : saisissez un montant (ex. 8 ou 12,50).");
            return;
        }
        p.setName(nameField.getText());
        p.setCategoryId(cat == null ? 0 : cat.getId());
        p.setPriceCents(price);
        p.setIcon(iconField.getText() == null || iconField.getText().isBlank() ? "☕" : iconField.getText().trim());
        p.setImagePath(imagePath);
        p.setActive(activeSwitch.isSelected());
        p.setFavorite(favoriteSwitch.isSelected());
        try {
            Product saved = catalog.saveProduct(p);
            Toast.success("Produit « " + saved.getName() + " » enregistré");
            all = catalog.allProducts();
            applyFilter();
            loading = true;
            table.getItems().stream().filter(x -> x.getId() == saved.getId()).findFirst()
                    .ifPresent(x -> table.getSelectionModel().select(x));
            loading = false;
            edit(all.stream().filter(x -> x.getId() == saved.getId()).findFirst().orElse(saved));
        } catch (BusinessException e) {
            Toast.warning(e.getMessage());
        } catch (DataAccessException e) {
            Toast.error(e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        if (editing != null) {
            confirmDelete(editing);
        }
    }

    private void confirmDelete(Product p) {
        Dialogs.confirm("Supprimer le produit",
                "« " + p.getName() + " » sera supprimé du catalogue. Les anciens tickets restent inchangés.",
                "SUPPRIMER", true, () -> {
                    try {
                        catalog.deleteProduct(p);
                        Toast.info("Produit supprimé");
                        reload(null);
                        edit(null);
                    } catch (DataAccessException e) {
                        Toast.error(e.getMessage());
                    }
                });
    }
}
