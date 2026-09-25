package com.cafemanager.service;

import com.cafemanager.dao.SettingsDAO;
import com.cafemanager.util.Money;

import java.util.HashMap;
import java.util.Map;

/** Paramètres de l'application (nom du café, TVA, imprimante...), mis en cache en mémoire. */
public final class SettingsService {

    private static final SettingsService INSTANCE = new SettingsService();

    private final SettingsDAO dao = new SettingsDAO();
    private volatile Map<String, String> values = new HashMap<>();

    private SettingsService() {
    }

    public static SettingsService get() {
        return INSTANCE;
    }

    public void reload() {
        values = dao.loadAll();
        Money.setCurrency(currency());
    }

    public void save(Map<String, String> changes) {
        dao.saveAll(changes);
        reload();
    }

    public String getString(String key, String def) {
        String v = values.get(key);
        return v == null ? def : v;
    }

    public boolean getBool(String key, boolean def) {
        String v = values.get(key);
        return v == null ? def : Boolean.parseBoolean(v);
    }

    public double getDouble(String key, double def) {
        try {
            return Double.parseDouble(getString(key, "").replace(',', '.'));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public String cafeName() { return getString("cafe_name", "CAFÉ CENTRAL"); }
    public String address() { return getString("address", ""); }
    public String phone() { return getString("phone", ""); }
    public String logoPath() { return getString("logo_path", ""); }
    public String currency() { return getString("currency", "DH"); }
    public double vatRate() { return getDouble("vat_rate", 0); }
    public String printerName() { return getString("printer_name", ""); }
    public boolean autoPrint() { return getBool("auto_print", true); }
    public int ticketWidthMm() { return "58".equals(getString("ticket_width", "80")) ? 58 : 80; }
    public String theme() { return getString("theme", "dark"); }
    public String registerNumber() { return getString("register_number", "1"); }
    public String footerMessage() { return getString("footer_message", "Merci de votre visite"); }
    public boolean serverCanPay() { return getBool("server_can_pay", false); }
}
