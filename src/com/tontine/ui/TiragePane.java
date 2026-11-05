package com.tontine.ui;

import java.util.List;
import java.util.Map;

import com.tontine.model.Tirage;
import com.tontine.model.Tontine;
import com.tontine.model.Utilisateur;
import com.tontine.service.CycleService;
import com.tontine.service.MembreService;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Tontine a tirage au sort : resultats des tirages (traces et non modifiables sans audit). */
public class TiragePane extends VBox {

    public TiragePane(Utilisateur u, Tontine t) {
        super(14);
        Label titre = new Label("Tirages au sort");
        titre.getStyleClass().add("carte-titre");
        getChildren().addAll(titre, Composants.sousTitre("\u00c0 chaque tour, le b\u00e9n\u00e9ficiaire est tir\u00e9 au sort parmi les membres "
                + "qui n'ont pas encore \u00e9t\u00e9 tir\u00e9s dans le tour en cours. Chaque r\u00e9sultat est enregistr\u00e9 "
                + "et ne peut \u00eatre corrig\u00e9 que par l'administrateur, avec une trace dans le journal d'audit."));

        Map<Integer, String> noms = new MembreService().nomsMembres(u.getId(), t.getId());
        List<Tirage> tirages = new CycleService().listerTirages(u.getId(), t.getId());
        boolean dejaTire = false;
        if (tirages.isEmpty()) {
            getChildren().add(Composants.sousTitre("Aucun tirage n'a encore eu lieu."));
        }
        for (int i = tirages.size() - 1; i >= 0; i--) {
            Tirage ti = tirages.get(i);
            boolean moi = ti.getBeneficiaireId() == u.getId();
            dejaTire |= moi;
            Label cycle = new Label("Tour " + ti.getSemaine());
            cycle.getStyleClass().add("badge-type");
            Label nom = new Label(noms.getOrDefault(ti.getBeneficiaireId(), "?") + (moi ? " (vous)" : ""));
            nom.getStyleClass().add("carte-titre");
            Label date = new Label(Aff.date(ti.getDate()));
            date.getStyleClass().add("pied");
            HBox ligne = new HBox(14, cycle, nom, Composants.espaceur(), date);
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.getStyleClass().add("ligne-liste");
            getChildren().add(ligne);
        }
        if (dejaTire) {
            Label l = new Label("Vous avez d\u00e9j\u00e0 \u00e9t\u00e9 tir\u00e9 au sort.");
            l.getStyleClass().add("message-succes");
            getChildren().add(0, l);
        }
    }
}
