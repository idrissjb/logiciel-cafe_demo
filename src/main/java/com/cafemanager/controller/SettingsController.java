package com.cafemanager.controller;

import com.cafemanager.App;
import com.cafemanager.model.Order;
import com.cafemanager.model.OrderItem;
import com.cafemanager.model.OrderStatus;
import com.cafemanager.model.PaymentMethod;
import com.cafemanager.service.CatalogService;
import com.cafemanager.service.DemoDataService;
import com.cafemanager.service.PrintService;
import com.cafemanager.service.SettingsService;
import com.cafemanager.ui.Dialogs;
import com.cafemanager.ui.IconBadge;
import com.cafemanager.ui.Modal;
import com.cafemanager.ui.Screen;
import com.cafemanager.ui.SwitchControl;
import com.cafemanager.ui.TicketActions;
import com.cafemanager.ui.Toast;
import com.cafemanager.util.BusinessException;
import com.cafemanager.util.DataAccessException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Paramètres : établissement, ticket & impression, thème, droits, outils de démonstration. */
public class SettingsController implements Screen {

    private static final String SYSTEM_DEFAULT = "Imprimante par défaut du système";

    @FXML private TextField cafeNameField;
    @FXML private TextField addressField;
    @FXML private TextField phoneField;
    @FXML private TextField currencyField;
    @FXML private TextField registerField;
    @FXML private StackPane logoHolder;
    @FXML private Button clearLogoButton;
    @FXML private Label logoLabel;
    @FXML private ToggleButton themeDark;
    @FXML private ToggleButton themeLight;
    @FXML private SwitchControl serverPaySwitch;
    @FXML private TextField vatField;
    @FXML private ComboBox<String> printerBox;
    @FXML private ToggleButton width58;
    @FXML private ToggleButton width80;
    @FXML private TextField footerField;
    @FXML private SwitchControl autoPrintSwitch;

    private final ToggleGroup themeGroup = new ToggleGroup();
    private final ToggleGroup widthGroup = new ToggleGroup();
    private final SettingsService settings = SettingsService.get();
    private String logoPath = "";

    @FXML
    private void initialize() {
        themeDark.setToggleGroup(themeGroup);
        themeLight.setToggleGroup(themeGroup);
        width58.setToggleGroup(widthGroup);
        width80.setToggleGroup(widthGroup);
        keepOneSelected(themeGroup, themeDark);
        keepOneSelected(widthGroup, width80);
        // Aperçu immédiat du thème (annulé si l'on quitte sans enregistrer : voir onHide)
        themeGroup.selectedToggleProperty().addListener((o, a, b) -> {
            if (b != null) {
                setThemeStyle(b == themeLight);
            }
        });
    }

    /** Empêche de désélectionner le seul bouton actif d'un groupe segmenté. */
    private static void keepOneSelected(ToggleGroup group, ToggleButton fallback) {
        group.selectedToggleProperty().addListener((o, oldT, newT) -> {
            if (newT == null && oldT != null) {
                group.selectToggle(oldT);
            }
        });
        group.selectToggle(fallback);
    }

    @Override
    public void onShow() {
        cafeNameField.setText(settings.cafeName());
        addressField.setText(settings.address());
        phoneField.setText(settings.phone());
        currencyField.setText(settings.currency());
        registerField.setText(settings.registerNumber());
        vatField.setText(trimZero(settings.vatRate()));
        footerField.setText(settings.footerMessage());
        autoPrintSwitch.setSelected(settings.autoPrint());
        serverPaySwitch.setSelected(settings.serverCanPay());
        themeGroup.selectToggle("light".equals(settings.theme()) ? themeLight : themeDark);
        widthGroup.selectToggle(settings.ticketWidthMm() == 58 ? width58 : width80);
        logoPath = settings.logoPath();
        refreshLogo();

        List<String> printers = PrintService.availablePrinters();
        printerBox.getItems().setAll(SYSTEM_DEFAULT);
        printerBox.getItems().addAll(printers);
        String saved = settings.printerName();
        if (saved.isBlank()) {
            printerBox.setValue(SYSTEM_DEFAULT);
        } else {
            if (!printers.contains(saved)) {
                printerBox.getItems().add(saved);   // imprimante configurée mais absente : on la garde visible
            }
            printerBox.setValue(saved);
        }
    }

    @Override
    public void onHide() {
        App.applyTheme();      // annule l'aperçu de thème non enregistré
    }

