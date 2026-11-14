package com.tontine.ui;

import java.util.List;
import java.util.Map;

import com.tontine.model.Tontine;
import com.tontine.model.Transaction;
import com.tontine.model.TypeTransaction;
import com.tontine.model.Utilisateur;
import com.tontine.service.ConsultationService;
import com.tontine.service.CotisationService;
import com.tontine.service.Format;
import com.tontine.service.MembreService;

import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

/** Anciens paiements en especes encore en attente (les especes ne sont plus acceptees) : le gerant les confirme ou les refuse. */
public class EspecesPane extends PaneGestion {

    private final Utilisateur gerant;
    private final Tontine tontine;
    private final CotisationService cotisations = new CotisationService();

    public EspecesPane(Utilisateur gerant, Tontine tontine) {
        this.gerant = gerant;
        this.tontine = tontine;
        rafraichir();
    }

    @Override
    protected void construire() {
        Tontine t = new ConsultationService().detail(tontine.getId());
        Map<Integer, String> noms = new MembreService().nomsMembres(gerant.getId(), t.getId());

        // ---- paiements en attente de confirmation
        List<Transaction> attente = cotisations.paiementsEnAttente(gerant.getId(), t.getId());
        getChildren().add(titreSection("Anciens paiements en esp\u00e8ces \u00e0 traiter (" + attente.size() + ")"));
        if (attente.isEmpty()) {
            getChildren().add(vide("Aucun paiement en attente."));
        }
        for (Transaction tx : attente) {
            boolean mien = tx.getUserId() == gerant.getId();
            String type = tx.getType() == TypeTransaction.REMBOURSEMENT ? "Remboursement de pr\u00eat" : "Cotisation";
            VBox infos = texte(noms.getOrDefault(tx.getUserId(), "?") + " : " + type + " de " + Format.montant(tx.getMontant()),
                    Aff.date(tx.getDate()) + (mien ? "\nVotre propre paiement : il doit \u00eatre confirm\u00e9 par l'administrateur." : ""));
            Button confirmer = Composants.primaire("Confirmer la r\u00e9ception");
            confirmer.setDisable(mien);
            confirmer.setOnAction(e -> action(() -> {
                if (Nav.confirmer("Confirmer que vous avez re\u00e7u " + Format.montant(tx.getMontant()) + " en esp\u00e8ces ?")) {
                    cotisations.confirmerPaiementEspece(gerant.getId(), tx.getId(), true);
                    succes("Paiement de " + Format.montant(tx.getMontant()) + " confirm\u00e9.");
                }
            }));
            Button refuser = Composants.contour("Refuser");
            refuser.setDisable(mien);
            refuser.setOnAction(e -> action(() -> {
                if (Nav.confirmer("Refuser ce paiement ? Il ne sera pas pris en compte.")) {
                    cotisations.confirmerPaiementEspece(gerant.getId(), tx.getId(), false);
                    succes("Paiement refus\u00e9.");
                }
            }));
            getChildren().add(ligne(infos, refuser, confirmer));
        }
    }
}
