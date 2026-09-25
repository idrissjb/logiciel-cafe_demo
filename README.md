# CAFÉ MANAGER — caisse (POS) pour café / cafétéria

Application de bureau **JavaFX + SQLite** (Maven), interface sombre « premium », pensée pour l'écran tactile
(cartes produits 3D, boutons larges, animations 100–200 ms, raccourcis clavier).

## Lancer l'application

Prérequis : **JDK 17 ou plus** et **Maven 3.8+** (connexion Internet la première fois pour télécharger JavaFX et SQLite).

```bash
mvn javafx:run          # lance l'application
mvn clean package       # compile et vérifie
```

Ou utilisez `run.sh` (Linux/macOS) / `run.bat` (Windows).
Dans **IntelliJ IDEA / VS Code / Eclipse** : ouvrir le dossier comme projet Maven, puis lancer la classe
`com.cafemanager.Launcher` (ne lancez pas `App` directement : le `Launcher` évite l'erreur
« JavaFX runtime components are missing »).

## Comptes de démonstration (créés au premier lancement)

| Identifiant | Mot de passe | Rôle |
|---|---|---|
| `admin` | `admin123` | Administrateur (tout) |
| `sara` | `1234` | Caissier (caisse, tables, encaissement, historique) |
| `yassine` | `1234` | Serveur (commandes, tables ; encaissement désactivé par défaut) |

Changez ces mots de passe dans **Utilisateurs** avant une vraie mise en service.
L'option **Paramètres → « Les serveurs peuvent encaisser »** autorise les serveurs à encaisser.

## Données

Tout est enregistré dans `~/.cafemanager/` : `cafe.db` (SQLite), `images/` (images importées), `exports/` (PDF).
Un autre dossier peut être choisi avec l'option JVM `-Dcafe.home=/chemin`.
Pour repartir de zéro : fermer l'application et supprimer ce dossier.
Au premier lancement : 7 catégories, 19 produits, 8 tables et 3 utilisateurs sont créés.
**Paramètres → Outils de démonstration** génère 30 jours de ventes fictives (dashboard/historique) ou les efface.

## Fonctionnalités

- **Caisse** : catégories (+ Favoris), grille de produits (clic = ajout, clics répétés = quantité), recherche insensible
  aux accents/majuscules (« capp » → Cappuccino), ticket en direct `TICKET #000125` avec ➕ ➖ 🗑 par ligne,
  sous-total / remise (% ou montant) / total, ANNULER · REMISE · PAIEMENT · IMPRIMER · ENCAISSER.
- **Paiement** : espèces (montant reçu, monnaie à rendre, pavé numérique, montants rapides), carte, autre.
  Le paiement, le ticket et la table sont mis à jour dans **une seule transaction**.
- **Ticket thermique** 58/80 mm : aperçu, impression (Java Print Service, imprimante choisie dans les paramètres),
  export PDF, duplicata à la réimpression. Numérotation unique et continue (`000001`, `000002`, …), sans trou même si
  une saisie est abandonnée.
- **Tables** : Libre / Occupée / Commande en attente / Payée, liées aux commandes ; déplacer/fusionner ;
  gestion des tables (admin).
- **Tickets** : historique filtrable (aujourd'hui, hier, 7/30 jours, dates, recherche), voir / détails / réimprimer.
- **Dashboard** : CA jour/semaine/mois, tickets, ticket moyen, top produit/catégorie, heure de pointe, cafés vendus,
  comparaison à hier, graphiques (ventes/heure, catégories, top produits, CA 7 jours).
- **Administration** : produits (icône emoji ou image), catégories (ordre, actif), utilisateurs (rôles), paramètres
  (établissement, TVA, devise, imprimante, largeur papier, impression auto, thème sombre/clair…).
- Confirmations pour toute action destructive, notifications (toasts), thème clair/sombre.

## Raccourcis

`F2` nouvelle commande · `F4` paiement · `F6` imprimer · `Échap` fermer une fenêtre ·
`Suppr` supprimer la ligne sélectionnée · `+` / `-` quantité de la ligne sélectionnée.

## Architecture

```
src/main/java/com/cafemanager
├── App / Launcher            démarrage JavaFX
├── model/                    Product, Category, Order, OrderItem, Payment, User, Role, CafeTable…
├── dao/                      accès SQLite (JDBC pur, requêtes préparées)
├── service/                  règles métier : commandes, paiement, tickets, impression, stats, catalogue, utilisateurs
├── controller/               contrôleurs FXML (POS, tables, historique, dashboard, produits, catégories, utilisateurs, paramètres)
├── ui/                       composants réutilisables (ProductCard, TicketPanel, PaymentDialog, Modal, Toast, SwitchControl…)
└── util/                     base de données, montants (long en centimes), mots de passe PBKDF2, chemins
src/main/resources
├── view/*.fxml               écrans
└── css/main.css              thème (sombre + clair), effets 3D, animations
```

Les montants sont stockés en **centimes (entiers)** : aucune erreur d'arrondi. Les mots de passe sont hachés (PBKDF2 + sel).

## Notes

- Sans imprimante installée, l'impression affiche un message clair ; l'aperçu et l'export PDF fonctionnent toujours.
- Les icônes sont des emojis : leur rendu dépend des polices du système (Windows/macOS : couleur ; certains Linux :
  installer `fonts-noto-color-emoji`).
- Vérification effectuée pendant le développement : compilation complète, tests de la couche métier (commande →
  table → paiement → ticket → PDF, numérotation, droits, administration) et captures d'écran de tous les écrans en
  1366×768 et 1920×1080.
