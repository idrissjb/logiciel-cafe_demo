package com.cafemanager.service;

import com.cafemanager.dao.OrderDAO;
import com.cafemanager.dao.PaymentDAO;
import com.cafemanager.dao.ProductDAO;
import com.cafemanager.dao.TableDAO;
import com.cafemanager.dao.TicketDAO;
import com.cafemanager.dao.UserDAO;
import com.cafemanager.model.CafeTable;
import com.cafemanager.model.Order;
import com.cafemanager.model.OrderStatus;
import com.cafemanager.model.Payment;
import com.cafemanager.model.PaymentMethod;
import com.cafemanager.model.Product;
import com.cafemanager.model.Role;
import com.cafemanager.model.User;
import com.cafemanager.util.DatabaseConnection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Outils de démonstration / test : génère un historique de ventes réaliste pour voir le dashboard et l'historique
 * "vivre", et permet de tout remettre à zéro.
 */
public class DemoDataService {

    private final ProductDAO productDAO = new ProductDAO();
    private final UserDAO userDAO = new UserDAO();
    private final TableDAO tableDAO = new TableDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final TicketDAO ticketDAO = new TicketDAO();

    /** Heures de pointe (poids) : matin et milieu d'après-midi. */
    private static final int[] HOUR_WEIGHT = {0, 0, 0, 0, 0, 0, 0, 2, 8, 10, 9, 6, 5, 5, 6, 8, 9, 8, 6, 4, 2, 1, 0, 0};

    /**
     * Génère des tickets payés sur les {@code days} derniers jours (aujourd'hui inclus, jusqu'à l'heure courante).
     *
     * @return nombre de tickets créés
     */
    public int generateSales(int days) {
        List<Product> products = productDAO.findAvailable();
        List<User> servers = userDAO.findAll().stream()
                .filter(u -> u.isActive() && u.getRole() == Role.SERVEUR).collect(Collectors.toList());
        List<CafeTable> tables = tableDAO.findAll();
        if (products.isEmpty()) {
            return 0;
        }
        // Les favoris et les cafés se vendent davantage
        List<Product> weighted = new ArrayList<>();
        for (Product p : products) {
            int w = 1 + (p.isFavorite() ? 4 : 0) + ("Cafés".equalsIgnoreCase(p.getCategoryName()) ? 2 : 0);
            for (int i = 0; i < w; i++) {
                weighted.add(p);
            }
        }
        Random rnd = new Random(42);
        LocalDateTime now = LocalDateTime.now();
        int[] created = {0};

        DatabaseConnection.txRun(c -> {
            for (int d = days - 1; d >= 0; d--) {
                LocalDate day = LocalDate.now().minusDays(d);
                int count = 35 + rnd.nextInt(40);
                List<LocalDateTime> times = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    int hour = pickHour(rnd);
                    LocalDateTime t = day.atTime(hour, rnd.nextInt(60), rnd.nextInt(60));
                    if (!t.isAfter(now)) {
                        times.add(t);
                    }
                }
                times.sort(LocalDateTime::compareTo);            // les numéros suivent l'ordre chronologique
                for (LocalDateTime t : times) {
                    Order o = new Order();
                    o.setCreatedAt(t.minusMinutes(2 + rnd.nextInt(10)));
                    if (!servers.isEmpty()) {
                        User s = servers.get(rnd.nextInt(servers.size()));
                        o.setServerId(s.getId());
                    }
                    if (!tables.isEmpty() && rnd.nextInt(100) < 55) {
                        o.setTableId(tables.get(rnd.nextInt(tables.size())).getId());
                    }
                    int lines = 1 + rnd.nextInt(3);
                    for (int i = 0; i < lines; i++) {
                        Product p = weighted.get(rnd.nextInt(weighted.size()));
                        int qty = 1 + (rnd.nextInt(100) < 25 ? rnd.nextInt(2) + 1 : 0);
                        for (int q = 0; q < qty; q++) {
                            o.addProduct(p);
                        }
                    }
                    long total = o.total();
                    int r = rnd.nextInt(100);
                    PaymentMethod method = r < 78 ? PaymentMethod.CASH : (r < 95 ? PaymentMethod.CARD : PaymentMethod.OTHER);
                    long received = total;
                    if (method == PaymentMethod.CASH) {
                        long[] notes = {500, 1000, 2000, 5000};
                        for (long n : notes) {
                            if (n >= total) {
                                received = n;
                                break;
                            }
                        }
                    }
                    o.setStatus(OrderStatus.PAID);
                    o.setPaidAt(t);
                    o.setPaymentMethod(method);
                    orderDAO.save(c, o);
                    orderDAO.markPaid(c, o);
                    Payment p = new Payment();
                    p.setOrderId(o.getId());
                    p.setMethod(method);
                    p.setAmountDueCents(total);
                    p.setAmountReceivedCents(received);
                    p.setChangeCents(received - total);
                    p.setPaidAt(t);
                    paymentDAO.insert(c, p);
                    ticketDAO.insert(c, o.getTicketNumber(), o.getId(), t);
                    created[0]++;
                }
            }
            // Les tables utilisées par les ventes de démo restent libres
            try (java.sql.Statement st = c.createStatement()) {
                st.executeUpdate("UPDATE tables_cafe SET status = 'LIBRE' WHERE id NOT IN "
                        + "(SELECT table_id FROM orders WHERE table_id IS NOT NULL AND status IN ('OPEN','PENDING_PAYMENT'))");
            }
            return null;
        });
        return created[0];
    }

    /** Supprime toutes les commandes, paiements et tickets, remet le compteur à zéro et libère les tables. */
    public void resetSales() {
        DatabaseConnection.txRun(c -> {
            try (java.sql.Statement st = c.createStatement()) {
                st.executeUpdate("DELETE FROM orders");                       // cascade : lignes, paiements, tickets
                st.executeUpdate("UPDATE sequences SET value = 0 WHERE name = 'ticket'");
                st.executeUpdate("UPDATE tables_cafe SET status = 'LIBRE'");
            }
            return null;
        });
    }

    private static int pickHour(Random rnd) {
        int total = 0;
        for (int w : HOUR_WEIGHT) {
            total += w;
        }
        int x = rnd.nextInt(total);
        for (int h = 0; h < HOUR_WEIGHT.length; h++) {
            x -= HOUR_WEIGHT[h];
            if (x < 0) {
                return h;
            }
        }
        return 9;
    }
}
