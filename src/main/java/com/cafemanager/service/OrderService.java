package com.cafemanager.service;

import com.cafemanager.dao.OrderDAO;
import com.cafemanager.dao.TableDAO;
import com.cafemanager.model.CafeTable;
import com.cafemanager.model.Order;
import com.cafemanager.model.OrderItem;
import com.cafemanager.model.OrderStatus;
import com.cafemanager.model.TableStatus;
import com.cafemanager.model.User;
import com.cafemanager.util.DatabaseConnection;
import com.cafemanager.util.SessionManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Cycle de vie d'une commande : création, sauvegarde automatique quand une table est liée,
 * demande d'addition, annulation, changement de table, fusion.
 * <p>
 * Règle de persistance : une commande liée à une table est enregistrée à chaque modification
 * (elle survit ainsi au changement d'écran et au redémarrage). Une commande "comptoir" (sans table)
 * est enregistrée au moment du paiement.
 */
public class OrderService {

    private final OrderDAO orderDAO = new OrderDAO();
    private final TableDAO tableDAO = new TableDAO();

    // ----------------------------------------------------------- création

    /** Nouvelle commande vide pour l'utilisateur connecté, avec le prochain numéro de ticket affiché. */
    public Order newDraft() {
        Order o = new Order();
        User u = SessionManager.get().getCurrentUser();
        if (u != null) {
            o.setServerId(u.getId());
            o.setServerName(u.getFullName());
        }
        o.setTicketNumber(orderDAO.peekNextTicketNumber());
        return o;
    }

    // ------------------------------------------------------ sauvegarde auto

    /**
     * À appeler après chaque modification des lignes / de la remise. Enregistre la commande si elle est liée à une
     * table ; supprime l'enregistrement si elle est redevenue vide.
     */
    public void onChanged(Order o) {
        if (o.getStatus() == OrderStatus.PENDING_PAYMENT) {
            o.setStatus(OrderStatus.OPEN);              // on a modifié la commande : plus "en attente de paiement"
        }
        if (o.isEmpty()) {
            discardIfPersisted(o);
            return;
        }
        if (o.getTableId() != null) {
            persist(o);
        }
    }

    private void persist(Order o) {
        Long prevId = o.getId();
        String prevNumber = o.getTicketNumber();
        try {
            DatabaseConnection.txRun(c -> {
                orderDAO.save(c, o);
                if (o.getTableId() != null) {
                    tableDAO.setStatus(c, o.getTableId(), statusFor(o.getStatus()));
                }
                return null;
            });
        } catch (RuntimeException e) {          // rollback : la commande en mémoire redevient "non enregistrée"
            o.setId(prevId);
            o.setTicketNumber(prevNumber);
            throw e;
        }
    }

    private void discardIfPersisted(Order o) {
        if (!o.isPersisted()) {
            return;
        }
        long id = o.getId();
        Long tableId = o.getTableId();
        DatabaseConnection.txRun(c -> {
            orderDAO.deleteEmptyDraft(c, id, o.getTicketNumber());
            if (tableId != null) {
                tableDAO.setStatus(c, tableId, TableStatus.LIBRE);
            }
            return null;
        });
        o.setId(null);
        o.setTicketNumber(orderDAO.peekNextTicketNumber());
    }

    private static TableStatus statusFor(OrderStatus s) {
        return s == OrderStatus.PENDING_PAYMENT ? TableStatus.EN_ATTENTE : TableStatus.OCCUPEE;
    }

    // -------------------------------------------------------------- tables

    public Optional<Order> findOpenOrder(long tableId) {
        return orderDAO.findOpenByTable(tableId);
    }

    /** Lie la commande (sans commande existante sur la table) à une table libre ; libère l'ancienne table. */
    public void assignFreeTable(Order o, CafeTable table) {
        Long oldTableId = o.getTableId();
        o.setTableId(table.getId());
        o.setTableName(table.getName());
        if (o.isEmpty() && !o.isPersisted()) {
            return;                                         // rien à enregistrer tant qu'il n'y a pas de ligne
        }
        persist(o);
        if (oldTableId != null && oldTableId != table.getId()) {
            tableDAO.setStatus(oldTableId, TableStatus.LIBRE);
        }
    }

    /** Ajoute les lignes de {@code source} à la commande déjà ouverte {@code target}, puis supprime {@code source}. */
    public Order mergeInto(Order target, Order source) {
        for (OrderItem it : source.getItems()) {
            Optional<OrderItem> same = target.findItem(it.getProductId());
            if (same.isPresent()) {
                same.get().setQuantity(same.get().getQuantity() + it.getQuantity());
            } else {
                target.getItems().add(new OrderItem(it.getProductId(), it.getProductName(),
                        it.getUnitPriceCents(), it.getQuantity()));
            }
        }
        if (source.isPersisted()) {
            long sourceId = source.getId();
            Long oldTable = source.getTableId();
            DatabaseConnection.txRun(c -> {
                orderDAO.delete(c, sourceId);
                if (oldTable != null && !oldTable.equals(target.getTableId())) {
                    tableDAO.setStatus(c, oldTable, TableStatus.LIBRE);
                }
                return null;
            });
        }
        target.setStatus(OrderStatus.OPEN);
        persist(target);
        return target;
    }

    // ------------------------------------------------------ addition / annulation

    /** Demande d'addition : la table passe en "commande en attente" (rouge). */
    public void requestBill(Order o) {
        if (o.isEmpty()) {
            return;
        }
        o.setStatus(OrderStatus.PENDING_PAYMENT);
        if (o.getTableId() != null) {
            persist(o);
        }
    }

    /** Annule la commande : conservée en base avec le statut ANNULÉE (traçabilité), table libérée. */
    public void cancel(Order o) {
        if (!o.isPersisted()) {
            return;
        }
        long id = o.getId();
        Long tableId = o.getTableId();
        DatabaseConnection.txRun(c -> {
            orderDAO.updateStatus(c, id, OrderStatus.CANCELLED);
            if (tableId != null) {
                tableDAO.setStatus(c, tableId, TableStatus.LIBRE);
            }
            return null;
        });
        o.setStatus(OrderStatus.CANCELLED);
    }

    // ---------------------------------------------------------- historique

    public List<Order> history(LocalDate from, LocalDate to, String search) {
        return orderDAO.findHistory(from, to, search);
    }

    public Optional<Order> findById(long id) {
        return orderDAO.findById(id);
    }

    /** Suppression définitive d'un ticket payé (administrateur). */
    public void deleteTicket(long orderId) {
        DatabaseConnection.txRun(c -> {
            orderDAO.delete(c, orderId);
            return null;
        });
    }
}
