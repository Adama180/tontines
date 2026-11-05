package com.tontine.ui;

import com.tontine.service.Libelles;
import java.util.List;
import java.util.Map;

import com.tontine.model.Adhesion;
import com.tontine.model.Distribution;
import com.tontine.model.Tontine;
import com.tontine.model.Utilisateur;
import com.tontine.service.ConsultationService;
import com.tontine.service.Format;
import com.tontine.service.MembreService;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** "Recevoir la cagnotte" : versements recus par le membre et ce qu'il peut encore recevoir. */
public class CagnottePane extends VBox {

    public CagnottePane(Utilisateur u, Tontine tontine) {
        super(16);
        ConsultationService consultation = new ConsultationService();
        MembreService membre = new MembreService();
        Tontine t = consultation.detail(tontine.getId());
        Adhesion a = consultation.monAdhesion(u.getId(), t.getId());

        List<Distribution> versements = membre.mesVersements(u.getId(), t.getId());
        double total = 0;
        for (Distribution d : versements) {
            total += d.getMontantTotal();
        }

        FlowPane cartes = new FlowPane(14, 14);
        cartes.getChildren().add(stat(Format.montant(total), "Total re\u00e7u dans cette tontine"));
        cartes.getChildren().add(stat(Format.montant(a == null ? 0 : a.getSolde()), "Mon solde actuel (cotisations en attente de versement)"));
        cartes.getChildren().add(stat(String.valueOf(versements.size()), "Versement(s) re\u00e7u(s)"));
        getChildren().add(cartes);

        String explication;
        if (t.isFondsCommun()) {
            explication = "En fin de " + Libelles.cycle(t) + ", vous recevez votre cotisation cumul\u00e9e plus une part \u00e9gale des int\u00e9r\u00eats et des p\u00e9nalit\u00e9s collect\u00e9s.";
        } else if (t.isTourDeRole()) {
            explication = "Vous recevez la cagnotte du " + Libelles.cycle(t) + " quand c'est votre tour (voir l'onglet \u00ab Tour de r\u00f4le \u00bb).";
        } else {
            explication = "Vous recevez la cagnotte du " + Libelles.cycle(t) + " si vous \u00eates tir\u00e9 au sort (voir l'onglet \u00ab Tirage au sort \u00bb).";
        }
        getChildren().add(Composants.sousTitre(explication));

        Label titre = new Label("Mes versements");
        titre.getStyleClass().add("carte-titre");
        getChildren().add(titre);
        if (versements.isEmpty()) {
            getChildren().add(Composants.sousTitre("Vous n'avez pas encore re\u00e7u de versement dans cette tontine."));
        }
        Map<Integer, Integer> numeros = membre.numerosDeCycle(t.getId());
        for (Distribution d : versements) {
            Label cycle = new Label(Libelles.cycleMaj(t) + " " + numeros.getOrDefault(d.getCycleId(), 0));
            cycle.getStyleClass().add("badge-type");
            Label montant = new Label(Format.montant(d.getMontantTotal()));
            montant.getStyleClass().add("carte-titre");
            Label date = new Label(Aff.date(d.getDate()));
            date.getStyleClass().add("pied");
            HBox ligne = new HBox(14, cycle, montant, Composants.espaceur(), date);
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.getStyleClass().add("ligne-liste");
            getChildren().add(ligne);
        }
    }

    private VBox stat(String valeur, String libelle) {
        Label v = new Label(valeur);
        v.getStyleClass().add("stat-valeur");
        v.setWrapText(true);
        Label l = new Label(libelle);
        l.getStyleClass().add("stat-libelle");
        l.setWrapText(true);
        VBox c = new VBox(6, v, l);
        c.getStyleClass().add("stat-card");
        c.setPrefWidth(250);
        return c;
    }
}
