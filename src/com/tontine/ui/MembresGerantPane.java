package com.tontine.ui;

import com.tontine.model.Adhesion;
import com.tontine.model.StatutAdhesion;
import com.tontine.model.Tontine;
import com.tontine.model.Utilisateur;
import com.tontine.service.AdhesionService;
import com.tontine.service.ConsultationService;
import com.tontine.service.Format;
import com.tontine.service.LigneMembre;
import com.tontine.service.Libelles;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Gestion des membres : coordonnees, soldes, retrait d'un membre. */
public class MembresGerantPane extends PaneGestion {

    private final Utilisateur gerant;
    private final Tontine tontine;
    private final AdhesionService adhesions = new AdhesionService();

    public MembresGerantPane(Utilisateur gerant, Tontine tontine) {
        this.gerant = gerant;
        this.tontine = tontine;
        rafraichir();
    }

    @Override
    protected void construire() {
        ConsultationService consultation = new ConsultationService();
        Tontine t = consultation.detail(tontine.getId());
        getChildren().add(vide(consultation.nombreMembres(t.getId()) + " membre(s) actif(s) sur " + t.getNbMembreMax()
                + ". Un membre ne peut \u00eatre retir\u00e9 que s'il n'a ni solde ni pr\u00eat en cours"
                + (t.isFondsCommun() ? "." : " et seulement entre deux tours.")));

        for (LigneMembre m : adhesions.listerMembres(gerant.getId(), t.getId())) {
            Adhesion a = m.getAdhesion();
            Utilisateur u = m.getUtilisateur();
            boolean actif = a.getStatut() == StatutAdhesion.ACTIF;
            VBox infos = texte(u.getNomComplet() + (a.getUserId() == gerant.getId() ? " (vous)" : ""),
                    u.getTelephone() + " - " + u.getEmail() + "\nTotal cotis\u00e9 " + Format.montant(a.getTotalCotise())
                            + " - solde " + Format.montant(a.getSolde()) + " - membre depuis le " + Aff.date(a.getDateAdhesion()));
            Label role = new Label(Libelles.role(a.getRole()));
            role.getStyleClass().add("badge");
            Label statut = new Label(actif ? "Actif" : "Retir\u00e9");
            statut.getStyleClass().addAll("badge-statut", actif ? "statut-acceptee" : "statut-refusee");
            ajouterLigne(infos, role, statut, a, actif);
        }
    }

    private void ajouterLigne(VBox infos, Label role, Label statut, Adhesion a, boolean actif) {
        javafx.scene.Node[] actions;
        if (actif && a.getUserId() != gerant.getId()) {
            Button retirer = Composants.contour("Retirer");
            retirer.setOnAction(e -> action(() -> {
                if (Nav.confirmer("Retirer ce membre de la tontine ?")) {
                    adhesions.desactiverMembre(gerant.getId(), a.getId());
                    succes("Le membre a \u00e9t\u00e9 retir\u00e9 de la tontine.");
                }
            }));
            actions = new javafx.scene.Node[] { role, statut, retirer };
        } else {
            actions = new javafx.scene.Node[] { role, statut };
        }
        getChildren().add(ligne(infos, actions));
    }
}
