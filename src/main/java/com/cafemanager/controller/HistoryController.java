package com.cafemanager.controller;

import com.cafemanager.model.Order;
import com.cafemanager.model.Payment;
import com.cafemanager.service.OrderService;
import com.cafemanager.service.PaymentService;
import com.cafemanager.ui.Emoji;
import com.cafemanager.ui.Dialogs;
import com.cafemanager.ui.OrderDetailsDialog;
import com.cafemanager.ui.Screen;
import com.cafemanager.ui.TicketActions;
import com.cafemanager.ui.Toast;
import com.cafemanager.util.DataAccessException;
import com.cafemanager.util.DateUtil;
import com.cafemanager.util.Money;
import com.cafemanager.util.SessionManager;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.beans.property.SimpleStringProperty;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Historique des ventes : tableau des tickets payés, filtres par période, voir / détails / réimprimer / supprimer. */
public class HistoryController implements Screen {

    @FXML private Label summaryLabel;
    @FXML private Label totalLabel;
    @FXML private ToggleButton todayChip;
    @FXML private ToggleButton yesterdayChip;
    @FXML private ToggleButton weekChip;
    @FXML private ToggleButton monthChip;
    @FXML private DatePicker fromPicker;
    @FXML private DatePicker toPicker;
    @FXML private TextField searchField;
    @FXML private TableView<Order> table;

    private final OrderService orders = new OrderService();
    private final PaymentService payments = new PaymentService();
    private final ToggleGroup chips = new ToggleGroup();
    private boolean updating;

