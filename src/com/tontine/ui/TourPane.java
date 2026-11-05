package com.tontine.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.tontine.model.Adhesion;
import com.tontine.model.DemandeTourAnticipe;
import com.tontine.model.StatutAdhesion;
import com.tontine.model.StatutDemande;
import com.tontine.model.Tontine;
import com.tontine.model.Utilisateur;
import com.tontine.service.AdhesionService;
import com.tontine.service.ConsultationService;
import com.tontine.service.CycleService;
import com.tontine.service.LigneMembre;
import com.tontine.service.MembreService;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Tontine a tour de role : ordre des beneficiaires et demande de tour anticipe. */
public class TourPane extends VBox {

    private final Utilisateur u;
    private final Tontine t;
    private final CycleService cycles = new CycleService();
    private String banniere;
    private final TextField motif = new TextField();
    private Button boutonDemander;

    public TourPane(Utilisateur u, Tontine t) {
        super(16);
        this.u = u;
        this.t = t;
        construire();
    }

    private void construire() {
        getChildren().clear();
        Tontine tf = new ConsultationService().detail(t.getId());
        if (banniere != null) {
            Label b = Composants.message();
            Composants.montrer(b, banniere, false);
            getChildren().add(b);
            banniere = null;
        }

        // ---- ordre des beneficiaires
        List<LigneMembre> actifs = new ArrayList<>();
        for (LigneMembre m : new AdhesionService().listerMembres(u.getId(), t.getId())) {
            if (m.getAdhesion().getStatut() == StatutAdhesion.ACTIF) {
                actifs.add(m);
            }
        }
        actifs.sort(Comparator.comparing((LigneMembre m) -> m.getAdhesion().getOrdreTour() == null
                ? Integer.MAX_VALUE : m.getAdhesion().getOrdreTour()));
        int prochain = actifs.isEmpty() ? -1 : tf.getTourActuelIndex() % actifs.size();

        Label titre = new Label("Ordre des b\u00e9n\u00e9ficiaires");
        titre.getStyleClass().add("carte-titre");
        getChildren().addAll(titre, Composants.sousTitre("\u00c0 chaque tour, un membre re\u00e7oit le total collect\u00e9. "
                + "Chacun continue ensuite \u00e0 cotiser normalement, sans dette."));
        for (int i = 0; i < actifs.size(); i++) {
            Adhesion a = actifs.get(i).getAdhesion();
            Label rang = new Label(String.valueOf(i + 1));
            rang.getStyleClass().add(i == prochain ? "badge" : "badge-type");
            Label nom = new Label(actifs.get(i).getUtilisateur().getNomComplet() + (a.getUserId() == u.getId() ? " (vous)" : ""));
            nom.getStyleClass().add("carte-titre");
            HBox ligne = new HBox(14, rang, nom, Composants.espaceur());
            ligne.setAlignment(Pos.CENTER_LEFT);
            if (i == prochain) {
                Label p = new Label("Prochain b\u00e9n\u00e9ficiaire");
                p.getStyleClass().add("badge");
                ligne.getChildren().add(p);
            }
            ligne.getStyleClass().add("ligne-liste");
            getChildren().add(ligne);
        }

        // ---- demande de tour anticipe
        Label t2 = new Label("Demander un tour anticip\u00e9");
        t2.getStyleClass().add("carte-titre");
        motif.clear();
        motif.setPromptText("Pourquoi avez-vous besoin de recevoir plus t\u00f4t ?");
        Label msg = Composants.message();
        boutonDemander = Composants.primaire("Envoyer ma demande au g\u00e9rant");
        boutonDemander.setOnAction(e -> {
            Composants.cacher(msg);
            try {
                cycles.demanderTourAnticipe(u.getId(), t.getId(), motif.getText());
                banniere = "Votre demande de tour anticip\u00e9 a \u00e9t\u00e9 envoy\u00e9e au g\u00e9rant.";
                construire();
            } catch (RuntimeException ex) {
                Composants.montrer(msg, Nav.messageErreur(ex), true);
            }
        });
        getChildren().add(Composants.carte(t2, Composants.sousTitre("Si le g\u00e9rant accepte, vous passez avant le prochain "
                + "b\u00e9n\u00e9ficiaire. Cela se fait entre deux tours."), Composants.champ("Motif", motif), msg, boutonDemander));

        // ---- mes demandes
        List<DemandeTourAnticipe> mes = new MembreService().mesDemandesTourAnticipe(u.getId(), t.getId());
        if (!mes.isEmpty()) {
            Label t3 = new Label("Mes demandes de tour anticip\u00e9");
            t3.getStyleClass().add("carte-titre");
            getChildren().add(t3);
            for (DemandeTourAnticipe d : mes) {
                Label m = new Label(d.getMotif());
                m.setWrapText(true);
                Label date = new Label(Aff.date(d.getDateDemande()));
                date.getStyleClass().add("pied");
                HBox ligne = new HBox(12, new VBox(4, m, date), Composants.espaceur(), Aff.badgeStatut(d.getStatut()));
                ligne.setAlignment(Pos.CENTER_LEFT);
                ligne.getStyleClass().add("ligne-liste");
                getChildren().add(ligne);
            }
        }
    }

    // acces pour les tests automatiques
    void remplirMotif(String texte) {
        motif.setText(texte);
    }

    Button getBoutonDemander() {
        return boutonDemander;
    }
}
