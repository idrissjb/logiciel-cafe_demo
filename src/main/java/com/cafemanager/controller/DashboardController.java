package com.cafemanager.controller;

import com.cafemanager.dao.StatsDAO.NameValue;
import com.cafemanager.service.StatsService;
import com.cafemanager.service.StatsService.DashboardStats;
import com.cafemanager.service.StatsService.Period;
import com.cafemanager.ui.Screen;
import com.cafemanager.ui.Toast;
import com.cafemanager.util.DataAccessException;
import com.cafemanager.util.Money;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Dashboard : indicateurs clés (CA jour / semaine / mois, tickets, ticket moyen, top produit...) et graphiques. */
public class DashboardController implements Screen {

    @FXML private Label subtitleLabel;
    @FXML private ToggleButton todayToggle;
    @FXML private ToggleButton weekToggle;
    @FXML private ToggleButton monthToggle;
    @FXML private GridPane kpiGrid;
    @FXML private BarChart<String, Number> hourChart;
    @FXML private PieChart categoryChart;
    @FXML private BarChart<Number, String> topChart;
    @FXML private AreaChart<String, Number> weekChart;
    @FXML private Label hourSub;
    @FXML private Label categorySub;
    @FXML private Label topSub;

    private final StatsService stats = new StatsService();
    private final ToggleGroup periodGroup = new ToggleGroup();
    private Period period = Period.TODAY;

