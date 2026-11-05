package com.tontine.ui;

import java.util.List;
import java.util.Map;

import com.tontine.model.Adhesion;
import com.tontine.model.StatutAdhesion;
import com.tontine.model.Tontine;
import com.tontine.model.Transaction;
import com.tontine.model.TypeTransaction;
import com.tontine.model.Utilisateur;
import com.tontine.service.AdhesionService;
import com.tontine.service.ConsultationService;
import com.tontine.service.CotisationService;
import com.tontine.service.Libelles;
import com.tontine.service.Format;
import com.tontine.service.LigneMembre;
import com.tontine.service.MembreService;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** Paiements en especes : enregistrer une cotisation recue, confirmer ou refuser les paiements en attente. */
public class EspecesPane extends PaneGestion {

    private final Utilisateur gerant;
    private final Tontine tontine;
    private final CotisationService cotisations = new CotisationService();
    private final ComboBox<LigneMembre> choixMembre = new ComboBox<>();
    private final TextField montant = new TextField();
    private Button boutonEnregistrer;

    public EspecesPane(Utilisateur gerant, Tontine tontine) {
        this.gerant = gerant;
        this.tontine = tontine;
        choixMembre.setConverter(new StringConverter<LigneMembre>() {
            @Override
            public String toString(LigneMembre m) {
                return m == null ? "" : m.getUtilisateur().getNomComplet();
            }

            @Override
            public LigneMembre fromString(String s) {
                return null;
            }
        });
        choixMembre.setMaxWidth(Double.MAX_VALUE);
        rafraichir();
    }

    @Override
    protected void construire() {
        Tontine t = new ConsultationService().detail(tontine.getId());
        Map<Integer, String> noms = new MembreService().nomsMembres(gerant.getId(), t.getId());
        boolean cycle = new MembreService().cycleEnCours(t.getId()) != null;

        // ---- enregistrer une cotisation recue en especes
        choixMembre.getItems().clear();
        for (LigneMembre m : new AdhesionService().listerMembres(gerant.getId(), t.getId())) {
            Adhesion a = m.getAdhesion();
            if (a.getStatut() == StatutAdhesion.ACTIF && a.getUserId() != gerant.getId()) {
                choixMembre.getItems().add(m);
            }
        }
        montant.setText(Aff.nombre(t.getMontantCotisation()));
        montant.setEditable(t.isFondsCommun());
        boutonEnregistrer = Composants.primaire("Enregistrer la cotisation");
        boutonEnregistrer.setOnAction(e -> action(() -> {
            LigneMembre m = choixMembre.getValue();
            if (m == null) {
                erreur("Choisissez le membre qui vous a remis l'argent.");
                return;
            }
            double somme = Saisie.montant(montant.getText(), "Montant");
            cotisations.enregistrerCotisationEspece(gerant.getId(), t.getId(), m.getAdhesion().getUserId(), somme);
            succes("Cotisation de " + Format.montant(somme) + " enregistr\u00e9e pour " + m.getUtilisateur().getNomComplet() + ".");
        }));
        VBox formulaire = Composants.carte(titreSection("Enregistrer une cotisation re\u00e7ue en esp\u00e8ces"));
        if (!cycle) {
            formulaire.getChildren().add(vide("Aucun " + Libelles.cycle(t) + " en cours : d\u00e9marrez un " + Libelles.cycle(t) + " avant d'enregistrer une cotisation."));
        } else {
            formulaire.getChildren().addAll(
                    vide("Pour votre propre cotisation, payez par Wave ou Orange Money (un paiement en esp\u00e8ces de votre part est confirm\u00e9 par l'administrateur)."),
                    new HBox(14, Composants.champ("Membre", choixMembre), Composants.champ("Montant (FCFA)", montant)),
                    boutonEnregistrer);
            HBox.setHgrow(formulaire.getChildren().get(2), javafx.scene.layout.Priority.ALWAYS);
        }
        getChildren().add(formulaire);

        // ---- paiements en attente de confirmation
        List<Transaction> attente = cotisations.paiementsEnAttente(gerant.getId(), t.getId());
        getChildren().add(titreSection("Paiements en esp\u00e8ces \u00e0 confirmer (" + attente.size() + ")"));
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

    // acces pour les tests automatiques
    ComboBox<LigneMembre> getChoixMembre() { return choixMembre; }
    TextField getMontant() { return montant; }
    Button getBoutonEnregistrer() { return boutonEnregistrer; }
}
