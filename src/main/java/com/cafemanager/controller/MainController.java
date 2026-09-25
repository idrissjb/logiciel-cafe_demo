package com.cafemanager.controller;

import com.cafemanager.App;
import com.cafemanager.model.CafeTable;
import com.cafemanager.model.Role;
import com.cafemanager.model.User;
import com.cafemanager.service.AuthService;
import com.cafemanager.service.SettingsService;
import com.cafemanager.ui.Anim;
import com.cafemanager.ui.Dialogs;
import com.cafemanager.ui.Modal;
import com.cafemanager.ui.Screen;
import com.cafemanager.ui.ViewLoader;
import com.cafemanager.util.DateUtil;
import com.cafemanager.util.SessionManager;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Coquille de l'application : sidebar réductible, barre supérieure (serveur, date, heure, caisse) et zone centrale
 * dans laquelle s'affichent les écrans. Gère aussi les raccourcis clavier globaux.
 */
public class MainController {

    private static final double SIDEBAR_WIDE = 252;
    private static final double SIDEBAR_NARROW = 96;

    /** Entrée du menu. */
    private static final class Nav {
        final String id;
        final String icon;
        final String label;
        final HBox node = new HBox(14);
        final Label text;
        /** Libellé affiché sous l'icône quand le menu est réduit. */
        final Label small;

        Nav(String id, String icon, String label) {
            this.id = id;
            this.icon = icon;
            this.label = label;
            Label ic = new Label(icon);
            ic.getStyleClass().addAll("nav-icon", "emoji");
            ic.setMinWidth(34);
            ic.setAlignment(Pos.CENTER);
            text = new Label(label);
            text.getStyleClass().add("nav-text");
            small = new Label(label);
            small.getStyleClass().add("nav-small");
            small.setVisible(false);
            small.setManaged(false);
            VBox iconBox = new VBox(3, ic, small);
            iconBox.setAlignment(Pos.CENTER);
            node.getChildren().addAll(iconBox, text);
            node.setAlignment(Pos.CENTER_LEFT);
            node.getStyleClass().addAll("nav-item", "pressable");
        }
    }

    private static MainController instance;

    @FXML private BorderPane root;
    @FXML private VBox sidebar;
    @FXML private StackPane content;
    @FXML private Label pageTitle;
    @FXML private Label avatarLabel;
    @FXML private Label userLabel;
    @FXML private Label dateLabel;
    @FXML private Label clockLabel;
    @FXML private Label registerLabel;

    private final AuthService auth = new AuthService();
    private final List<Nav> navs = new ArrayList<>();
    private final Map<String, ViewLoader.View<?>> views = new HashMap<>();
    private Nav activeNav;
    private Screen currentScreen;
    private boolean collapsed;
    private Label brandText;
    private Label toggleLabel;
    private Label logoutText;
    private Timeline clock;
    private EventHandler<KeyEvent> keyFilter;

    public static MainController get() {
        return instance;
    }

    // =================================================================== démarrage

    @FXML
    private void initialize() {
        instance = this;
    }

    /** Appelé par App une fois la vue affichée. */
    public void start() {
        User user = SessionManager.get().getCurrentUser();
        userLabel.setText(user.getRole().getLabel() + " : " + user.getFullName());
        avatarLabel.setText(user.getFullName().substring(0, 1).toUpperCase());
        registerLabel.setText("Caisse n°" + SettingsService.get().registerNumber());

        buildSidebar(user.getRole());
        startClock();
        installShortcuts();

        // Petit écran (1366 x 768) : on démarre avec la sidebar réduite pour laisser la place aux produits
        if (App.getStage().getWidth() < 1450) {
            setCollapsed(true, false);
        }
        navigate("pos");
    }

    /** Libère les ressources (déconnexion). */
    public void dispose() {
        if (clock != null) {
            clock.stop();
        }
        if (keyFilter != null && App.getScene() != null) {
            App.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
        }
        if (currentScreen != null) {
            currentScreen.onHide();
        }
        if (instance == this) {
            instance = null;
        }
    }

    // ===================================================================== sidebar

