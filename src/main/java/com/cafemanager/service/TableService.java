package com.cafemanager.service;

import com.cafemanager.dao.OrderDAO;
import com.cafemanager.dao.TableDAO;
import com.cafemanager.model.CafeTable;
import com.cafemanager.model.TableStatus;
import com.cafemanager.util.BusinessException;

import java.util.List;

/** Gestion des tables du café. */
public class TableService {

    private final TableDAO tableDAO = new TableDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    public List<CafeTable> findAll() {
        return tableDAO.findAll();
    }

    public CafeTable add(String name) {
        String n = clean(name);
        return tableDAO.insert(n);
    }

    public void rename(CafeTable table, String name) {
        tableDAO.rename(table.getId(), clean(name));
    }

    public void delete(CafeTable table) {
        if (orderDAO.findOpenByTable(table.getId()).isPresent()) {
            throw new BusinessException("Cette table a une commande en cours : encaissez-la ou annulez-la d'abord.");
        }
        tableDAO.delete(table.getId());
    }

    /** Libère une table (après paiement ou pour corriger un état). Refusé si une commande y est encore ouverte. */
    public void free(CafeTable table) {
        if (orderDAO.findOpenByTable(table.getId()).isPresent()) {
            throw new BusinessException("Une commande est encore ouverte sur cette table.");
        }
        tableDAO.setStatus(table.getId(), TableStatus.LIBRE);
    }

    private String clean(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Le nom de la table est obligatoire.");
        }
        return name.trim();
    }
}
