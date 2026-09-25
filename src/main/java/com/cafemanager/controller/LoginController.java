package com.cafemanager.controller;

import com.cafemanager.App;
import com.cafemanager.model.Role;
import com.cafemanager.model.User;
import com.cafemanager.service.AuthService;
import com.cafemanager.service.SettingsService;
import com.cafemanager.ui.Anim;
import com.cafemanager.ui.PasswordStrengthMeter;
import com.cafemanager.ui.RevealPasswordField;
import com.cafemanager.util.BusinessException;
import com.cafemanager.util.DataAccessException;
import com.cafemanager.util.FieldErrorsException;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.LinkedHashMap;
import java.util.Map;

/** Page de connexion et d'inscription. */
public class LoginController {

    /** Réglage (table settings) qui mémorise le dernier utilisateur connecté. */
    private static final String LAST_USERNAME_KEY = "last_username";

    /** Un champ du formulaire d'inscription et le message d'erreur affiché sous lui. */
    private record Field(Node input, Label error) {
    }

    @FXML private StackPane root;
    @FXML private Pane glowLayer;
    @FXML private StackPane artBox;
    @FXML private ImageView bgImage;
    @FXML private ImageView photoView;
    @FXML private VBox card;
    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private RevealPasswordField passwordField;
    @FXML private RevealPasswordField confirmField;
    @FXML private ComboBox<Role> roleBox;
    @FXML private VBox fullNameBox;
    @FXML private VBox confirmBox;
    @FXML private VBox roleGroup;
    @FXML private PasswordStrengthMeter strengthMeter;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label fullNameError;
    @FXML private Label usernameError;
    @FXML private Label passwordError;
    @FXML private Label confirmError;
    @FXML private Label roleError;
    @FXML private Label errorLabel;
    @FXML private Button submitButton;
    @FXML private Button switchButton;

    private final AuthService auth = new AuthService();
    private final Map<String, Field> fields = new LinkedHashMap<>();
    private boolean registerMode;
    private String lastUsername = "";

    @FXML
    private void initialize() {
        Image photo = new Image(getClass().getResource("/images/login-cafe.jpg").toExternalForm());
        bgImage.setImage(photo);
        bgImage.setEffect(new GaussianBlur(28));
        bgImage.setOpacity(0.9);
        root.widthProperty().addListener((o, a, w) -> coverBackground(photo));
        root.heightProperty().addListener((o, a, h) -> coverBackground(photo));

        photoView.setImage(photo);
        cover(photoView, photo, artBox.getPrefWidth(), artBox.getPrefHeight());
        Rectangle clip = new Rectangle(artBox.getPrefWidth(), artBox.getPrefHeight());
        clip.setArcWidth(64);
        clip.setArcHeight(64);
        artBox.setClip(clip);

        // Halos lumineux discrets en arrière-plan
        addGlow(0.12, 0.18, 300, Color.web("#C98B5B", 0.20));
        addGlow(0.88, 0.85, 340, Color.web("#8B5E3C", 0.26));
        addGlow(0.62, 0.05, 200, Color.web("#D4A373", 0.10));

        roleBox.getItems().setAll(Role.SERVEUR, Role.CAISSIER);
        roleBox.setValue(Role.SERVEUR);

        // L'ordre d'insertion est l'ordre de focus quand plusieurs champs sont en erreur
        fields.put("fullName", new Field(fullNameField, fullNameError));
        fields.put("username", new Field(usernameField, usernameError));
        fields.put("password", new Field(passwordField, passwordError));
        fields.put("confirm", new Field(confirmField, confirmError));
        fields.put("role", new Field(roleBox, roleError));

        // Dès que l'on corrige un champ, son erreur disparaît
        fullNameField.textProperty().addListener((o, a, b) -> edited("fullName"));
        usernameField.textProperty().addListener((o, a, b) -> edited("username"));
        passwordField.textProperty().addListener((o, a, b) -> {
            edited("password");
            strengthMeter.update(b);
        });
        confirmField.textProperty().addListener((o, a, b) -> edited("confirm"));
        roleBox.valueProperty().addListener((o, a, b) -> edited("role"));

        lastUsername = SettingsService.get().getString(LAST_USERNAME_KEY, "");
        usernameField.setText(lastUsername);
    }

    private void coverBackground(Image photo) {
        cover(bgImage, photo, root.getWidth(), root.getHeight());
    }

    /** Affiche l'image en "cover" : remplit w×h en recadrant au centre, sans déformer. */
    private static void cover(ImageView view, Image img, double w, double h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        double scale = Math.max(w / img.getWidth(), h / img.getHeight());
        double vw = w / scale;
        double vh = h / scale;
        view.setViewport(new Rectangle2D((img.getWidth() - vw) / 2, (img.getHeight() - vh) / 2, vw, vh));
        view.setFitWidth(w);
        view.setFitHeight(h);
    }

