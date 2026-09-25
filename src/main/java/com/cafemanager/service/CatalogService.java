package com.cafemanager.service;

import com.cafemanager.dao.CategoryDAO;
import com.cafemanager.dao.ProductDAO;
import com.cafemanager.model.Category;
import com.cafemanager.model.Product;
import com.cafemanager.util.AppPaths;
import com.cafemanager.util.BusinessException;
import com.cafemanager.util.DataAccessException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

/** Catalogue : produits et catégories, avec validation des saisies. */
public class CatalogService {

    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    // -------------------------------------------------------------- lecture

    public List<Product> allProducts() {
        return productDAO.findAll();
    }

    /** Produits vendables en caisse. */
    public List<Product> availableProducts() {
        return productDAO.findAvailable();
    }

    public List<Category> allCategories() {
        return categoryDAO.findAll();
    }

    public List<Category> activeCategories() {
        return categoryDAO.findActive();
    }

    // --------------------------------------------------------------- produits

    public Product saveProduct(Product p) {
        if (p.getName() == null || p.getName().isBlank()) {
            throw new BusinessException("Le nom du produit est obligatoire.");
        }
        if (p.getCategoryId() <= 0) {
            throw new BusinessException("Choisissez une catégorie.");
        }
        if (p.getPriceCents() < 0) {
            throw new BusinessException("Le prix ne peut pas être négatif.");
        }
        p.setName(p.getName().trim());
        if (p.getId() == 0) {
            return productDAO.insert(p);
        }
        productDAO.update(p);
        return p;
    }

    public void deleteProduct(Product p) {
        productDAO.delete(p.getId());     // les anciens tickets gardent le nom/prix (copiés dans order_items)
    }

    public void setProductActive(Product p, boolean active) {
        productDAO.setActive(p.getId(), active);
        p.setActive(active);
    }

    // -------------------------------------------------------------- catégories

    public Category saveCategory(Category c) {
        if (c.getName() == null || c.getName().isBlank()) {
            throw new BusinessException("Le nom de la catégorie est obligatoire.");
        }
        c.setName(c.getName().trim());
        try {
            if (c.getId() == 0) {
                if (c.getDisplayOrder() <= 0) {
                    c.setDisplayOrder(categoryDAO.nextDisplayOrder());
                }
                return categoryDAO.insert(c);
            }
            categoryDAO.update(c);
            return c;
        } catch (DataAccessException e) {
            if (String.valueOf(e.getMessage()).contains("UNIQUE")) {
                throw new BusinessException("Une catégorie porte déjà ce nom.");
            }
            throw e;
        }
    }

    public void deleteCategory(Category c) {
        int n = categoryDAO.countProducts(c.getId());
        if (n > 0) {
            throw new BusinessException("Cette catégorie contient " + n + " produit" + (n > 1 ? "s" : "")
                    + ". Déplacez-les ou supprimez-les d'abord.");
        }
        categoryDAO.delete(c.getId());
    }

    // ------------------------------------------------------------------ images

    /**
     * Copie une image choisie par l'utilisateur dans le dossier de données de l'application
     * (l'image reste disponible même si le fichier d'origine est déplacé) et retourne son chemin.
     */
    public String importImage(File source) {
        try {
            String name = source.getName().replaceAll("[^A-Za-z0-9._-]", "_");
            String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT) : ".png";
            Path target = AppPaths.images().resolve(System.currentTimeMillis() + "_"
                    + name.replaceAll("\\.[^.]*$", "") + ext);
            Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new BusinessException("Impossible d'importer l'image : " + e.getMessage());
        }
    }
}
