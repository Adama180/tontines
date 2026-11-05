package com.tontine.ui;

import java.util.List;

import com.tontine.model.DemandeAdhesion;
import com.tontine.model.DemandeCreation;
import com.tontine.model.Utilisateur;
import com.tontine.service.ConsultationService;
import com.tontine.service.DemandeService;
import com.tontine.service.Format;
import com.tontine.service.Libelles;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Suivi des demandes d'un utilisateur : adhesions et creations (en attente, acceptee, refusee). */
public class MesDemandesView extends VBox {

    public MesDemandesView(Utilisateur utilisateur) {
        super(14);
        DemandeService demandes = new DemandeService();
        ConsultationService consultation = new ConsultationService();

        // ---- demandes d'adhesion
        getChildren().add(titreSection("Mes demandes pour rejoindre une tontine"));
        List<DemandeAdhesion> adhesions = demandes.suivreDemandesAdhesion(utilisateur.getId());
        if (adhesions.isEmpty()) {
            getChildren().add(vide("Vous n'avez fait aucune demande d'adh\u00e9sion pour le moment."));
        }
        for (DemandeAdhesion d : adhesions) {
            String nomTontine;
            try {
                nomTontine = consultation.detail(d.getTontineId()).getNom();
            } catch (RuntimeException e) {
                nomTontine = "Tontine #" + d.getTontineId();
            }
            getChildren().add(ligne(nomTontine, "Envoy\u00e9e le " + Aff.date(d.getDateEnvoi()), Aff.badgeStatut(d.getStatut())));
        }

        // ---- demandes de creation
        getChildren().add(titreSection("Mes demandes de cr\u00e9ation de tontine"));
        List<DemandeCreation> creations = demandes.suivreDemandesCreation(utilisateur.getId());
        if (creations.isEmpty()) {
            getChildren().add(vide("Vous n'avez fait aucune demande de cr\u00e9ation pour le moment."));
        }
        for (DemandeCreation d : creations) {
            getChildren().add(ligne(d.getNomSouhaite(),
                    Libelles.type(d.getTypeSouhaite()) + " - " + Libelles.reglesDemande(d) + " - envoy\u00e9e le "
                            + Aff.date(d.getDateEnvoi()),
                    Aff.badgeStatut(d.getStatut())));
        }
    }

    private Label titreSection(String texte) {
        Label l = new Label(texte);
        l.getStyleClass().add("carte-titre");
        l.setPadding(new javafx.geometry.Insets(12, 0, 0, 0));
        return l;
    }

    private Label vide(String texte) {
        Label l = new Label(texte);
        l.getStyleClass().add("sous-titre");
        return l;
    }

    private HBox ligne(String titre, String detail, Label statut) {
        Label t = new Label(titre);
        t.getStyleClass().add("carte-titre");
        Label d = Composants.sousTitre(detail);
        HBox h = new HBox(12, new VBox(4, t, d), Composants.espaceur(), statut);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add("ligne-liste");
        return h;
    }
}