    private void buildSidebar(Role role) {
        sidebar.getChildren().clear();

        Label logo = new Label("☕");
        logo.getStyleClass().addAll("logo-mark", "emoji");
        brandText = new Label("CAFÉ MANAGER");
        brandText.getStyleClass().add("logo-text");
        brandText.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        Label toggle = new Label("«");
        toggle.getStyleClass().add("collapse-btn");
        toggle.setOnMouseClicked(e -> setCollapsed(!collapsed, true));
        toggle.setTooltip(new Tooltip("Réduire le menu"));
        toggleLabel = toggle;
        logo.setOnMouseClicked(e -> {
            if (collapsed) {
                setCollapsed(false, true);
            }
        });
        Tooltip logoTip = new Tooltip("Agrandir le menu");
        logoTip.setShowDelay(Duration.millis(150));
        Tooltip.install(logo, logoTip);
        HBox brand = new HBox(12, logo, brandText, grow, toggle);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("brand");
        sidebar.getChildren().add(brand);

        addNav("pos", "🏠", "Caisse", true);
        addNav("tables", "🍽", "Tables", true);
        addNav("history", "📋", "Tickets", role.canViewHistory());
        addNav("dashboard", "📊", "Dashboard", role.canViewDashboard());
        addNav("products", "☕", "Produits", role.isAdmin());
        addNav("categories", "📂", "Catégories", role.isAdmin());
        addNav("users", "👥", "Utilisateurs", role.isAdmin());
        addNav("settings", "⚙", "Paramètres", role.isAdmin());

        Region fill = new Region();
        VBox.setVgrow(fill, Priority.ALWAYS);
        sidebar.getChildren().add(fill);

        Nav logout = new Nav("logout", "🚪", "Déconnexion");
        logout.node.getStyleClass().add("nav-logout");
        logout.node.setOnMouseClicked(e -> onLogout());
        logoutText = logout.text;
        navs.add(logout);
        sidebar.getChildren().add(logout.node);
    }

    private void addNav(String id, String icon, String label, boolean allowed) {
        if (!allowed) {
            return;
        }
        Nav n = new Nav(id, icon, label);
        n.node.setOnMouseClicked((MouseEvent e) -> navigate(id));
        Tooltip tip = new Tooltip(label);
        tip.setShowDelay(Duration.millis(150));
        Tooltip.install(n.node, tip);
        navs.add(n);
        sidebar.getChildren().add(n.node);
    }

    private void setCollapsed(boolean value, boolean animate) {
        collapsed = value;
        double target = value ? SIDEBAR_NARROW : SIDEBAR_WIDE;
        Runnable applyTexts = () -> {
            for (Nav n : navs) {
                n.text.setVisible(!collapsed);
                n.text.setManaged(!collapsed);
                n.small.setVisible(collapsed);
                n.small.setManaged(collapsed);
            }
            brandText.setVisible(!collapsed);
            brandText.setManaged(!collapsed);
            toggleLabel.setVisible(!collapsed);
            toggleLabel.setManaged(!collapsed);
            sidebar.getStyleClass().remove("collapsed");
            if (collapsed) {
                sidebar.getStyleClass().add("collapsed");
            }
        };
        if (!animate) {
            sidebar.setPrefWidth(target);
            applyTexts.run();
            return;
        }
        if (!value) {
            applyTexts.run();               // on montre les textes dès le début de l'agrandissement
        }
        Timeline t = new Timeline(new KeyFrame(Duration.millis(190),
                new KeyValue(sidebar.prefWidthProperty(), target, Interpolator.EASE_BOTH)));
        t.setOnFinished(e -> {
            if (value) {
                applyTexts.run();
            }
        });
        t.play();
    }

    // =================================================================== navigation

