package com.cafemanager.ui;

import com.cafemanager.model.Category;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListCell;

/**
 * Affichage fiable des icônes emoji.
 * <p>
 * Dans un texte normal, la police par défaut n'a pas les émojis récents (🥤, 🥪, 🥐 …) et dessine un carré vide.
 * Placer l'icône dans son propre {@code Label} de style {@code emoji} force la police d'émojis du système.
 */
public final class Emoji {

    private Emoji() {
    }

    /** Une icône seule, avec la police d'émojis. */
    public static Label glyph(String emoji) {
        Label l = new Label(emoji);
        l.getStyleClass().add("emoji");
        return l;
    }

    /** Vrai pour les vrais émojis (plans supplémentaires Unicode) : ce sont eux que la police normale ne sait pas dessiner. */
    private static boolean isEmoji(int codePoint) {
        return codePoint >= 0x1F000;
    }

    private static final String GRAPHIC_KEY = "emoji-graphic";

    /**
     * Comme {@code setText}, mais si le texte commence par un émoji ("🖨  IMPRIMER") celui-ci est
     * affiché dans son propre label. Un texte sans émoji est simplement affiché.
     */
    public static void text(Labeled target, String text) {
        if (text != null && !text.isEmpty() && isEmoji(text.codePointAt(0))) {
            int cp = text.codePointAt(0);
            String rest = text.substring(Character.charCount(cp)).replaceFirst("^\\uFE0F?\\s*", "");
            beside(target, new String(Character.toChars(cp)), rest);
            target.getProperties().put(GRAPHIC_KEY, Boolean.TRUE);
            return;
        }
        target.setText(text);
        if (target.getProperties().remove(GRAPHIC_KEY) != null) {
            target.setGraphic(null);
        }
    }

    /** Bouton dont le texte peut commencer par un émoji. */
    public static Button button(String text) {
        Button b = new Button();
        text(b, text);
        return b;
    }

    /** Applique {@link #text} à tous les boutons / labels d'une vue chargée depuis un FXML. */
    public static void fixTree(Node node) {
        if (node instanceof Labeled l && l.getGraphic() == null && l.getText() != null) {
            text(l, l.getText());
        }
        if (node instanceof Parent p) {
            for (Node child : p.getChildrenUnmodifiable()) {
                fixTree(child);
            }
        }
    }

    /** Icône à gauche, texte à droite (bouton, puce…). */
    public static void beside(Labeled target, String emoji, String text) {
        target.setText(text);
        if (emoji == null || emoji.isBlank()) {
            target.setGraphic(null);
            return;
        }
        target.setGraphic(glyph(emoji.trim()));
        target.setContentDisplay(text == null || text.isEmpty() ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
        target.setGraphicTextGap(8);
    }

    /** Icône au-dessus du texte (cartes de mode de paiement). */
    public static void above(Labeled target, String emoji, String text) {
        target.setText(text);
        target.setGraphic(glyph(emoji));
        target.setContentDisplay(ContentDisplay.TOP);
        target.setGraphicTextGap(4);
    }

    /** Liste déroulante de catégories : « icône + nom » sans carré vide. */
    public static void categoryCells(ComboBox<Category> box) {
        box.setCellFactory(v -> new CategoryCell());
        box.setButtonCell(new CategoryCell());
    }

    private static final class CategoryCell extends ListCell<Category> {
        @Override
        protected void updateItem(Category c, boolean empty) {
            super.updateItem(c, empty);
            if (empty || c == null) {
                setText(null);
                setGraphic(null);
            } else {
                beside(this, c.getIcon(), c.getName());
            }
        }
    }
}
