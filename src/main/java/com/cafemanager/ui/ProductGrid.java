package com.cafemanager.ui;

import com.cafemanager.model.Product;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

/**
 * Grille de cartes produits qui s'adapte à la largeur disponible (1366x768 comme 1920x1080) :
 * le nombre de colonnes est calculé automatiquement et les cartes se partagent exactement la largeur.
 */
public class ProductGrid extends StackPane {

    private static final double MIN_CARD_WIDTH = 168;
    private static final double GAP = 16;
    private static final double PAD = 16;

    private final TilePane tiles = new TilePane();
    private final ScrollPane scroll = new ScrollPane(tiles);
    private final Label empty = new Label("Aucun produit à afficher");
    private final Consumer<Product> onSelect;
    private List<Product> current = List.of();

    public ProductGrid(Consumer<Product> onSelect) {
        this.onSelect = onSelect;
        getStyleClass().add("product-grid");

        tiles.setHgap(GAP);
        tiles.setVgap(GAP);
        tiles.setPadding(new Insets(PAD, PAD, PAD + 8, PAD));
        tiles.setTileAlignment(Pos.CENTER);
        tiles.getStyleClass().add("product-tiles");

        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("flat-scroll");
        scroll.viewportBoundsProperty().addListener((o, a, b) -> relayout(b.getWidth()));

        empty.getStyleClass().add("empty-state");
        empty.setVisible(false);

        getChildren().addAll(scroll, empty);
    }

    public void setProducts(List<Product> products) {
        this.current = products;
        tiles.getChildren().clear();
        int i = 0;
        for (Product p : products) {
            ProductCard card = new ProductCard(p, onSelect);
            tiles.getChildren().add(card);
            if (i < 24) {                                   // apparition échelonnée très rapide
                card.setOpacity(0);
                PauseTransition delay = new PauseTransition(Duration.millis(i * 14));
                delay.setOnFinished(e -> Anim.fadeIn(card, 140));
                delay.play();
            }
            i++;
        }
        empty.setVisible(products.isEmpty());
        relayout(scroll.getViewportBounds().getWidth());
        scroll.setVvalue(0);
    }

    private void relayout(double viewportWidth) {
        if (viewportWidth <= 0) {
            return;
        }
        double usable = viewportWidth - 2 * PAD;
        int cols = Math.max(1, (int) Math.floor((usable + GAP) / (MIN_CARD_WIDTH + GAP)));
        double w = Math.floor((usable - GAP * (cols - 1)) / cols);
        double h = Math.max(150, Math.min(w * 0.98, 196));
        tiles.setPrefTileWidth(w);
        tiles.setPrefTileHeight(h);
        tiles.setPrefColumns(cols);
    }

    public List<Product> getProducts() {
        return current;
    }
}
