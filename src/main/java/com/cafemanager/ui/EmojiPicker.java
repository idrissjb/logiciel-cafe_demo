package com.cafemanager.ui;

import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;

import java.util.function.Consumer;

/** Grille d'icônes (émojis) prêtes à l'emploi pour les produits et les catégories. */
public class EmojiPicker extends FlowPane {

    private static final String[] ICONS = {
            "☕", "🥛", "🍫", "🍵", "🌿", "🥤", "🧃", "🍹", "🍧", "💧", "🥐", "🥖", "🍞", "🥯", "🍰", "🧁",
            "🍩", "🍪", "🥧", "🍮", "🍦", "🍨", "🥪", "🍔", "🍕", "🌮", "🥗", "🍳", "🥞", "🧇", "🍓", "🍋",
            "🍊", "🍎", "🍌", "🍯", "🥜", "🍴", "🔥", "⭐"
    };

    public EmojiPicker(Consumer<String> onPick) {
        setHgap(6);
        setVgap(6);
        setPrefWrapLength(340);
        for (String e : ICONS) {
            Button b = new Button(e);
            b.getStyleClass().addAll("emoji-grid-btn", "emoji", "pressable");
            b.setFocusTraversable(false);
            b.setOnAction(ev -> onPick.accept(e));
            getChildren().add(b);
        }
    }
}
