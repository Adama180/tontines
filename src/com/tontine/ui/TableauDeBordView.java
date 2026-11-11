package com.tontine.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.tontine.model.Adhesion;
import com.tontine.model.Tontine;
import com.tontine.model.Utilisateur;
import com.tontine.service.AdminService;
import com.tontine.service.AuthService;
import com.tontine.service.ConsultationService;
import com.tontine.service.Format;
import com.tontine.service.Indicateurs;
import com.tontine.service.Libelles;
import com.tontine.service.NotificationService;
import com.tontine.service.StatistiquesService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Espace connecte : barre laterale (menu selon le role) + zone de contenu.
 * Sur une fenetre etroite, la barre laterale se replie et s'ouvre avec le bouton de menu.
 */
public class TableauDeBordView extends BorderPane implements Fenetre {

    private static final double SEUIL_ETROIT = 980;

    private final Utilisateur utilisateur;
    private final VBox menu = new VBox(2);
    private final List<Button> boutons = new ArrayList<>();
    private final Map<String, Button> parLibelle = new LinkedHashMap<>();
    private final StackPane contenu = new StackPane();
    private final Label titrePage = new Label();
    private final Hyperlink lienNotifications = new Hyperlink();
    private final Button boutonMenu = new Button();
    private VBox barre;
    private boolean etroit;
    private boolean barreOuverte;

    public TableauDeBordView(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
        getStyleClass().add("page");
        barre = barreLaterale();
        setCenter(zoneCentrale());
        majBarre();

        if (utilisateur.isAdmin()) {
            ajouterMenu("Accueil", this::accueilAdmin);
            ajouterMenu("Demandes de cr\u00e9ation", () -> new DemandesCreationAdminPane(utilisateur));
            ajouterMenu("Utilisateurs", () -> new UtilisateursAdminPane(utilisateur));
            ajouterMenu("Tontines", () -> new TontinesAdminPane(utilisateur));
            ajouterMenu("Pr\u00eats des g\u00e9rants", () -> new PretsGerantsAdminPane(utilisateur));
            ajouterMenu("Paiements des g\u00e9rants", () -> new PaiementsGerantsAdminPane(utilisateur));
            ajouterMenu("Tirages au sort", () -> new TiragesAdminPane(utilisateur));
            ajouterMenu("Versements", () -> new VersementsAdminPane(utilisateur));
            ajouterMenu("Journal d'audit", () -> new JournalAuditPane(utilisateur));
        } else {
            ajouterMenu("Accueil", this::accueilUtilisateur);
            ajouterMenu("Explorer les tontines", () -> new ExplorerPane(utilisateur, this));
            ajouterMenu("Cr\u00e9er ma tontine", () -> new CreerTontineView(utilisateur, this));
            ajouterMenu("Mes demandes", () -> new MesDemandesView(utilisateur));
            ajouterMenu("Mes tontines", () -> new MesTontinesView(utilisateur, this));
        }
        ajouterMenu("Notifications", () -> new NotificationsView(utilisateur, this::rafraichirNotifications));
        ajouterMenu("Mon profil", () -> new ProfilView(utilisateur));
        boutons.get(0).fire();

        Adaptatif.surLargeur(this, SEUIL_ETROIT, e -> {
            etroit = e;
            barreOuverte = false;
            Adaptatif.classeCompact(this, e);
            majBarre();
        });
    }

    // ------------------------------------------------------------------ structure

