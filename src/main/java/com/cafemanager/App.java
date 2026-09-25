package com.cafemanager;

import com.cafemanager.controller.LoginController;
import com.cafemanager.controller.MainController;
import com.cafemanager.service.SettingsService;
import com.cafemanager.ui.Anim;
import com.cafemanager.ui.Modal;
import com.cafemanager.ui.Toast;
import com.cafemanager.ui.ViewLoader;
import com.cafemanager.util.DatabaseConnection;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Application CAFÉ MANAGER.
 * <p>
 * La racine de la scène est un StackPane : le 1er enfant est l'écran courant (connexion ou application),
 * les suivants sont les couches superposées (fenêtres modales, notifications).
 */
public class App extends Application {

    private static Stage stage;
    private static Scene scene;
    private static StackPane root;
    private static StackPane screenHolder;
    private static MainController main;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        try {
            DatabaseConnection.init();
            SettingsService.get().reload();
        } catch (RuntimeException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir la base de données :\n" + e.getMessage());
            alert.showAndWait();
            Platform.exit();
            return;
        }

        screenHolder = new StackPane();
        root = new StackPane(screenHolder);
        root.getStyleClass().add("app-root-holder");
        scene = new Scene(root, 1366, 768);
        scene.getStylesheets().add(ViewLoader.css("main"));
        Modal.install(root, scene);
        Anim.installGlobalPressFeedback(scene);
        applyTheme();

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            Toast.error("Erreur inattendue : " + e.getMessage());
        });

        stage.setTitle("CAFÉ MANAGER");
        stage.setMinWidth(1100);
        stage.setMinHeight(680);
        stage.setScene(scene);
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setWidth(Math.min(1366, bounds.getWidth()));
        stage.setHeight(Math.min(768, bounds.getHeight()));
        stage.setMaximized(true);
        showLogin();
        stage.show();
    }

    // ------------------------------------------------------------- navigation

    public static void showLogin() {
        Modal.closeAll();
        if (main != null) {
            main.dispose();
            main = null;
        }
        ViewLoader.View<LoginController> v = ViewLoader.load("login");
        setScreen(v.root());
        v.controller().onShown();
    }

    public static void showMain() {
        Modal.closeAll();
        ViewLoader.View<MainController> v = ViewLoader.load("main");
        main = v.controller();
        setScreen(v.root());
        main.start();
    }

    private static void setScreen(Parent p) {
        screenHolder.getChildren().setAll(p);
        Anim.fadeIn(p, 220);
    }

    /** Applique le thème (sombre par défaut, ou clair) défini dans les paramètres. */
    public static void applyTheme() {
        if (root == null) {
            return;
        }
        boolean light = "light".equals(SettingsService.get().theme());
        root.getStyleClass().remove("light");
        if (light) {
            root.getStyleClass().add("light");
        }
    }

    public static Stage getStage() {
        return stage;
    }

    public static Scene getScene() {
        return scene;
    }

    public static StackPane getRoot() {
        return root;
    }
}
