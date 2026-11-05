package com.tontine.ui;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.tontine.model.Cycle;
import com.tontine.model.StatutAdhesion;
import com.tontine.model.Tirage;
import com.tontine.model.Tontine;
import com.tontine.model.Utilisateur;
import com.tontine.service.AdhesionService;
import com.tontine.service.AdminService;
import com.tontine.service.ConsultationService;
import com.tontine.service.LigneMembre;
import com.tontine.service.MembreService;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Tirages au sort de la plateforme. Un resultat n'est jamais modifie discretement : l'administrateur peut le corriger
 * tant que le versement n'a pas eu lieu, avec un motif obligatoire, une trace dans le journal d'audit et une
 * information a tous les membres.
 */
public class TiragesAdminPane extends PaneGestion {

    private final Utilisateur admin;
    private final AdminService service = new AdminService();
    private final MembreService membre = new MembreService();
    private final ComboBox<LigneMembre> nouveauBeneficiaire = new ComboBox<>();
    private final TextField motif = new TextField();
    private Button boutonEnregistrer;
    private Tirage tirageEnCorrection;

    public TiragesAdminPane(Utilisateur admin) {
        this.admin = admin;
        nouveauBeneficiaire.setConverter(new StringConverter<LigneMembre>() {
            @Override
            public String toString(LigneMembre m) {
                return m == null ? "" : m.getUtilisateur().getNomComplet();
            }

            @Override
            public LigneMembre fromString(String s) {
                return null;
            }
        });
        nouveauBeneficiaire.setMaxWidth(Double.MAX_VALUE);
        motif.setPromptText("Ex. : erreur de saisie, membre absent...");
        rafraichir();
    }

    @Override
    protected void construire() {
        boutonEnregistrer = null;
        ConsultationService consultation = new ConsultationService();
        Map<Integer, Utilisateur> users = service.listerUtilisateurs(admin.getId()).stream()
                .collect(Collectors.toMap(Utilisateur::getId, u -> u));
        List<Tirage> tirages = service.tousLesTirages(admin.getId());

        getChildren().add(vide("Un tirage peut \u00eatre corrig\u00e9 tant que le versement du tour n'est pas effectu\u00e9. "
                + "La correction exige un motif, est inscrite dans le journal d'audit et pr\u00e9vient les membres."));
        getChildren().add(titreSection("Tirages (" + tirages.size() + ")"));
        if (tirages.isEmpty()) {
            getChildren().add(vide("Aucun tirage au sort n'a encore eu lieu."));
        }
        for (Tirage ti : tirages) {
            Tontine t = consultation.detail(ti.getTontineId());
            Utilisateur gagnant = users.get(ti.getBeneficiaireId());
            Cycle c = membre.cycleEnCours(t.getId());
            boolean corrigeable = c != null && c.getNumero() == ti.getSemaine();

            VBox infos = texte("\u00ab " + t.getNom() + " \u00bb - tour " + ti.getSemaine(),
                    "B\u00e9n\u00e9ficiaire : " + (gagnant == null ? "?" : gagnant.getNomComplet()) + " - " + Aff.date(ti.getDate()));
            Label etat = new Label(corrigeable ? "Modifiable" : "Vers\u00e9");
            etat.getStyleClass().addAll("badge-statut", corrigeable ? "statut-attente" : "statut-acceptee");
            if (corrigeable) {
                Button corriger = Composants.contour("Corriger");
                corriger.setOnAction(e -> {
                    tirageEnCorrection = ti;
                    rafraichir();
                });
                getChildren().add(ligne(infos, etat, corriger));
            } else {
                getChildren().add(ligne(infos, etat));
            }
            if (tirageEnCorrection != null && tirageEnCorrection.getId() == ti.getId() && corrigeable) {
                getChildren().add(formulaire(ti, t));
            }
        }
    }

    private VBox formulaire(Tirage ti, Tontine t) {
        nouveauBeneficiaire.getItems().clear();
        for (LigneMembre m : new AdhesionService().listerMembres(admin.getId(), t.getId())) {
            if (m.getAdhesion().getStatut() == StatutAdhesion.ACTIF && m.getAdhesion().getUserId() != ti.getBeneficiaireId()) {
                nouveauBeneficiaire.getItems().add(m);
            }
        }
        motif.clear();
        boutonEnregistrer = Composants.primaire("Enregistrer la correction");
        boutonEnregistrer.setOnAction(e -> action(() -> {
            LigneMembre m = nouveauBeneficiaire.getValue();
            if (m == null) {
                erreur("Choisissez le nouveau b\u00e9n\u00e9ficiaire.");
                return;
            }
            if (!Nav.confirmer("Remplacer le b\u00e9n\u00e9ficiaire par " + m.getUtilisateur().getNomComplet() + " ? Cette correction sera trac\u00e9e.")) {
                return;
            }
            service.corrigerTirage(admin.getId(), ti.getId(), m.getAdhesion().getUserId(), motif.getText());
            tirageEnCorrection = null;
            succes("Tirage corrig\u00e9 : " + m.getUtilisateur().getNomComplet() + " recevra la cagnotte. Les membres sont pr\u00e9venus.");
        }));
        Button annuler = Composants.contour("Annuler");
        annuler.setOnAction(e -> {
            tirageEnCorrection = null;
            rafraichir();
        });
        VBox f = new VBox(12, Composants.champ("Nouveau b\u00e9n\u00e9ficiaire", nouveauBeneficiaire),
                Composants.champ("Motif de la correction (obligatoire)", motif), new HBox(12, boutonEnregistrer, annuler));
        f.getStyleClass().add("carte");
        return f;
    }

    // acces pour les tests automatiques
    ComboBox<LigneMembre> getNouveauBeneficiaire() { return nouveauBeneficiaire; }
    TextField getMotif() { return motif; }
    Button getBoutonEnregistrer() { return boutonEnregistrer; }
}
