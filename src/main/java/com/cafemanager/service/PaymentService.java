package com.cafemanager.service;

import com.cafemanager.dao.OrderDAO;
import com.cafemanager.dao.PaymentDAO;
import com.cafemanager.dao.TableDAO;
import com.cafemanager.dao.TicketDAO;
import com.cafemanager.model.Order;
import com.cafemanager.model.OrderStatus;
import com.cafemanager.model.Payment;
import com.cafemanager.model.PaymentMethod;
import com.cafemanager.model.TableStatus;
import com.cafemanager.model.User;
import com.cafemanager.util.BusinessException;
import com.cafemanager.util.DatabaseConnection;
import com.cafemanager.util.SessionManager;

import java.time.LocalDateTime;
import java.util.Optional;

/** Encaissement : enregistre commande, paiement et ticket dans une seule transaction. */
public class PaymentService {

    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final TicketDAO ticketDAO = new TicketDAO();
    private final TableDAO tableDAO = new TableDAO();

    /** Droit d'encaisser selon le rôle de l'utilisateur connecté. */
    public boolean canPay() {
        User u = SessionManager.get().getCurrentUser();
        return u != null && u.getRole().canPay(SettingsService.get().serverCanPay());
    }

    /**
     * Encaisse la commande.
     *
     * @param receivedCents montant reçu (espèces uniquement ; ignoré pour carte/autre : on encaisse le total exact)
     */
    public Payment pay(Order order, PaymentMethod method, long receivedCents) {
        if (!canPay()) {
            throw new BusinessException("Votre rôle ne permet pas d'encaisser.");
        }
        if (order.isEmpty()) {
            throw new BusinessException("Le ticket est vide.");
        }
        if (method == null) {
            throw new BusinessException("Choisissez un mode de paiement.");
        }
        long total = order.total();
        long received;
        long change;
        if (method == PaymentMethod.CASH) {
            if (receivedCents < total) {
                throw new BusinessException("Montant reçu insuffisant.");
            }
            received = receivedCents;
            change = receivedCents - total;
        } else {
            received = total;
            change = 0;
        }

        LocalDateTime now = LocalDateTime.now();
        User cashier = SessionManager.get().getCurrentUser();

        // État à restaurer si la transaction échoue (l'objet en mémoire ne doit jamais mentir sur la base)
        Long prevId = order.getId();
        String prevNumber = order.getTicketNumber();
        OrderStatus prevStatus = order.getStatus();

        try {
            return DatabaseConnection.tx(c -> {
                order.setStatus(OrderStatus.PAID);
                order.setPaidAt(now);
                order.setPaymentMethod(method);
                orderDAO.save(c, order);               // insère si besoin (attribue le n° de ticket) + lignes finales
                orderDAO.markPaid(c, order);

                Payment p = new Payment();
                p.setOrderId(order.getId());
                p.setMethod(method);
                p.setAmountDueCents(total);
                p.setAmountReceivedCents(received);
                p.setChangeCents(change);
                p.setPaidAt(now);
                p.setCashierId(cashier == null ? null : cashier.getId());
                paymentDAO.insert(c, p);

                ticketDAO.insert(c, order.getTicketNumber(), order.getId(), now);
                if (order.getTableId() != null) {
                    tableDAO.setStatus(c, order.getTableId(), TableStatus.PAYEE);
                }
                return p;
            });
        } catch (RuntimeException e) {
            order.setId(prevId);
            order.setTicketNumber(prevNumber);
            order.setStatus(prevStatus);
            order.setPaidAt(null);
            order.setPaymentMethod(null);
            throw e;
        }
    }

    public Optional<Payment> findByOrder(long orderId) {
        return paymentDAO.findByOrder(orderId);
    }
}
