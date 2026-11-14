package com.tontine.ui;

import java.util.List;

import com.tontine.model.StatutCompte;
import com.tontine.model.Utilisateur;
import com.tontine.model.DemandeReactivation;
import com.tontine.service.AdminService;
import com.tontine.service.ReactivationService;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Gestion des comptes : recherche, suspension et reactivation. */
public class UtilisateursAdminPane extends PaneGestion {

    private final Utilisateur admin;
    private final AdminService service = new AdminService();
    private final ReactivationService reactivations = new ReactivationService();
    private final com.tontine.dao.UtilisateurDao utilisateurs = new com.tontine.dao.UtilisateurDao();
    private final TextField recherche = new TextField();
    private final VBox liste = new VBox(12);
    private final Label resume = new Label();

    public UtilisateursAdminPane(Utilisateur admin) {
        this.admin = admin;
        recherche.setPromptText("Rechercher par nom, email ou t\u00e9l\u00e9phone");
        recherche.setMaxWidth(420);
        resume.getStyleClass().add("sous-titre");
        recherche.textProperty().addListener((o, a, b) -> remplir());
        rafraichir();
    }

    @Override
    protected void construire() {
        // demandes de reactivation envoyees par les utilisateurs suspendus
        List<DemandeReactivation> demandes = reactivations.demandesEnAttente(admin.getId());
        getChildren().add(titreSection("Demandes de r\u00e9activation (" + demandes.size() + ")"));
        if (demandes.isEmpty()) {
            getChildren().add(vide("Aucune demande de r\u00e9activation en attente."));
        }
        for (DemandeReactivation d : demandes) {
            Utilisateur u = utilisateurs.findById(d.getUserId());
            Button accepter = Composants.primaire("R\u00e9activer le compte");
            accepter.setOnAction(e -> action(() -> {
                reactivations.traiter(admin.getId(), d.getId(), true);
                succes("Le compte de " + u.getNomComplet() + " est r\u00e9activ\u00e9.");
            }));
            Button refuser = Composants.contour("Refuser");
            refuser.setOnAction(e -> action(() -> {
                reactivations.traiter(admin.getId(), d.getId(), false);
                succes("La demande de " + u.getNomComplet() + " a \u00e9t\u00e9 refus\u00e9e.");
            }));
            getChildren().add(ligne(texte(u.getNomComplet(), "Motif : " + d.getMotif() + " - " + Aff.date(d.getDateDemande())), refuser, accepter));
        }
        getChildren().addAll(titreSection("Tous les comptes"), recherche, resume, liste);
        remplir();
    }

    private void remplir() {
        liste.getChildren().clear();
        String q = recherche.getText() == null ? "" : recherche.getText().trim().toLowerCase();
        List<Utilisateur> tous = service.listerUtilisateurs(admin.getId());
        int n = 0;
        int suspendus = 0;
        for (Utilisateur u : tous) {
            if (u.getStatut() == StatutCompte.SUSPENDU) {
                suspendus++;
            }
            String texte = (u.getNomComplet() + " " + u.getEmail() + " " + u.getTelephone()).toLowerCase();
            if (!q.isEmpty() && !texte.contains(q)) {
                continue;
            }
            n++;
            boolean suspendu = u.getStatut() == StatutCompte.SUSPENDU;
            Label role = new Label(u.isAdmin() ? "Administrateur" : "Utilisateur");
            role.getStyleClass().add(u.isAdmin() ? "badge" : "badge-type");
            Label statut = new Label(suspendu ? "Suspendu" : "Actif");
            statut.getStyleClass().addAll("badge-statut", suspendu ? "statut-refusee" : "statut-acceptee");
            javafx.scene.Node[] actions;
            if (u.isAdmin()) {
                actions = new javafx.scene.Node[] { role, statut };
            } else if (suspendu) {
                Button reactiver = Composants.primaire("R\u00e9activer");
                reactiver.setOnAction(e -> action(() -> {
                    service.reactiverCompte(admin.getId(), u.getId());
                    succes("Le compte de " + u.getNomComplet() + " est r\u00e9activ\u00e9.");
                }));
                actions = new javafx.scene.Node[] { role, statut, reactiver };
            } else {
                Button suspendre = Composants.contour("Suspendre");
                suspendre.setOnAction(e -> action(() -> {
                    if (Nav.confirmer("Suspendre le compte de " + u.getNomComplet() + " ? Il ne pourra plus se connecter.")) {
                        service.suspendreCompte(admin.getId(), u.getId());
                        succes("Le compte de " + u.getNomComplet() + " est suspendu.");
                    }
                }));
                actions = new javafx.scene.Node[] { role, statut, suspendre };
            }
            liste.getChildren().add(ligne(texte(u.getNomComplet(), u.getEmail() + " - " + u.getTelephone()
                    + " - inscrit le " + Aff.date(u.getDateCreation())), actions));
        }
        resume.setText(tous.size() + " compte(s) dont " + suspendus + " suspendu(s)"
                + (q.isEmpty() ? "" : " - " + n + " r\u00e9sultat(s) pour \u00ab " + recherche.getText().trim() + " \u00bb"));
        if (n == 0) {
            liste.getChildren().add(vide("Aucun utilisateur ne correspond."));
        }
    }

    // acces pour les tests automatiques
    TextField getRecherche() {
        return recherche;
    }
}
