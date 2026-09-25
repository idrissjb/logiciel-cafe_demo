package com.cafemanager.controller;

import com.cafemanager.model.CafeTable;
import com.cafemanager.model.Category;
import com.cafemanager.model.Order;
import com.cafemanager.model.OrderItem;
import com.cafemanager.model.Payment;
import com.cafemanager.model.PaymentMethod;
import com.cafemanager.model.Product;
import com.cafemanager.service.CatalogService;
import com.cafemanager.service.OrderService;
import com.cafemanager.service.PaymentService;
import com.cafemanager.service.SettingsService;
import com.cafemanager.service.TableService;
import com.cafemanager.ui.Emoji;
import com.cafemanager.ui.Dialogs;
import com.cafemanager.ui.DiscountDialog;
import com.cafemanager.ui.PaymentDialog;
import com.cafemanager.ui.ProductGrid;
import com.cafemanager.ui.Screen;
import com.cafemanager.ui.TablePickerDialog;
import com.cafemanager.ui.TicketActions;
import com.cafemanager.ui.TicketPanel;
import com.cafemanager.ui.Toast;
import com.cafemanager.util.BusinessException;
import com.cafemanager.util.DataAccessException;
import com.cafemanager.util.Money;
import com.cafemanager.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Écran de caisse : catégories, recherche, grille de produits (un clic = ajout au ticket) et ticket dynamique.
 * La logique métier est déléguée aux services ; ce contrôleur fait le lien entre l'interface et eux.
 */
public class POSController implements Screen {

    @FXML private TextField searchField;
    @FXML private Button clearSearch;
    @FXML private ScrollPane categoryScroll;
    @FXML private HBox categoryBar;
    @FXML private StackPane gridHolder;
    @FXML private StackPane ticketHolder;

    private final CatalogService catalog = new CatalogService();
    private final OrderService orders = new OrderService();
    private final PaymentService payments = new PaymentService();
    private final TableService tables = new TableService();

    private final TicketPanel ticket = new TicketPanel();
    private ProductGrid grid;
    private final ToggleGroup categoryGroup = new ToggleGroup();

    private List<Product> allProducts = List.of();
    private Category selectedCategory;
    private Order order;
    private boolean rebuildingCategories;

    // ================================================================== init

