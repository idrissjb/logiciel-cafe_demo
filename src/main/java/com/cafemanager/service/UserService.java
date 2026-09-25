package com.cafemanager.service;

import com.cafemanager.dao.UserDAO;
import com.cafemanager.model.Role;
import com.cafemanager.model.User;
import com.cafemanager.util.BusinessException;
import com.cafemanager.util.DataAccessException;
import com.cafemanager.util.PasswordUtil;
import com.cafemanager.util.SessionManager;

import java.util.List;

/** Gestion des comptes utilisateurs (réservée à l'administrateur). */
public class UserService {

    private final UserDAO userDAO = new UserDAO();

    public List<User> findAll() {
        return userDAO.findAll();
    }

    /**
     * Crée ou modifie un utilisateur.
     *
     * @param newPassword mot de passe en clair ; vide = inchangé (obligatoire à la création)
     */
    public User save(User u, String newPassword) {
        if (u.getUsername() == null || u.getUsername().isBlank()) {
            throw new BusinessException("Le nom d'utilisateur est obligatoire.");
        }
        if (u.getFullName() == null || u.getFullName().isBlank()) {
            throw new BusinessException("Le nom affiché est obligatoire.");
        }
        u.setUsername(u.getUsername().trim());
        u.setFullName(u.getFullName().trim());
        boolean hasPassword = newPassword != null && !newPassword.isEmpty();
        boolean isNew = u.getId() == 0;
        if (isNew && !hasPassword) {
            throw new BusinessException("Le mot de passe est obligatoire pour un nouvel utilisateur.");
        }
        if (hasPassword && newPassword.length() < 4) {
            throw new BusinessException("Le mot de passe doit contenir au moins 4 caractères.");
        }
        if (hasPassword) {
            u.setSalt(PasswordUtil.newSalt());
            u.setPasswordHash(PasswordUtil.hash(newPassword, u.getSalt()));
        }

        if (!isNew) {
            User me = SessionManager.get().getCurrentUser();
            boolean self = me != null && me.getId() == u.getId();
            if (self && !u.isActive()) {
                throw new BusinessException("Vous ne pouvez pas désactiver votre propre compte.");
            }
            if (self && u.getRole() != Role.ADMINISTRATEUR) {
                throw new BusinessException("Vous ne pouvez pas retirer votre propre rôle d'administrateur.");
            }
            ensureAnotherAdminRemains(u);
        }
        try {
            if (isNew) {
                return userDAO.insert(u);
            }
            userDAO.update(u, hasPassword);
            return u;
        } catch (DataAccessException e) {
            if (String.valueOf(e.getMessage()).contains("UNIQUE")) {
                throw new BusinessException("Ce nom d'utilisateur existe déjà.");
            }
            throw e;
        }
    }

    public void delete(User u) {
        User me = SessionManager.get().getCurrentUser();
        if (me != null && me.getId() == u.getId()) {
            throw new BusinessException("Vous ne pouvez pas supprimer votre propre compte.");
        }
        if (u.getRole() == Role.ADMINISTRATEUR && u.isActive() && userDAO.countActiveAdmins() <= 1) {
            throw new BusinessException("Impossible : c'est le dernier administrateur actif.");
        }
        try {
            userDAO.delete(u.getId());
        } catch (DataAccessException e) {
            throw new BusinessException("Suppression impossible : " + e.getMessage());
        }
    }

    private void ensureAnotherAdminRemains(User edited) {
        boolean stillAdmin = edited.getRole() == Role.ADMINISTRATEUR && edited.isActive();
        if (stillAdmin) {
            return;
        }
        boolean wasActiveAdmin = userDAO.findAll().stream()
                .anyMatch(x -> x.getId() == edited.getId() && x.getRole() == Role.ADMINISTRATEUR && x.isActive());
        if (wasActiveAdmin && userDAO.countActiveAdmins() <= 1) {
            throw new BusinessException("Il doit toujours rester au moins un administrateur actif.");
        }
    }
}