    /** Affiche l'écran demandé (pos, tables, history, dashboard, products, categories, users, settings). */
    public void navigate(String id) {
        String title;
        switch (id) {
            case "pos": title = "Caisse"; break;
            case "tables": title = "Tables"; break;
            case "history": title = "Historique des ventes"; break;
            case "dashboard": title = "Dashboard"; break;
            case "products": title = "Produits"; break;
            case "categories": title = "Catégories"; break;
            case "users": title = "Utilisateurs"; break;
            case "settings": title = "Paramètres"; break;
            default: return;
        }
        ViewLoader.View<?> view = views.computeIfAbsent(id, ViewLoader::load);
        Object controller = view.controller();

        if (currentScreen != null) {
            currentScreen.onHide();
        }
        content.getChildren().setAll(view.root());
        Anim.slideIn(view.root(), 12, 190);
        currentScreen = controller instanceof Screen ? (Screen) controller : null;
        if (currentScreen != null) {
            currentScreen.onShow();
        }
        pageTitle.setText(title);
        setActive(id);
    }

    private void setActive(String id) {
        for (Nav n : navs) {
            n.node.getStyleClass().remove("active");
            if (n.id.equals(id)) {
                n.node.getStyleClass().add("active");
                activeNav = n;
            }
        }
    }

    /** Ouvre la caisse sur une table (appelé depuis l'écran des tables). */
    public void openPosForTable(CafeTable table) {
        navigate("pos");
        POSController pos = (POSController) views.get("pos").controller();
        pos.selectTable(table);
    }

    public POSController pos() {
        ViewLoader.View<?> v = views.get("pos");
        return v == null ? null : (POSController) v.controller();
    }

    public boolean isPosActive() {
        return activeNav != null && "pos".equals(activeNav.id);
    }

    /** Supprime la vue mise en cache (ex. la caisse doit recharger le catalogue). */
    public void invalidate(String id) {
        // Les écrans rechargent leurs données dans onShow(); rien à faire pour l'instant.
    }

    // ====================================================================== actions

    @FXML
    private void onSettings() {
        if (SessionManager.get().getCurrentUser().getRole().isAdmin()) {
            navigate("settings");
        } else {
            com.cafemanager.ui.Toast.info("Les paramètres sont réservés à l'administrateur.");
        }
    }

    @FXML
    private void onLogout() {
        Runnable doLogout = () -> {
            auth.logout();
            App.showLogin();
        };
        POSController p = pos();
        if (p != null && p.hasUnsavedItems()) {
            Dialogs.confirm("Déconnexion",
                    "Le ticket en cours n'est pas enregistré (aucune table) : il sera perdu.", "SE DÉCONNECTER", true, doLogout);
        } else {
            doLogout.run();
        }
    }

    // ====================================================================== horloge

    private void startClock() {
        Runnable tick = () -> {
            LocalDateTime now = LocalDateTime.now();
            dateLabel.setText(DateUtil.date(now));
            clockLabel.setText(now.format(DateUtil.TIME));
        };
        tick.run();
        clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick.run()));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    // ================================================================== raccourcis

    /**
     * F2 nouvelle commande, F4 paiement, F6 imprimer, Échap ferme la fenêtre (géré par Modal),
     * Suppr supprime la ligne sélectionnée, + / - changent la quantité.
     */
    private void installShortcuts() {
        keyFilter = e -> {
            if (Modal.isOpen()) {
                return;
            }
            KeyCode code = e.getCode();
            boolean typing = App.getScene().getFocusOwner() instanceof TextInputControl;

            if (code == KeyCode.F2 || code == KeyCode.F4 || code == KeyCode.F6) {
                if (!isPosActive()) {
                    navigate("pos");
                }
                POSController p = pos();
                if (code == KeyCode.F2) {
                    p.newOrder();
                } else if (code == KeyCode.F4) {
                    p.pay();
                } else {
                    p.print();
                }
                e.consume();
                return;
            }
            if (!isPosActive() || typing) {
                return;
            }
            POSController p = pos();
            String text = e.getText() == null ? "" : e.getText();
            if (code == KeyCode.DELETE || code == KeyCode.BACK_SPACE) {
                p.removeSelected();
                e.consume();
            } else if (code == KeyCode.ADD || code == KeyCode.PLUS || text.equals("+")) {
                p.changeSelectedQuantity(1);
                e.consume();
            } else if (code == KeyCode.SUBTRACT || code == KeyCode.MINUS || text.equals("-")) {
                p.changeSelectedQuantity(-1);
                e.consume();
            }
        };
        App.getScene().addEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
    }
}
