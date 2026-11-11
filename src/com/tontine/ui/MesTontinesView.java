package com.tontine.ui;

import java.util.List;

import com.tontine.model.Adhesion;
import com.tontine.model.Tontine;
import com.tontine.model.Utilisateur;
import com.tontine.service.ConsultationService;
import com.tontine.service.Format;
import com.tontine.service.Libelles;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Liste des tontines dont l'utilisateur est membre : point d'entree de l'espace membre. */
public class MesTontinesView extends VBox {

    public MesTontinesView(Utilisateur utilisateur, Fenetre fenetre) {
        super(14);
        ConsultationService consultation = new ConsultationService();
        List<Tontine> liste = consultation.mesTontines(utilisateur.getId());
        if (liste.isEmpty()) {
            Label titre = new Label("Vous n'\u00eates membre d'aucune tontine pour le moment");
            titre.getStyleClass().add("carte-titre");
            getChildren().add(Composants.carte(titre, Composants.sousTitre(
                    "Utilisez \u00ab Explorer les tontines \u00bb pour envoyer une demande d'adh\u00e9sion. "
                            + "Vous pourrez cotiser d\u00e8s que le g\u00e9rant l'aura accept\u00e9e.")));
            return;
        }
        getChildren().add(Composants.sousTitre("Choisissez une tontine pour cotiser, consulter l'historique, "
                + "demander un pr\u00eat ou suivre votre cagnotte."));
        for (Tontine t : liste) {
            Adhesion a = consultation.monAdhesion(utilisateur.getId(), t.getId());
            Label nom = new Label(t.getNom());
            nom.getStyleClass().add("carte-titre");
            Label detail = Composants.sousTitre(Libelles.type(t.getType()) + " - cotisation "
                    + Libelles.cotisation(t) + " (" + Libelles.frequence(t.getFrequence()) + ")");
            Label role = new Label(a == null ? "" : Libelles.role(a.getRole()));
            role.getStyleClass().add("badge");
            Button ouvrir = Composants.primaire("Ouvrir");
            ouvrir.setOnAction(e -> fenetre.afficher(t.getNom(), new TontineMembreView(utilisateur, t, fenetre)));
            HBox ligne = new HBox(12, new VBox(4, nom, detail), Composants.espaceur(), role, ouvrir);
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.getStyleClass().add("ligne-liste");
            getChildren().add(ligne);
        }
    }
}