    @FXML
    private void initialize() {
        todayToggle.setToggleGroup(periodGroup);
        weekToggle.setToggleGroup(periodGroup);
        monthToggle.setToggleGroup(periodGroup);
        todayToggle.setUserData(Period.TODAY);
        weekToggle.setUserData(Period.WEEK);
        monthToggle.setUserData(Period.MONTH);
        todayToggle.setSelected(true);
        periodGroup.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) {
                a.setSelected(true);
                return;
            }
            period = (Period) b.getUserData();
            reload();
        });
        hourChart.setAnimated(false);
        topChart.setAnimated(false);
        weekChart.setAnimated(false);
        categoryChart.setAnimated(false);
    }

    @Override
    public void onShow() {
        reload();
    }

    @FXML
    private void onRefresh() {
        reload();
    }

    private void reload() {
        DashboardStats s;
        try {
            s = stats.load(period);
        } catch (DataAccessException e) {
            Toast.error(e.getMessage());
            return;
        }
        String scope = period.getLabel().toLowerCase(Locale.FRENCH);
        subtitleLabel.setText("Vue d'ensemble de l'activité · indicateurs détaillés : " + scope);
        buildKpis(s, scope);
        buildHourChart(s);
        buildCategoryChart(s);
        buildTopChart(s);
        buildWeekChart(s);
        hourSub.setText("Chiffre d'affaires, en DH · " + scope);
        categorySub.setText("Répartition du chiffre d'affaires · " + scope);
        topSub.setText("Quantités vendues · " + scope);
    }

    // ------------------------------------------------------------------ KPI

    private void buildKpis(DashboardStats s, String scope) {
        kpiGrid.getChildren().clear();

        String delta;
        String deltaStyle = "kpi-sub";
        if (s.changeVsYesterdayPct == null) {
            delta = "Pas de ventes hier pour comparer";
        } else {
            double p = s.changeVsYesterdayPct;
            delta = (p >= 0 ? "▲ +" : "▼ ") + Math.round(p) + " % par rapport à hier";
            deltaStyle = p >= 0 ? "kpi-up" : "kpi-down";
        }
        kpi(0, 0, "💰", "CA aujourd'hui", Money.compact(s.revenueToday), delta, deltaStyle, true);
        kpi(1, 0, "📅", "CA semaine", Money.compact(s.revenueWeek), "Depuis lundi", "kpi-sub", false);
        kpi(2, 0, "🗓", "CA mois", Money.compact(s.revenueMonth), "Depuis le 1er du mois", "kpi-sub", false);
        kpi(3, 0, "🧾", "Commandes", Money.group(s.ticketsToday), "Tickets encaissés aujourd'hui", "kpi-sub", false);
        kpi(4, 0, "⚖", "Ticket moyen", Money.compact(s.avgTicketToday), "Aujourd'hui", "kpi-sub", false);

        kpi(0, 1, "🏆", "Produit populaire", s.topProduct == null ? "—" : s.topProduct,
                s.topProduct == null ? "Aucune vente" : Money.group(s.topProductQty) + " ventes · " + scope, "kpi-sub", false, true);
        kpi(1, 1, "📂", "Catégorie n°1", s.topCategory == null ? "—" : s.topCategory,
                s.topCategory == null ? "Aucune vente" : Money.compact(s.topCategoryRevenue) + " · " + scope, "kpi-sub", false, true);
        kpi(2, 1, "⏰", "Heure de pointe", s.peakHour < 0 ? "—" : s.peakHour + "h – " + (s.peakHour + 1) + "h",
                s.peakHour < 0 ? "Aucune vente" : s.peakHourTickets + " commandes · " + scope, "kpi-sub", false, true);
        kpi(3, 1, "☕", "Cafés vendus", Money.group(s.coffeesSold), scope, "kpi-sub", false);
        kpi(4, 1, "📦", "Articles vendus", Money.group(s.unitsSold), scope, "kpi-sub", false);
    }

    private void kpi(int col, int row, String icon, String label, String value, String sub, String subStyle, boolean hero) {
        kpi(col, row, icon, label, value, sub, subStyle, hero, false);
    }

    private void kpi(int col, int row, String icon, String label, String value, String sub, String subStyle,
                     boolean hero, boolean smallValue) {
        Label ic = new Label(icon);
        ic.getStyleClass().addAll("kpi-icon", "emoji");
        Label l = new Label(label.toUpperCase(Locale.FRENCH));
        l.getStyleClass().add("kpi-label");
        HBox head = new HBox(10, ic, l);
        head.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label v = new Label(value);
        v.getStyleClass().add(smallValue ? "kpi-value-sm" : "kpi-value");
        v.setMaxWidth(Double.MAX_VALUE);
        Label sb = new Label(sub);
        sb.getStyleClass().add(subStyle);
        sb.setWrapText(true);

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        VBox card = new VBox(10, head, v, sb);
        card.getStyleClass().add("kpi-card");
        if (hero) {
            card.getStyleClass().add("hero");
        }
        card.setMinHeight(148);
        GridPane.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        kpiGrid.add(card, col, row);
    }

    // ---------------------------------------------------------------- graphiques

    private void buildHourChart(DashboardStats s) {
        int min = 24;
        int max = -1;
        for (Map.Entry<Integer, Long> e : s.hourlyRevenue.entrySet()) {
            if (e.getValue() > 0) {
                min = Math.min(min, e.getKey());
                max = Math.max(max, e.getKey());
            }
        }
        if (max < 0) {
            min = 7;
            max = 21;
        }
        min = Math.min(min, 8);
        max = Math.max(max, 20);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int h = min; h <= max; h++) {
            series.getData().add(new XYChart.Data<>(h + "h", s.hourlyRevenue.getOrDefault(h, 0L) / 100.0));
        }
        hourChart.getData().setAll(Collections.singletonList(series));
    }

    private void buildCategoryChart(DashboardStats s) {
        List<PieChart.Data> data = new ArrayList<>();
        long total = s.byCategory.stream().mapToLong(NameValue::value).sum();
        for (NameValue nv : s.byCategory) {
            long pct = total == 0 ? 0 : Math.round(nv.value() * 100.0 / total);
            data.add(new PieChart.Data(nv.name() + "  " + pct + " %", nv.value() / 100.0));
        }
        categoryChart.getData().setAll(data);
    }

    private void buildTopChart(DashboardStats s) {
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        // ordre inversé : le n°1 s'affiche en haut
        List<NameValue> list = new ArrayList<>(s.topProducts);
        Collections.reverse(list);
        for (NameValue nv : list) {
            series.getData().add(new XYChart.Data<>(nv.value(), nv.name()));
        }
        topChart.getData().setAll(Collections.singletonList(series));
    }

    private void buildWeekChart(DashboardStats s) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<LocalDate, Long> e : s.last7Days.entrySet()) {
            String day = e.getKey().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
            series.getData().add(new XYChart.Data<>(day + " " + e.getKey().getDayOfMonth(), e.getValue() / 100.0));
        }
        weekChart.getData().setAll(Collections.singletonList(series));
    }
}
