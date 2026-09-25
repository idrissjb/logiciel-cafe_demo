package com.cafemanager.controller;

import com.cafemanager.model.CafeTable;
import com.cafemanager.model.TableStatus;
import com.cafemanager.service.TableService;
import com.cafemanager.ui.Emoji;
import com.cafemanager.ui.Anim;
import com.cafemanager.ui.Dialogs;
import com.cafemanager.ui.Modal;
import com.cafemanager.ui.Screen;
import com.cafemanager.ui.Toast;
import com.cafemanager.util.BusinessException;
import com.cafemanager.util.DataAccessException;
import com.cafemanager.util.Money;
import com.cafemanager.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Plan des tables : une carte par table, colorée selon son état
 * (vert libre, orange en cours, rouge en attente de paiement, bleu payée). Un clic ouvre la caisse sur la table.
 */
public class TableController implements Screen {

    @FXML private FlowPane tablesPane;
    @FXML private Label summaryLabel;
    @FXML private HBox legendBox;
    @FXML private Button manageButton;

    private final TableService service = new TableService();
    private Timeline autoRefresh;

    @Override
    public void onShow() {
        manageButton.setVisible(SessionManager.get().getCurrentUser().getRole().isAdmin());
        manageButton.setManaged(manageButton.isVisible());
        reload();
        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(20), e -> reload()));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }

    @Override
    public void onHide() {
        if (autoRefresh != null) {
            autoRefresh.stop();
        }
    }

    @FXML
    private void onRefresh() {
        reload();
    }

    private void reload() {
        List<CafeTable> tables;
        try {
            tables = service.findAll();
        } catch (DataAccessException e) {
            Toast.error(e.getMessage());
            return;
        }
        tablesPane.getChildren().clear();
        Map<TableStatus, Integer> counts = new EnumMap<>(TableStatus.class);
        for (CafeTable t : tables) {
            counts.merge(t.getStatus(), 1, Integer::sum);
            tablesPane.getChildren().add(card(t));
        }

        legendBox.getChildren().clear();
        for (TableStatus s : TableStatus.values()) {
            Label dot = new Label("●");
            dot.getStyleClass().addAll("legend-dot", "dot-" + s.getCssClass());
            Label l = new Label(s.getLabel() + " (" + counts.getOrDefault(s, 0) + ")");
            l.getStyleClass().add("screen-sub");
            legendBox.getChildren().add(new HBox(6, dot, l));
        }
        int busy = counts.getOrDefault(TableStatus.OCCUPEE, 0) + counts.getOrDefault(TableStatus.EN_ATTENTE, 0);
        summaryLabel.setText(tables.size() + " tables · " + counts.getOrDefault(TableStatus.LIBRE, 0) + " libres · "
                + busy + " en cours de service");
    }

    private VBox card(CafeTable t) {
        VBox card = new VBox(4);
        card.getStyleClass().addAll("cafe-table", t.getStatus().getCssClass());

        Label name = new Label(t.getName());
        name.getStyleClass().add("table-name");
        Label state = new Label(t.getStatus().getLabel().toUpperCase());
        state.getStyleClass().add("table-state");
        card.getChildren().addAll(name, state);

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        card.getChildren().add(grow);

        if (t.getOpenOrderId() != null) {
            Label amount = new Label(Money.fmt(t.getOpenOrderTotal()));
            amount.getStyleClass().add("table-amount");
            Label info = new Label(t.getOpenOrderItems() + (t.getOpenOrderItems() > 1 ? " articles" : " article")
                    + " · " + since(t.getOpenSince()));
            info.getStyleClass().add("table-info");
            card.getChildren().addAll(amount, info);
        } else if (t.getStatus() == TableStatus.PAYEE) {
            Label info = new Label("Client parti ?");
            info.getStyleClass().add("table-info");
            Button release = new Button("Libérer la table");
            release.getStyleClass().addAll("table-release", "pressable");
            release.setOnAction(e -> {
                e.consume();
                free(t);
            });
            card.getChildren().addAll(info, release);
        } else {
            Label info = new Label("Touchez pour ouvrir");
            info.getStyleClass().add("table-info");
            card.getChildren().add(info);
        }

        DropShadow shadow = new DropShadow(16, 0, 8, Color.rgb(0, 0, 0, 0.40));
        card.setEffect(shadow);
        Anim.installCardEffects(card, shadow, 5, 1.03);
        card.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof Button) {
                return;
            }
            MainController.get().openPosForTable(t);
        });
        return card;
    }

    private static String since(LocalDateTime start) {
        if (start == null) {
            return "";
        }
        long min = java.time.Duration.between(start, LocalDateTime.now()).toMinutes();
        if (min < 1) {
            return "à l'instant";
        }
        return min < 60 ? min + " min" : (min / 60) + " h " + String.format("%02d", min % 60);
    }

    private void free(CafeTable t) {
        try {
            service.free(t);
            Toast.info(t.getName() + " libérée");
            reload();
        } catch (BusinessException e) {
            Toast.warning(e.getMessage());
        }
    }

    // ============================================================ gestion (admin)

    @FXML
    private void onManage() {
        VBox rows = new VBox(8);
        rows.setPadding(new Insets(2, 4, 2, 0));
        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(rows);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("flat-scroll");
        sp.setPrefViewportHeight(320);
        sp.setPrefWidth(460);
        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            rows.getChildren().clear();
            for (CafeTable t : service.findAll()) {
                Label n = new Label(t.getName());
                n.getStyleClass().add("dialog-message");
                Button rename = new Button("✎");
                rename.getStyleClass().addAll("row-btn", "pressable");
                rename.setOnAction(e -> Dialogs.prompt("Renommer la table", "Nom de la table", t.getName(), "ENREGISTRER",
                        v -> guard(() -> service.rename(t, v), refresh[0])));
                Button del = Emoji.button("🗑");
                del.getStyleClass().addAll("row-btn", "row-btn-danger", "pressable");
                del.setOnAction(e -> Dialogs.confirm("Supprimer la table", "« " + t.getName() + " » sera supprimée du plan.",
                        "SUPPRIMER", true, () -> guard(() -> service.delete(t), refresh[0])));
                HBox row = new HBox(10, n, Dialogs.spacer(), rename, del);
                row.setAlignment(Pos.CENTER_LEFT);
                rows.getChildren().add(row);
            }
        };
        refresh[0].run();

        Button add = Dialogs.button("＋  AJOUTER UNE TABLE", "btn-secondary", "btn-lg");
        add.setOnAction(e -> Dialogs.prompt("Nouvelle table", "Nom (ex. Table 7, Terrasse 3)", "", "AJOUTER",
                v -> guard(() -> service.add(v), refresh[0])));
        Button close = Dialogs.button("TERMINÉ", "btn-primary", "btn-lg");
        close.setOnAction(e -> Modal.close());
        Modal.show(Dialogs.card("Gérer les tables", sp, add, close), true, this::reload);
    }

    private void guard(Runnable action, Runnable after) {
        try {
            action.run();
            after.run();
        } catch (BusinessException e) {
            Toast.warning(e.getMessage());
        } catch (DataAccessException e) {
            Toast.warning(e.getMessage().contains("UNIQUE") ? "Ce nom existe déjà." : e.getMessage());
        }
    }
}
