package com.cafemanager.ui;

import com.cafemanager.model.CafeTable;
import com.cafemanager.util.Money;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/** Sélection rapide d'une table depuis la caisse. */
public final class TablePickerDialog {

    private TablePickerDialog() {
    }

    /**
     * @param onPick reçoit la table choisie, ou {@code null} pour "sans table" (comptoir / à emporter)
     */
    public static void show(List<CafeTable> tables, Long currentTableId, boolean allowNone, Consumer<CafeTable> onPick) {
        FlowPane grid = new FlowPane(12, 12);
        grid.setPrefWrapLength(560);
        for (CafeTable t : tables) {
            VBox tile = new VBox(4);
            tile.setAlignment(Pos.CENTER);
            tile.getStyleClass().addAll("table-tile", "table-" + t.getStatus().getCssClass(), "pressable");
            if (currentTableId != null && currentTableId == t.getId()) {
                tile.getStyleClass().add("current");
            }
            Label name = new Label(t.getName());
            name.getStyleClass().add("table-tile-name");
            Label st = new Label(t.getStatus().getLabel());
            st.getStyleClass().add("table-tile-status");
            tile.getChildren().addAll(name, st);
            if (t.getOpenOrderId() != null) {
                Label amount = new Label(Money.compact(t.getOpenOrderTotal()));
                amount.getStyleClass().add("table-tile-amount");
                tile.getChildren().add(amount);
            }
            tile.setOnMouseClicked(e -> {
                Modal.close();
                onPick.accept(t);
            });
            grid.getChildren().add(tile);
        }

        HBox legend = new HBox(16);
        legend.setAlignment(Pos.CENTER_LEFT);
        for (com.cafemanager.model.TableStatus s : com.cafemanager.model.TableStatus.values()) {
            Label dot = new Label("●");
            dot.getStyleClass().addAll("legend-dot", "dot-" + s.getCssClass());
            Label l = new Label(s.getLabel());
            l.getStyleClass().add("dialog-detail");
            legend.getChildren().add(new HBox(6, dot, l));
        }

        VBox body = new VBox(16, grid, legend);
        body.setPadding(new Insets(2, 0, 0, 0));

        Button close = Dialogs.button("ANNULER", "btn-ghost", "btn-lg");
        close.setOnAction(e -> Modal.close());
        VBox card;
        if (allowNone) {
            Button none = Dialogs.button("SANS TABLE (COMPTOIR)", "btn-secondary", "btn-lg");
            none.setOnAction(e -> {
                Modal.close();
                onPick.accept(null);
            });
            card = Dialogs.card("Choisir une table", body, close, none);
        } else {
            card = Dialogs.card("Choisir une table", body, close);
        }
        card.setPrefWidth(640);
        Modal.show(card);
    }
}
