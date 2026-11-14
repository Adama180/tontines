package com.tontine.ui;

import java.util.Map;

import com.tontine.model.Cycle;
import com.tontine.model.ModePaiement;
import com.tontine.model.Pret;
import com.tontine.model.StatutPret;
import com.tontine.model.Tontine;
import com.tontine.model.Utilisateur;
import com.tontine.service.ConsultationService;
import com.tontine.service.Format;
import com.tontine.service.Libelles;
import com.tontine.service.MembreService;
import com.tontine.service.PretService;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Prets du fonds commun : conditions, demande, mes prets (avec remboursement) et prets de toute la tontine. */
public class PretsPane extends VBox {

    private final Utilisateur u;
    private final Tontine t;
    private final PretService prets = new PretService();
    private final MembreService membre = new MembreService();
    private final ConsultationService consultation = new ConsultationService();
    private String banniere; // message de succes affiche apres une action

    // champs de la demande (gardes pour les tests)
    private final TextField montantPret = new TextField();
    private final TextField joursPret = new TextField();
    private final TextField motifPret = new TextField();
    private Button boutonDemander;

    public PretsPane(Utilisateur u, Tontine t) {
        super(16);
        this.u = u;
        this.t = t;
        construire();
    }

    private void construire() {
        getChildren().clear();
        Tontine tf = consultation.detail(t.getId());
        Cycle cycle = membre.cycleEnCours(t.getId());
        Map<Integer, String> noms = membre.nomsMembres(u.getId(), t.getId());

        if (banniere != null) {
            Label b = Composants.message();
            Composants.montrer(b, banniere, false);
            getChildren().add(b);
            banniere = null;
        }

        // ---- conditions
        FlowPane conditions = new FlowPane(14, 14);
        conditions.getChildren().addAll(
                stat(Format.montant(prets.fondsDisponible(t.getId())), "Fonds disponible pour les pr\u00eats"),
                stat(tf.getTauxInteret() == null ? "non d\u00e9fini" : Aff.nombre(tf.getTauxInteret()) + " %", "Taux d'int\u00e9r\u00eat (int\u00e9r\u00eat simple)"),
                stat(tf.getDureeMaxPret() == null ? "non d\u00e9finie" : tf.getDureeMaxPret() + " jours", "Dur\u00e9e maximale d'un pr\u00eat"));
        getChildren().add(conditions);

        // ---- demande de pret
        Label titreDemande = new Label("Demander un pr\u00eat");
        titreDemande.getStyleClass().add("carte-titre");
        if (cycle == null) {
            getChildren().add(Composants.carte(titreDemande, Composants.sousTitre(
                    "Les pr\u00eats sont possibles pendant un cycle en cours. Le g\u00e9rant doit d'abord en d\u00e9marrer un.")));
        } else if (tf.getTauxInteret() == null || tf.getDureeMaxPret() == null) {
            getChildren().add(Composants.carte(titreDemande, Composants.sousTitre(
                    "Le g\u00e9rant n'a pas encore d\u00e9fini les conditions de pr\u00eat (taux et dur\u00e9e maximale).")));
        } else {
            montantPret.clear();
            joursPret.clear();
            motifPret.clear();
            montantPret.setPromptText("ex. 10000");
            joursPret.setPromptText("max. " + tf.getDureeMaxPret());
            motifPret.setPromptText("Ex. : achat de marchandises");
            Label msg = Composants.message();
            boutonDemander = Composants.primaire("Envoyer ma demande de pr\u00eat");
            boutonDemander.setOnAction(e -> {
                Composants.cacher(msg);
                try {
                    double m = Saisie.montant(montantPret.getText(), "Montant");
                    int j = Saisie.entier(joursPret.getText(), "Dur\u00e9e (jours)");
                    prets.demanderPret(u.getId(), t.getId(), m, j, motifPret.getText());
                    banniere = "Votre demande de pr\u00eat de " + Format.montant(m)
                            + " a \u00e9t\u00e9 envoy\u00e9e. Elle sera valid\u00e9e par "
                            + (u.getId() == tf.getGerantId() ? "l'administrateur." : "le g\u00e9rant.");
                    construire();
                } catch (RuntimeException ex) {
                    Composants.montrer(msg, Nav.messageErreur(ex), true);
                }
            });
            HBox ligne = new HBox(14, Composants.champ("Montant (FCFA)", montantPret),
                    Composants.champ("Dur\u00e9e (jours)", joursPret), Composants.champ("Motif", motifPret));
            motifPret.setPrefWidth(300);
            getChildren().add(Composants.carte(titreDemande,
                    Composants.sousTitre("Un seul pr\u00eat \u00e0 la fois. Int\u00e9r\u00eat = montant \u00d7 taux, pour toute la dur\u00e9e du pr\u00eat. "
                            + (u.getId() == tf.getGerantId()
                                    ? "Comme vous \u00eates g\u00e9rant, c'est l'administrateur qui validera."
                                    : "Le g\u00e9rant validera votre demande.")),
                    ligne, msg, boutonDemander));
        }

        // ---- mes prets
        Label titreMes = new Label("Mes pr\u00eats");
        titreMes.getStyleClass().add("carte-titre");
        getChildren().add(titreMes);
        int mesPrets = 0;
        for (Pret p : prets.mesPrets(u.getId())) {
            if (p.getTontineId() != t.getId()) {
                continue;
            }
            mesPrets++;
            getChildren().add(lignePret(p, tf, null));
        }
        if (mesPrets == 0) {
            getChildren().add(Composants.sousTitre("Vous n'avez aucun pr\u00eat dans cette tontine."));
        }

        // ---- tous les prets (transparence)
        Label titreTous = new Label("Pr\u00eats de la tontine (visibles par tous les membres)");
        titreTous.getStyleClass().add("carte-titre");
        getChildren().add(titreTous);
        java.util.List<Pret> tous = prets.listerPrets(u.getId(), t.getId());
        if (tous.isEmpty()) {
            getChildren().add(Composants.sousTitre("Aucun pr\u00eat dans cette tontine pour le moment."));
        }
        for (Pret p : tous) {
            getChildren().add(lignePret(p, tf, noms.getOrDefault(p.getUserId(), "?")));
        }
    }