    @FXML
    private void initialize() {
        grid = new ProductGrid(this::addProduct);
        gridHolder.getChildren().add(grid);
        ticketHolder.getChildren().add(ticket);

        ticket.onPlus = i -> changeQuantity(i, 1);
        ticket.onMinus = i -> changeQuantity(i, -1);
        ticket.onRemove = this::removeItem;
        ticket.onPay = this::pay;
        ticket.onQuickPay = this::quickPay;
        ticket.onPrint = this::print;
        ticket.onDiscount = this::discount;
        ticket.onCancel = this::cancelOrder;
        ticket.onPickTable = this::pickTable;
        ticket.onPreview = this::preview;

        categoryGroup.selectedToggleProperty().addListener((o, a, b) -> {
            if (rebuildingCategories) {
                return;
            }
            if (b == null) {
                if (a != null) {
                    a.setSelected(true);          // toujours une catégorie active
                }
                return;
            }
            selectedCategory = (Category) b.getUserData();
            applyFilter();
        });

        searchField.textProperty().addListener((o, a, b) -> {
            clearSearch.setVisible(b != null && !b.isEmpty());
            applyFilter();
        });
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE && !searchField.getText().isEmpty()) {
                searchField.clear();
                e.consume();
            }
        });

        // La molette fait défiler la barre de catégories horizontalement
        categoryScroll.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.getDeltaY() != 0) {
                categoryScroll.setHvalue(categoryScroll.getHvalue() - e.getDeltaY() / categoryBar.getWidth());
                e.consume();
            }
        });
    }

    @Override
    public void onShow() {
        try {
            allProducts = catalog.availableProducts();
            buildCategories();
            order = SessionManager.get().getDraft();
            if (order == null) {
                order = orders.newDraft();
                SessionManager.get().setDraft(order);
            }
            ticket.setPaymentAllowed(payments.canPay());
            ticket.setOrder(order);
            applyFilter();
        } catch (DataAccessException e) {
            Toast.error(e.getMessage());
        }
    }

    // ================================================================ catégories & filtre

    private void buildCategories() {
        Long previous = selectedCategory == null ? null : selectedCategory.getId();
        rebuildingCategories = true;
        categoryBar.getChildren().clear();
        categoryGroup.getToggles().clear();

        List<Category> cats = catalog.activeCategories().stream()
                .filter(c -> allProducts.stream().anyMatch(p -> p.getCategoryId() == c.getId()))
                .collect(Collectors.toList());
        cats.add(Category.favorites());

        ToggleButton first = null;
        ToggleButton restore = null;
        for (Category c : cats) {
            ToggleButton b = new ToggleButton();
            Emoji.beside(b, c.getIcon(), c.getName());
            b.getStyleClass().addAll("category-chip", "pressable");
            b.setToggleGroup(categoryGroup);
            b.setUserData(c);
            b.setFocusTraversable(false);
            categoryBar.getChildren().add(b);
            if (first == null) {
                first = b;
            }
            if (previous != null && c.getId() == previous) {
                restore = b;
            }
        }
        ToggleButton toSelect = restore != null ? restore : first;
        if (toSelect != null) {
            toSelect.setSelected(true);
            selectedCategory = (Category) toSelect.getUserData();
        }
        rebuildingCategories = false;
    }

    private void applyFilter() {
        if (grid == null) {
            return;
        }
        String q = normalize(searchField.getText());
        List<Product> list;
        if (!q.isEmpty()) {
            list = allProducts.stream().filter(p -> normalize(p.getName()).contains(q)).collect(Collectors.toList());
        } else if (selectedCategory == null) {
            list = allProducts;
        } else if (selectedCategory.isFavorites()) {
            list = allProducts.stream().filter(Product::isFavorite).collect(Collectors.toList());
        } else {
            list = allProducts.stream().filter(p -> p.getCategoryId() == selectedCategory.getId())
                    .collect(Collectors.toList());
        }
        grid.setProducts(list);
    }

    /** "Cappuccino" et "capp" ou "CAPP" ou "cäpp" se correspondent : minuscules, sans accents. */
    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    @FXML
    private void onClearSearch() {
        searchField.clear();
    }

    // ================================================================ édition du ticket

    private void addProduct(Product p) {
        run(() -> {
            OrderItem item = order.addProduct(p);
            orders.onChanged(order);
            ticket.itemAdded(item);
            ticket.refreshTotals(false);
        });
    }

    private void changeQuantity(OrderItem item, int delta) {
        run(() -> {
            boolean stillThere = order.changeQuantity(item, delta);
            orders.onChanged(order);
            if (stillThere) {
                ticket.itemChanged(item);
            } else {
                ticket.itemRemoved(item);
            }
        });
    }

    private void removeItem(OrderItem item) {
        run(() -> {
            order.removeItem(item);
            orders.onChanged(order);
            ticket.itemRemoved(item);
        });
    }

    /** Suppr : supprime la ligne sélectionnée. */
    public void removeSelected() {
        OrderItem it = ticket.selectedItem();
        if (it != null) {
            removeItem(it);
        }
    }

    /** + / - : quantité de la ligne sélectionnée. */
    public void changeSelectedQuantity(int delta) {
        OrderItem it = ticket.selectedItem();
        if (it != null) {
            changeQuantity(it, delta);
        }
    }

    // ================================================================ actions du ticket

    /** F2 : nouvelle commande. */
    public void newOrder() {
        if (order.isEmpty()) {
            Toast.info("La commande en cours est déjà vide");
            return;
        }
        if (order.getTableId() != null) {
            // Commande liée à une table : elle est déjà enregistrée, on la laisse en cours et on repart à neuf.
            String table = order.getTableName();
            startFreshOrder();
            Toast.success("Commande conservée sur « " + table + " » — nouvelle commande prête");
        } else {
            Dialogs.confirm("Nouvelle commande",
                    "Le ticket en cours (sans table) sera abandonné.", "ABANDONNER LE TICKET", true, this::startFreshOrder);
        }
    }

    private void startFreshOrder() {
        order = orders.newDraft();
        SessionManager.get().setDraft(order);
        ticket.setOrder(order);
    }

    /** Bouton ANNULER : annule la commande après confirmation. */
    private void cancelOrder() {
        String detail = order.isPersisted()
                ? "Le ticket " + order.getTicketNumber() + " sera annulé" + (order.getTableName() != null ? " et « " + order.getTableName() + " » sera libérée." : ".")
                : "Tous les articles du ticket en cours seront retirés.";
        Dialogs.confirm("Annuler la commande", detail, "ANNULER LA COMMANDE", true, () -> run(() -> {
            orders.cancel(order);
            startFreshOrder();
            Toast.info("Commande annulée");
        }));
    }

    private void discount() {
        if (order.isEmpty()) {
            return;
        }
        DiscountDialog.show(order, () -> run(() -> {
            orders.onChanged(order);
            ticket.refreshTotals(true);
        }));
    }

    /** F4 : paiement. */
    public void pay() {
        if (!payments.canPay()) {
            Toast.warning("L'encaissement est réservé au caissier et à l'administrateur");
            return;
        }
        if (order.isEmpty()) {
            Toast.info("Ajoutez d'abord des produits au ticket");
            return;
        }
        PaymentDialog.show(order, this::afterPaid);
    }

    /** ENCAISSER : espèces, montant exact, sans fenêtre. */
    private void quickPay() {
        if (!payments.canPay()) {
            Toast.warning("L'encaissement est réservé au caissier et à l'administrateur");
            return;
        }
        if (order.isEmpty()) {
            return;
        }
        run(() -> {
            Order paid = order;
            Payment p = payments.pay(paid, PaymentMethod.CASH, paid.total());
            afterPaid(paid, p);
            Toast.success("Ticket " + paid.getTicketNumber() + " encaissé : " + Money.fmt(p.getAmountDueCents()) + " (espèces)");
            if (SettingsService.get().autoPrint()) {
                TicketActions.print(paid, p);
            }
        });
    }

    /** F6 : imprime l'addition (ticket non encore payé) et met la table "en attente de paiement". */
    public void print() {
        if (order.isEmpty()) {
            Toast.info("Le ticket est vide");
            return;
        }
        run(() -> {
            orders.requestBill(order);
            ticket.refreshTotals(false);
            TicketActions.print(order, null);
        });
    }

    private void preview() {
        if (!order.isEmpty()) {
            TicketActions.preview(order, null);
        }
    }

    /** Appelé dès qu'un paiement est enregistré : la caisse repart sur une nouvelle commande vide. */
    private void afterPaid(Order paidOrder, Payment payment) {
        startFreshOrder();
    }

    // ================================================================ tables

    private void pickTable() {
        List<CafeTable> list = tables.findAll();
        TablePickerDialog.show(list, order.getTableId(), !order.isPersisted(), this::selectTable);
    }

    /**
     * Lie la commande à une table (ou "sans table" si {@code table} est null).
     * Si la table a déjà une commande ouverte, on la reprend (ou on fusionne avec le ticket en cours).
     */
    public void selectTable(CafeTable table) {
        if (order == null) {
            onShow();
        }
        run(() -> {
            if (table == null) {
                order.setTableId(null);
                order.setTableName(null);
                ticket.refreshTotals(false);
                return;
            }
            Optional<Order> open = orders.findOpenOrder(table.getId());
            boolean sameOrder = open.isPresent() && order.isPersisted() && order.getId().equals(open.get().getId());
            if (open.isPresent() && !sameOrder) {
                Order existing = open.get();
                if (order.isEmpty()) {
                    switchTo(existing);
                } else {
                    Dialogs.confirm("Table déjà occupée",
                            "« " + table.getName() + " » a déjà une commande. Les articles du ticket en cours y seront ajoutés.",
                            "FUSIONNER", false, () -> run(() -> switchTo(orders.mergeInto(existing, order))));
                }
            } else {
                orders.assignFreeTable(order, table);
                ticket.refreshTotals(true);
                Toast.info("Ticket lié à « " + table.getName() + " »");
            }
        });
    }

    private void switchTo(Order o) {
        order = o;
        SessionManager.get().setDraft(o);
        ticket.setOrder(o);
        Toast.info("Commande de « " + o.getTableName() + " » reprise");
    }

    // ================================================================ divers

    /** Vrai si le ticket en cours contient des articles non enregistrés (aucune table). */
    public boolean hasUnsavedItems() {
        return order != null && !order.isEmpty() && order.getTableId() == null;
    }

    /** Exécute une action en affichant les erreurs (métier ou base) sous forme de notification. */
    private void run(Runnable action) {
        try {
            action.run();
        } catch (BusinessException e) {
            Toast.warning(e.getMessage());
        } catch (DataAccessException e) {
            Toast.error(e.getMessage());
        }
    }
}