    private void addGlow(double fx, double fy, double radius, Color color) {
        Circle c = new Circle(radius, color);
        c.setEffect(new GaussianBlur(90));
        c.centerXProperty().bind(root.widthProperty().multiply(fx));
        c.centerYProperty().bind(root.heightProperty().multiply(fy));
        glowLayer.getChildren().add(c);
    }

    /** Appelé quand l'écran est affiché. */
    public void onShown() {
        Anim.slideIn(card, 24, 320);
        // Nom déjà rempli : on va directement au mot de passe
        if (usernameField.getText().isBlank()) {
            usernameField.requestFocus();
        } else {
            passwordField.focusField();
        }
    }

    // ------------------------------------------------------------ actions

    @FXML
    private void onSubmit() {
        clearAllErrors();
        try {
            User user;
            if (registerMode) {
                user = auth.register(fullNameField.getText(), usernameField.getText(), passwordField.getText(),
                        confirmField.getText(), roleBox.getValue());
            } else {
                user = auth.login(usernameField.getText(), passwordField.getText());
            }
            rememberUsername(user.getUsername());
            App.showMain();
        } catch (FieldErrorsException e) {
            showFieldErrors(e.getErrors());
        } catch (BusinessException e) {
            // Vider le champ d'abord : sa modification efface les erreurs affichées
            passwordField.clear();
            passwordField.focusField();
            showBanner(e.getMessage());
            if (!registerMode) {
                setInvalid(usernameField, true);
                setInvalid(passwordField, true);
            }
        } catch (DataAccessException e) {
            showBanner("Erreur de base de données. Réessayez.");
        }
    }

    /** Bascule entre "Se connecter" et "Créer un compte" sur la même carte. */
    @FXML
    private void onSwitchMode() {
        registerMode = !registerMode;
        clearAllErrors();
        passwordField.clear();
        confirmField.clear();

        setShown(fullNameBox, registerMode);
        setShown(confirmBox, registerMode);
        setShown(roleGroup, registerMode);
        setShown(strengthMeter, registerMode);
        setShown(subtitleLabel, !registerMode);
        card.getStyleClass().remove("login-card-compact");
        if (registerMode) {
            card.getStyleClass().add("login-card-compact");
        }

        titleLabel.setText(registerMode ? "Créer un compte" : "Bienvenue");
        submitButton.setText(registerMode ? "CRÉER MON COMPTE" : "SE CONNECTER");
        switchButton.setText(registerMode ? "Déjà un compte ? Se connecter" : "Pas de compte ? Créer un compte");

        if (registerMode) {
            // Un nouveau compte ne doit pas hériter du dernier utilisateur mémorisé
            fullNameField.clear();
            usernameField.clear();
            fullNameField.requestFocus();
        } else {
            usernameField.setText(lastUsername);
            if (lastUsername.isBlank()) {
                usernameField.requestFocus();
            } else {
                passwordField.focusField();
            }
        }
    }

    // ------------------------------------------------------------ erreurs

    private void showFieldErrors(Map<String, String> errors) {
        Anim.shake(card);
        Node first = null;
        for (Map.Entry<String, Field> e : fields.entrySet()) {
            String message = errors.get(e.getKey());
            if (message == null) {
                continue;
            }
            Field f = e.getValue();
            f.error().setText(message);
            setShown(f.error(), true);
            setInvalid(f.input(), true);
            if (first == null) {
                first = f.input();
            }
        }
        if (first != null) {
            focus(first);
        }
    }

    private void showBanner(String message) {
        errorLabel.setText(message);
        setShown(errorLabel, true);
        Anim.shake(card);
    }

    /** Le champ vient d'être modifié : on retire son erreur (et l'éventuel bandeau de connexion). */
    private void edited(String key) {
        Field f = fields.get(key);
        setShown(f.error(), false);
        setInvalid(f.input(), false);
        if (!registerMode) {
            setShown(errorLabel, false);
            setInvalid(usernameField, false);
            setInvalid(passwordField, false);
        }
    }

    private void clearAllErrors() {
        for (Field f : fields.values()) {
            setShown(f.error(), false);
            setInvalid(f.input(), false);
        }
        setShown(errorLabel, false);
    }

    private static void setInvalid(Node input, boolean invalid) {
        if (input instanceof RevealPasswordField pw) {
            pw.setInvalid(invalid);
            return;
        }
        input.getStyleClass().remove("login-input-error");
        if (invalid) {
            input.getStyleClass().add("login-input-error");
        }
    }

    private static void focus(Node input) {
        if (input instanceof RevealPasswordField pw) {
            pw.focusField();
        } else {
            input.requestFocus();
        }
    }

    private static void setShown(Node n, boolean shown) {
        n.setVisible(shown);
        n.setManaged(shown);
    }

    // ------------------------------------------------------------ mémorisation

    /** Mémorise le nom d'utilisateur pour le préremplir à la prochaine connexion (sans jamais bloquer l'accès). */
    private void rememberUsername(String username) {
        lastUsername = username;
        try {
            SettingsService.get().save(Map.of(LAST_USERNAME_KEY, username));
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}