    /** Une ligne de pret. nomEmprunteur = null pour "mes prets" (avec bouton Rembourser). */
    private VBox lignePret(Pret p, Tontine tf, String nomEmprunteur) {
        double interets = prets.interets(p, tf);
        String detail;
        if (p.getStatut() == StatutPret.ACCORDE || p.getStatut() == StatutPret.REMBOURSE) {
            detail = "Int\u00e9r\u00eat " + Format.montant(interets) + " - total d\u00fb " + Format.montant(prets.totalDu(p, tf))
                    + " - rembours\u00e9 " + Format.montant(p.getMontantRembourse())
                    + " - reste " + Format.montant(prets.resteAPayer(p, tf));
        } else {
            detail = "Demand\u00e9 le " + Aff.date(p.getDateDemande()) + " - " + p.getNbJours() + " jours"
                    + (p.getMotif() == null || p.getMotif().isEmpty() ? "" : " - " + p.getMotif());
        }
        Label titre = new Label((nomEmprunteur == null ? "" : nomEmprunteur + " : ") + "pr\u00eat de " + Format.montant(p.getMontant())
                + " sur " + p.getNbJours() + " jours");
        titre.getStyleClass().add("carte-titre");
        titre.setWrapText(true);
        Label d = Composants.sousTitre(detail);
        HBox haut = new HBox(12, new VBox(4, titre, d), Composants.espaceur(), Aff.badgePret(p.getStatut()));
        haut.setAlignment(Pos.CENTER_LEFT);

        VBox ligne = new VBox(10, haut);
        ligne.getStyleClass().add("ligne-liste");
        if (nomEmprunteur == null && p.getStatut() == StatutPret.ACCORDE) {
            VBox zone = new VBox();
            Button rembourser = Composants.or("Rembourser");
            rembourser.setOnAction(e -> {
                if (zone.getChildren().isEmpty()) {
                    zone.getChildren().add(formulaireRemboursement(p, tf));
                } else {
                    zone.getChildren().clear();
                }
            });
            haut.getChildren().add(haut.getChildren().size() - 1, rembourser);
            ligne.getChildren().add(zone);
        }
        return ligne;
    }

    private VBox formulaireRemboursement(Pret p, Tontine tf) {
        double reste = prets.resteAPayer(p, tf);
        TextField montant = new TextField(Aff.nombre(reste));
        ChoixPaiement paiement = new ChoixPaiement();
        Label msg = Composants.message();
        Button valider = Composants.primaire("Confirmer le remboursement");
        valider.setOnAction(e -> {
            Composants.cacher(msg);
            try {
                double m = Saisie.montant(montant.getText(), "Montant");
                ModePaiement mode = paiement.getMode();
                if (!Nav.confirmer("Rembourser " + Format.montant(m) + " par " + Libelles.mode(mode) + " ?")) {
                    return;
                }
                prets.rembourser(u.getId(), p.getId(), m, mode, paiement.getNumero());
                banniere = "Remboursement de " + Format.montant(m) + " enregistr\u00e9.";
                construire();
            } catch (RuntimeException ex) {
                Composants.montrer(msg, Nav.messageErreur(ex), true);
            }
        });
        VBox v = new VBox(12, Composants.sousTitre("Reste \u00e0 rembourser : " + Format.montant(reste)),
                Composants.champ("Montant du remboursement (FCFA)", montant), paiement, msg, valider);
        v.setPadding(new javafx.geometry.Insets(6, 0, 0, 0));
        return v;
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

    // acces pour les tests automatiques
    void remplirDemande(String montant, String jours, String motif) {
        montantPret.setText(montant);
        joursPret.setText(jours);
        motifPret.setText(motif);
    }

    Button getBoutonDemander() {
        return boutonDemander;
    }
}