    private static String trimZero(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d).replace('.', ',');
    }

    private void setThemeStyle(boolean light) {
        var root = App.getRoot();
        if (root == null) {
            return;
        }
        root.getStyleClass().remove("light");
        if (light) {
            root.getStyleClass().add("light");
        }
    }

    // ==================================================================== logo

    @FXML
    private void onPickLogo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir le logo");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File f = fc.showOpenDialog(Modal.window());
        if (f != null) {
            try {
                logoPath = new CatalogService().importImage(f);
                refreshLogo();
            } catch (BusinessException e) {
                Toast.warning(e.getMessage());
            }
        }
    }

    @FXML
    private void onClearLogo() {
        logoPath = "";
        refreshLogo();
    }

    private void refreshLogo() {
        boolean has = logoPath != null && !logoPath.isBlank();
        logoHolder.getChildren().setAll(new IconBadge("☕", has ? logoPath : null, 56));
        logoLabel.setText(has ? new File(logoPath).getName() : "Aucun logo");
        clearLogoButton.setDisable(!has);
    }

    // ================================================================ enregistrer

    @FXML
    private void onSave() {
        String name = cafeNameField.getText() == null ? "" : cafeNameField.getText().trim();
        if (name.isEmpty()) {
            Toast.warning("Le nom du café est obligatoire.");
            return;
        }
        double vat;
        try {
            String t = vatField.getText() == null ? "" : vatField.getText().trim().replace(',', '.');
            vat = t.isEmpty() ? 0 : Double.parseDouble(t);
            if (vat < 0 || vat > 100) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            Toast.warning("TVA invalide : saisissez un pourcentage entre 0 et 100.");
            return;
        }
        String currency = currencyField.getText() == null || currencyField.getText().isBlank()
                ? "DH" : currencyField.getText().trim();
        String printer = printerBox.getValue() == null || SYSTEM_DEFAULT.equals(printerBox.getValue())
                ? "" : printerBox.getValue();

        Map<String, String> m = new LinkedHashMap<>();
        m.put("cafe_name", name);
        m.put("address", nz(addressField.getText()));
        m.put("phone", nz(phoneField.getText()));
        m.put("logo_path", nz(logoPath));
        m.put("currency", currency);
        m.put("register_number", nz(registerField.getText()).isEmpty() ? "1" : registerField.getText().trim());
        m.put("vat_rate", String.valueOf(vat));
        m.put("printer_name", printer);
        m.put("auto_print", String.valueOf(autoPrintSwitch.isSelected()));
        m.put("ticket_width", width58.isSelected() ? "58" : "80");
        m.put("footer_message", nz(footerField.getText()));
        m.put("theme", themeLight.isSelected() ? "light" : "dark");
        m.put("server_can_pay", String.valueOf(serverPaySwitch.isSelected()));
        try {
            settings.save(m);
            App.applyTheme();
            Toast.success("Paramètres enregistrés");
        } catch (DataAccessException e) {
            Toast.error(e.getMessage());
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    // ======================================================== test impression

    private static Order sampleOrder() {
        Order o = new Order();
        o.setTicketNumber("TEST");
        o.setStatus(OrderStatus.PAID);
        o.setCreatedAt(LocalDateTime.now());
        o.setPaidAt(LocalDateTime.now());
        o.setPaymentMethod(PaymentMethod.CASH);
        var u = com.cafemanager.util.SessionManager.get().getCurrentUser();
        o.setServerName(u == null ? "Test" : u.getFullName());
        o.getItems().add(new OrderItem(null, "Café normal", 800, 2));
        o.getItems().add(new OrderItem(null, "Cappuccino", 1500, 1));
        o.getItems().add(new OrderItem(null, "Croissant", 700, 1));
        return o;
    }

    @FXML
    private void onTestPreview() {
        Order o = sampleOrder();
        com.cafemanager.ui.TicketPreviewDialog.show(o, null);
    }

    @FXML
    private void onTestPrint() {
        Order o = sampleOrder();
        TicketActions.print(o, null);
    }

    // ================================================================== démo

    @FXML
    private void onGenerateDemo() {
        Dialogs.confirm("Générer des ventes de démonstration",
                "Des tickets fictifs seront ajoutés sur les 30 derniers jours (ils s'ajoutent aux ventes existantes).",
                "GÉNÉRER", false, () -> {
                    try {
                        int n = new DemoDataService().generateSales(30);
                        Toast.success(n + " tickets de démonstration créés");
                    } catch (RuntimeException e) {
                        Toast.error(e.getMessage());
                    }
                });
    }

    @FXML
    private void onResetSales() {
        Dialogs.confirm("Effacer toutes les ventes",
                "Tous les tickets, paiements et l'historique seront supprimés et la numérotation repartira de 000001. "
                        + "Cette action est irréversible.",
                "EFFACER", true, () -> {
                    try {
                        new DemoDataService().resetSales();
                        Toast.info("Ventes effacées");
                    } catch (RuntimeException e) {
                        Toast.error(e.getMessage());
                    }
                });
    }
}