    private VBox barreLaterale() {
        Label nom = new Label(utilisateur.getNomComplet());
        nom.getStyleClass().add("sidebar-nom");
        nom.setWrapText(true);
        Label email = new Label(utilisateur.getEmail());
        email.getStyleClass().add("sidebar-info");
        email.setWrapText(true);
        Label badge = new Label(utilisateur.isAdmin() ? "Administrateur" : "Utilisateur");
        badge.getStyleClass().add("badge");
        HBox profil = new HBox(12, Composants.avatar(utilisateur.getNomComplet(), 42), new VBox(3, nom, email, badge));
        profil.setAlignment(Pos.CENTER_LEFT);
        profil.getStyleClass().add("sidebar-profil");

        Button deconnexion = new Button("D\u00e9connexion");
        deconnexion.getStyleClass().add("menu-btn");
        deconnexion.setGraphic(Icones.icone("sortie", 20));
        deconnexion.setMaxWidth(Double.MAX_VALUE);
        deconnexion.setOnAction(e -> {
            new AuthService().deconnecter();
            Nav.aller(new AccueilView());
        });

        Region espace = new Region();
        VBox.setVgrow(espace, Priority.ALWAYS);
        ScrollPane menuDefilant = new ScrollPane(menu);
        menuDefilant.setFitToWidth(true);
        menuDefilant.getStyleClass().add("defilement");
        VBox.setVgrow(menuDefilant, Priority.ALWAYS);
        VBox b = new VBox(14, Composants.logo(true), profil, menuDefilant, deconnexion);
        b.getStyleClass().add("sidebar");
        b.setPrefWidth(262);
        b.setMinWidth(262);
        b.setMaxWidth(262);
        return b;
    }

    private Node zoneCentrale() {
        titrePage.getStyleClass().add("titre");
        boutonMenu.getStyleClass().add("btn-icone");
        boutonMenu.setGraphic(Icones.icone("menu", 20));
        boutonMenu.setVisible(false);
        boutonMenu.setManaged(false);
        boutonMenu.setOnAction(e -> {
            barreOuverte = !barreOuverte;
            majBarre();
        });

        lienNotifications.getStyleClass().add("lien-notif");
        lienNotifications.setGraphic(Icones.icone("cloche", 18));
        lienNotifications.setOnAction(e -> {
            Button b = parLibelle.get("Notifications");
            if (b != null) {
                b.fire();
            }
        });
        rafraichirNotifications();
        HBox entete = new HBox(12, boutonMenu, titrePage, Composants.espaceur(), lienNotifications,
                Composants.avatar(utilisateur.getNomComplet(), 34));
        entete.setAlignment(Pos.CENTER_LEFT);
        entete.getStyleClass().add("entete-page");

        ScrollPane defilement = new ScrollPane(contenu);
        defilement.setFitToWidth(true);
        defilement.getStyleClass().add("defilement");
        contenu.setPadding(new Insets(24, 30, 30, 30));
        contenu.setAlignment(Pos.TOP_LEFT);

        BorderPane zone = new BorderPane();
        zone.setTop(entete);
        zone.setCenter(defilement);
        return zone;
    }

    /** Barre laterale visible si la fenetre est large, ou si l'utilisateur l'a ouverte sur une fenetre etroite. */
    private void majBarre() {
        setLeft(!etroit || barreOuverte ? barre : null);
        boutonMenu.setVisible(etroit);
        boutonMenu.setManaged(etroit);
        contenu.setPadding(etroit ? new Insets(16, 14, 20, 14) : new Insets(24, 30, 30, 30));
    }

    /** Met a jour le compteur de notifications non lues affiche en haut a droite. */
    public void rafraichirNotifications() {
        try {
            int n = NotificationService.compterNonLues(utilisateur.getId());
            lienNotifications.setText(n == 0 ? "Aucune nouvelle notification" : n + " notification(s) non lue(s)");
            lienNotifications.getStyleClass().remove("lien-notif-actif");
            if (n > 0) {
                lienNotifications.getStyleClass().add("lien-notif-actif");
            }
        } catch (RuntimeException e) {
            lienNotifications.setText("");
        }
    }

