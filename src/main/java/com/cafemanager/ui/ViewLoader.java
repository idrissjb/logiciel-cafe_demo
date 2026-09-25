package com.cafemanager.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;

/** Chargement des vues FXML (resources/view/*.fxml). */
public final class ViewLoader {

    /** Une vue chargée : sa racine et son contrôleur. */
    public static final class View<T> {
        private final Parent root;
        private final T controller;

        View(Parent root, T controller) {
            this.root = root;
            this.controller = controller;
        }

        public Parent root() {
            return root;
        }

        public T controller() {
            return controller;
        }
    }

    private ViewLoader() {
    }

    public static <T> View<T> load(String name) {
        URL url = ViewLoader.class.getResource("/view/" + name + ".fxml");
        if (url == null) {
            throw new IllegalStateException("Vue introuvable : " + name);
        }
        try {
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            Emoji.fixTree(root);
            return new View<>(root, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("Erreur de chargement de la vue " + name, e);
        }
    }

    public static String css(String name) {
        return ViewLoader.class.getResource("/css/" + name + ".css").toExternalForm();
    }
}
