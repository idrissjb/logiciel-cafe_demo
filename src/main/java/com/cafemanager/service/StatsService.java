package com.cafemanager.service;

import com.cafemanager.dao.StatsDAO;
import com.cafemanager.dao.StatsDAO.NameValue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Calcule toutes les données du dashboard. */
public class StatsService {

    /** Période d'analyse des graphiques et des "meilleurs". */
    public enum Period {
        TODAY("Aujourd'hui"), WEEK("7 jours"), MONTH("30 jours");

        private final String label;

        Period(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /** Résultat prêt à afficher. */
    public static class DashboardStats {
        public long revenueToday;
        public long revenueYesterday;
        public long revenueWeek;
        public long revenueMonth;
        public long ticketsToday;
        public long avgTicketToday;
        public Double changeVsYesterdayPct;          // null si hier = 0

        public String topProduct;
        public long topProductQty;
        public String topCategory;
        public long topCategoryRevenue;
        public int peakHour = -1;
        public long peakHourTickets;
        public long coffeesSold;                     // dans la période choisie
        public long unitsSold;

        public Map<Integer, Long> hourlyRevenue = new TreeMap<>();   // 0..23 -> centimes
        public List<NameValue> byCategory;
        public List<NameValue> topProducts;
        public Map<LocalDate, Long> last7Days;
    }

    private final StatsDAO dao = new StatsDAO();

    public DashboardStats load(Period period) {
        DashboardStats s = new DashboardStats();
        LocalDate today = LocalDate.now();
        LocalDateTime tomorrow = today.plusDays(1).atStartOfDay();

        long[] t = dao.revenueAndCount(today.atStartOfDay(), tomorrow);
        s.revenueToday = t[0];
        s.ticketsToday = t[1];
        s.avgTicketToday = t[1] == 0 ? 0 : Math.round((double) t[0] / t[1]);
        s.revenueYesterday = dao.revenueAndCount(today.minusDays(1).atStartOfDay(), today.atStartOfDay())[0];
        s.changeVsYesterdayPct = s.revenueYesterday == 0 ? null
                : (s.revenueToday - s.revenueYesterday) * 100.0 / s.revenueYesterday;

        LocalDate monday = today.with(DayOfWeek.MONDAY);
        s.revenueWeek = dao.revenueAndCount(monday.atStartOfDay(), tomorrow)[0];
        s.revenueMonth = dao.revenueAndCount(today.withDayOfMonth(1).atStartOfDay(), tomorrow)[0];

        LocalDate from = switch (period) {
            case TODAY -> today;
            case WEEK -> today.minusDays(6);
            case MONTH -> today.minusDays(29);
        };
        LocalDateTime start = from.atStartOfDay();

        s.topProducts = dao.topProducts(start, tomorrow, 8);
        if (!s.topProducts.isEmpty()) {
            s.topProduct = s.topProducts.get(0).name();
            s.topProductQty = s.topProducts.get(0).value();
        }
        s.byCategory = dao.revenueByCategory(start, tomorrow);
        if (!s.byCategory.isEmpty()) {
            s.topCategory = s.byCategory.get(0).name();
            s.topCategoryRevenue = s.byCategory.get(0).value();
        }
        for (int h = 0; h < 24; h++) {
            s.hourlyRevenue.put(h, 0L);
        }
        for (Map.Entry<Integer, long[]> e : dao.byHour(start, tomorrow).entrySet()) {
            s.hourlyRevenue.put(e.getKey(), e.getValue()[0]);
            if (e.getValue()[1] > s.peakHourTickets) {
                s.peakHourTickets = e.getValue()[1];
                s.peakHour = e.getKey();
            }
        }
        s.coffeesSold = dao.unitsSoldInCategoryLike(start, tomorrow, "Caf%");
        s.unitsSold = dao.unitsSold(start, tomorrow);
        s.last7Days = dao.revenuePerDay(today.minusDays(6), today);
        return s;
    }
}