    @FXML
    private void initialize() {
        for (ToggleButton b : new ToggleButton[]{todayChip, yesterdayChip, weekChip, monthChip}) {
            b.setToggleGroup(chips);
            b.setFocusTraversable(false);
        }
        fromPicker.setValue(LocalDate.now());
        toPicker.setValue(LocalDate.now());
        todayChip.setSelected(true);

        chips.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null || updating) {
                return;
            }
            LocalDate today = LocalDate.now();
            updating = true;
            if (b == todayChip) {
                fromPicker.setValue(today);
                toPicker.setValue(today);
            } else if (b == yesterdayChip) {
                fromPicker.setValue(today.minusDays(1));
                toPicker.setValue(today.minusDays(1));
            } else if (b == weekChip) {
                fromPicker.setValue(today.minusDays(6));
                toPicker.setValue(today);
            } else {
                fromPicker.setValue(today.minusDays(29));
                toPicker.setValue(today);
            }
            updating = false;
            reload();
        });
        fromPicker.valueProperty().addListener((o, a, b) -> dateChanged());
        toPicker.valueProperty().addListener((o, a, b) -> dateChanged());
        searchField.textProperty().addListener((o, a, b) -> reload());

        buildColumns();
        table.setPlaceholder(new Label("Aucun ticket sur cette période"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void dateChanged() {
        if (updating) {
            return;
        }
        chips.selectToggle(null);
        reload();
    }

    @Override
    public void onShow() {
        reload();
    }

    private void buildColumns() {
        table.getColumns().add(col("N° Ticket", 0.9, o -> "#" + o.getTicketNumber(), "-fx-font-weight: bold;"));
        table.getColumns().add(col("Date", 1.0, o -> DateUtil.date(o.getPaidAt()), null));
        table.getColumns().add(col("Heure", 0.7, o -> DateUtil.time(o.getPaidAt()), null));
        table.getColumns().add(col("Serveur", 1.1, o -> o.getServerName() == null ? "—" : o.getServerName(), null));
        table.getColumns().add(col("Table", 1.0, o -> o.getTableName() == null ? "Comptoir" : o.getTableName(), null));
        table.getColumns().add(col("Montant", 1.0, o -> Money.fmt(o.getStoredTotal()), "-fx-font-weight: bold; -fx-text-fill: -c-accent-hi;"));
        table.getColumns().add(col("Paiement", 1.1, o -> o.getPaymentMethod() == null ? "—" : o.getPaymentMethod().getLabel(), null));

        TableColumn<Order, Order> actions = new TableColumn<>("Actions");
        actions.setSortable(false);
        actions.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue()));
        actions.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Order o, boolean empty) {
                super.updateItem(o, empty);
                if (empty || o == null) {
                    setGraphic(null);
                    return;
                }
                Button view = rowButton("👁", "Voir le ticket", false);
                view.setOnAction(e -> withPayment(o, (full, p) -> TicketActions.preview(full, p)));
                Button details = rowButton("ℹ", "Afficher les détails", false);
                details.setOnAction(e -> withPayment(o, OrderDetailsDialog::show));
                Button print = rowButton("🖨", "Réimprimer", false);
                print.setOnAction(e -> withPayment(o, (full, p) -> TicketActions.print(full, p)));
                HBox box = new HBox(8, view, details, print);
                if (SessionManager.get().getCurrentUser().getRole().isAdmin()) {
                    Button del = rowButton("🗑", "Supprimer le ticket", true);
                    del.setOnAction(e -> Dialogs.confirm("Supprimer le ticket",
                            "Le ticket #" + o.getTicketNumber() + " (" + Money.fmt(o.getStoredTotal()) + ") sera supprimé définitivement.",
                            "SUPPRIMER", true, () -> {
                                orders.deleteTicket(o.getId());
                                Toast.info("Ticket supprimé");
                                reload();
                            }));
                    box.getChildren().add(del);
                }
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        actions.setMinWidth(230);
        table.getColumns().add(actions);
    }

    private static Button rowButton(String text, String tip, boolean danger) {
        Button b = Emoji.button(text);
        b.getStyleClass().addAll("row-btn", "pressable");
        if (danger) {
            b.getStyleClass().add("row-btn-danger");
        }
        b.setTooltip(new Tooltip(tip));
        b.setFocusTraversable(false);
        return b;
    }

    private interface Text {
        String of(Order o);
    }

    private TableColumn<Order, String> col(String title, double weight, Text f, String style) {
        TableColumn<Order, String> c = new TableColumn<>(title);
        c.setCellValueFactory(v -> new SimpleStringProperty(f.of(v.getValue())));
        c.setPrefWidth(weight * 100);
        if (style != null) {
            c.setCellFactory(x -> new TableCell<>() {
                @Override
                protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty);
                    setText(empty ? null : s);
                    setStyle(style);
                }
            });
        }
        return c;
    }

    /** Charge le ticket complet (avec ses lignes) et son paiement, puis exécute l'action. */
    private void withPayment(Order summary, java.util.function.BiConsumer<Order, Payment> action) {
        try {
            Optional<Order> full = orders.findById(summary.getId());
            if (full.isEmpty()) {
                Toast.warning("Ticket introuvable");
                return;
            }
            Payment p = payments.findByOrder(summary.getId()).orElse(null);
            action.accept(full.get(), p);
        } catch (DataAccessException e) {
            Toast.error(e.getMessage());
        }
    }

    private void reload() {
        if (updating || fromPicker.getValue() == null || toPicker.getValue() == null) {
            return;
        }
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        if (to.isBefore(from)) {
            LocalDate t = from;
            from = to;
            to = t;
        }
        try {
            List<Order> list = orders.history(from, to, searchField.getText());
            table.getItems().setAll(list);
            long sum = list.stream().mapToLong(Order::getStoredTotal).sum();
            totalLabel.setText(Money.compact(sum));
            long avg = list.isEmpty() ? 0 : Math.round((double) sum / list.size());
            summaryLabel.setText(list.size() + (list.size() > 1 ? " tickets" : " ticket") + " · ticket moyen "
                    + Money.compact(avg) + (list.size() >= 2000 ? " · (2 000 premiers résultats)" : ""));
        } catch (DataAccessException e) {
            Toast.error(e.getMessage());
        }
    }
}
