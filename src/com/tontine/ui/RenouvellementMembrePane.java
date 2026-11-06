package com.tontine.ui;

import com.tontine.model.Renouvellement;
import com.tontine.model.StatutRenouvellement;
import com.tontine.model.Tontine;
import com.tontine.model.Utilisateur;
import com.tontine.service.AdhesionService;
import com.tontine.service.ConsultationService;
import com.tontine.service.Libelles;
import com.tontine.service.RenouvellementService;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Le membre recoit l'alerte de relance du gerant : il valide (il continue) ou refuse (il est retire de la tontine). */
public class RenouvellementMembrePane extends VBox {

    private final Utilisateur u;
    private final Tontine t;
    private final RenouvellementService service = new RenouvellementService();
    private String banniere;

    public RenouvellementMembrePane(Utilisateur u, Tontine t) {
        super(16);
        this.u = u;
        this.t = t;
        construire();
    }

    private void construire() {
        getChildren().clear();
        if (banniere != null) {
            Label b = Composants.message();
            Composants.montrer(b, banniere, false);
            getChildren().add(b);
            banniere = null;
        }
        Tontine tf = new ConsultationService().detail(t.getId());
        boolean terminee = new AdhesionService().estTerminee(tf);
        Label titre = new Label("Renouvellement de la tontine");
        titre.getStyleClass().add("carte-titre");
        Renouvellement r = terminee ? service.monAlerte(u.getId(), t.getId()) : null;

        Label msg = Composants.message();
        String explication;
        VBox carte;
        if (!terminee) {
            explication = "Quand la tontine sera termin\u00e9e, le g\u00e9rant vous enverra une alerte : vous pourrez alors valider "
                    + "(continuer) ou refuser (quitter la tontine).";
            carte = Composants.carte(titre, Composants.sousTitre(explication));
        } else if (r == null) {
            explication = "Le g\u00e9rant ne vous a pas encore envoy\u00e9 d'alerte de renouvellement. Vous serez pr\u00e9venu(e) par notification.";
            carte = Composants.carte(titre, Composants.sousTitre(explication));
        } else if (r.getStatut() == StatutRenouvellement.EN_ATTENTE) {
            explication = "La tontine " + tf.getNom() + " est termin\u00e9e. Souhaitez-vous continuer au prochain " + Libelles.cycle(tf)
                    + " ? Si vous validez, vous restez membre. Si vous refusez, ou si vous ne r\u00e9pondez pas avant la relance, vous serez retir\u00e9(e) automatiquement de la tontine.";
            Button valider = Composants.primaire("Je continue (valider)");
            Button refuser = Composants.contour("Je ne continue pas (refuser)");
            valider.setOnAction(e -> repondre(msg, true));
            refuser.setOnAction(e -> {
                if (Nav.confirmer("Refuser de continuer ? Vous serez retir\u00e9(e) de la tontine.")) {
                    repondre(msg, false);
                }
            });
            HBox boutons = new HBox(12, valider, refuser);
            carte = Composants.carte(titre, Composants.sousTitre(explication), msg, boutons);
        } else {
            explication = "Vous avez r\u00e9pondu : vous continuez la tontine. Le g\u00e9rant relancera le prochain " + Libelles.cycle(tf) + ".";
            carte = Composants.carte(titre, Composants.sousTitre(explication));
        }
        getChildren().add(carte);
    }

    private void repondre(Label msg, boolean valider) {
        Composants.cacher(msg);
        try {
            service.repondre(u.getId(), t.getId(), valider);
            banniere = valider ? "Merci : vous continuez la tontine." : "Vous avez refus\u00e9 : vous ne faites plus partie de la tontine.";
            construire();
        } catch (RuntimeException ex) {
            Composants.montrer(msg, Nav.messageErreur(ex), true);
        }
    }
}