    /** Ajoute une entree au menu : la vue est construite a chaque clic (donnees toujours a jour). */
    public void ajouterMenu(String libelle, Supplier<Node> vue) {
        Button b = new Button(libelle);
        b.getStyleClass().add("menu-btn");
        b.setGraphic(Icones.icone(iconePour(libelle), 20));
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> {
            for (Button autre : boutons) {
                autre.getStyleClass().remove("menu-btn-actif");
            }
            b.getStyleClass().add("menu-btn-actif");
            Node noeud;
            try {
                noeud = vue.get();
            } catch (RuntimeException ex) {
                Label erreur = new Label(Nav.messageErreur(ex));
                erreur.setWrapText(true);
                erreur.getStyleClass().add("message-erreur");
                noeud = erreur;
            }
            afficher(libelle, noeud);
            rafraichirNotifications();
            if (etroit && barreOuverte) {
                barreOuverte = false;
                majBarre();
            }
        });
        boutons.add(b);
        parLibelle.put(libelle, b);
        menu.getChildren().add(b);
    }

    private static String iconePour(String libelle) {
        switch (libelle) {
        case "Accueil":
            return "accueil";
        case "Explorer les tontines":
            return "recherche";
        case "Cr\u00e9er ma tontine":
            return "ajouter";
        case "Mes demandes":
        case "Demandes de cr\u00e9ation":
            return "liste";
        case "Mes tontines":
        case "Tontines":
            return "groupe";
        case "Utilisateurs":
            return "personne";
        case "Pr\u00eats des g\u00e9rants":
            return "paiement";
        case "Paiements des g\u00e9rants":
            return "portefeuille";
        case "Tirages au sort":
            return "de";
        case "Versements":
            return "graphique";
        case "Journal d'audit":
            return "historique";
        case "Notifications":
            return "cloche";
        case "Mon profil":
            return "reglages";
        default:
            return "fleche";
        }
    }

    /** Affiche un ecran dans la zone de contenu (utilise par les ecrans qui en ouvrent un autre). */
    @Override
    public void afficher(String titre, Node noeud) {
        titrePage.setText(titre);
        contenu.getChildren().setAll(noeud);
        Effets.fondu(noeud);
    }

    // ------------------------------------------------------------------ pages

    private Node accueilUtilisateur() {
        ConsultationService consultation = new ConsultationService();
        VBox page = new VBox(18);

        List<Tontine> mesTontines = consultation.mesTontines(utilisateur.getId());
        page.getChildren().add(Composants.banniere("Bonjour, " + utilisateur.getPrenom() + " !",
                mesTontines.isEmpty() ? "Bienvenue sur NAT Tontines : trouvez ou cr\u00e9ez votre premi\u00e8re tontine."
                        : "Vous participez \u00e0 " + mesTontines.size() + " tontine" + (mesTontines.size() > 1 ? "s" : "")
                                + ". Voici vos tontines."));

        if (mesTontines.isEmpty()) {
            // etat normal et prevu (cahier des charges, section 3.2) : ni erreur ni ecran vide
            Button explorer = Composants.primaire("Explorer les tontines");
            explorer.setOnAction(e -> parLibelle.get("Explorer les tontines").fire());
            Button creer = Composants.contour("Cr\u00e9er ma tontine");
            creer.setOnAction(e -> parLibelle.get("Cr\u00e9er ma tontine").fire());
            page.getChildren().add(Composants.etatVide("groupe", "Vous n'\u00eates membre d'aucune tontine pour le moment",
                    "Explorez les tontines disponibles et envoyez une demande d'adh\u00e9sion, "
                            + "ou demandez \u00e0 devenir g\u00e9rant pour cr\u00e9er la v\u00f4tre. Vous pourrez cotiser "
                            + "d\u00e8s que le g\u00e9rant aura accept\u00e9 votre demande.", explorer, creer));
        } else {
            Label titre = new Label("Mes tontines");
            titre.getStyleClass().add("carte-titre");
            page.getChildren().add(titre);
            for (Tontine t : mesTontines) {
                Adhesion a = consultation.monAdhesion(utilisateur.getId(), t.getId());
                Label nom = new Label(t.getNom());
                nom.getStyleClass().add("carte-titre");
                Label detail = Composants.sousTitre(Libelles.type(t.getType()) + " - cotisation "
                        + Libelles.cotisation(t) + " (" + Libelles.frequence(t.getFrequence()) + ")");
                Label role = new Label(a == null ? "" : Libelles.role(a.getRole()));
                role.getStyleClass().add("badge");
                Button ouvrir = Composants.primaire("Ouvrir");
                ouvrir.setOnAction(e -> afficher(t.getNom(), new TontineMembreView(utilisateur, t, this)));
                role.setMinWidth(Region.USE_PREF_SIZE);
                ouvrir.setMinWidth(Region.USE_PREF_SIZE);
                HBox ligne = new HBox(12, new VBox(4, nom, detail), Composants.espaceur(), role, ouvrir);
                ligne.setAlignment(Pos.CENTER_LEFT);
                ligne.getStyleClass().add("ligne-liste");
                page.getChildren().add(ligne);
            }
        }
        return page;
    }

    private Node accueilAdmin() {
        Indicateurs stats = new StatistiquesService().globales(utilisateur.getId());
        FlowPane cartes = new FlowPane(16, 16);
        int i = 0;
        for (Map.Entry<String, String> e : stats.getValeurs().entrySet()) {
            Label valeur = new Label(e.getValue());
            valeur.getStyleClass().add("stat-valeur");
            valeur.setWrapText(true);
            String cle = e.getKey().trim();
            if (cle.startsWith("- ")) {
                cle = "Tontines de type " + cle.substring(2).toLowerCase();
            }
            Label libelle = new Label(cle);
            libelle.getStyleClass().add("stat-libelle");
            libelle.setWrapText(true);
            VBox c = new VBox(6, valeur, libelle);
            c.getStyleClass().add("stat-card");
            if (i % 3 == 1) {
                c.getStyleClass().add("stat-card-or");
            } else if (i % 3 == 2) {
                c.getStyleClass().add("stat-card-teal");
            }
            i++;
            c.setPrefWidth(250);
            cartes.getChildren().add(c);
        }
        // ---- a traiter : acces direct aux files d'attente de l'administrateur
        AdminService admin = new AdminService();
        int demandes = admin.demandesCreationEnAttente(utilisateur.getId()).size();
        int prets = admin.pretsGerantsEnAttente(utilisateur.getId()).size();
        int paiements = admin.paiementsGerantsEnAttente(utilisateur.getId()).size();
        Label aTraiter = new Label("\u00c0 traiter");
        aTraiter.getStyleClass().add("carte-titre");
        FlowPane raccourcis = new FlowPane(12, 12,
                raccourci("Demandes de cr\u00e9ation", demandes),
                raccourci("Pr\u00eats des g\u00e9rants", prets),
                raccourci("Paiements des g\u00e9rants", paiements));
        Label activite = new Label("Activit\u00e9 globale de la plateforme");
        activite.getStyleClass().add("carte-titre");
        return new VBox(18, Composants.banniere("Tableau de bord administrateur",
                "Supervisez les demandes, les pr\u00eats, les versements et l'activit\u00e9 de toutes les tontines."),
                aTraiter, raccourcis, activite, cartes);
    }

    /** Bouton d'acces direct a une file d'attente, mis en avant s'il y a quelque chose a traiter. */
    private Button raccourci(String menuLibelle, int nombre) {
        Button b = nombre > 0 ? Composants.or(menuLibelle + " (" + nombre + ")") : Composants.contour(menuLibelle + " (0)");
        b.setOnAction(e -> parLibelle.get(menuLibelle).fire());
        return b;
    }

    // acces pour les tests automatiques
    boolean estEtroit() {
        return etroit;
    }

    boolean barreVisible() {
        return getLeft() != null;
    }

    Button getBoutonMenu() {
        return boutonMenu;
    }
}
