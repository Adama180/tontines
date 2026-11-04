package com.tontine.ui;

import com.tontine.model.Frequence;
import com.tontine.model.Tontine;
import com.tontine.service.Format;
import com.tontine.service.CotisationService;
import com.tontine.model.Utilisateur;
import com.tontine.service.AdhesionService;
import com.tontine.service.ConsultationService;
import com.tontine.service.Libelles;
import com.tontine.service.MembreService;
import com.tontine.service.ParametresTontine;

import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** Regles de la tontine definies par le gerant (aucune valeur codee en dur). Modifiables entre deux cycles. */
public class ParametresPane extends PaneGestion {

    private final Utilisateur gerant;
    private final Tontine tontine;
    private final TextField description = new TextField();
    private final TextField montant = new TextField();
    private final ComboBox<Frequence> frequence = new ComboBox<>();
    private final TextField dureeCycle = new TextField();
    private final TextField montantMax = new TextField();
    private final TextField penalite = new TextField();
    private final TextField nbMax = new TextField();
    private final TextField taux = new TextField();
    private final TextField dureeMaxPret = new TextField();
    private Button enregistrer;

    public ParametresPane(Utilisateur gerant, Tontine tontine) {
        this.gerant = gerant;
        this.tontine = tontine;
        frequence.getItems().addAll(Frequence.values());
        frequence.setConverter(new StringConverter<Frequence>() {
            @Override
            public String toString(Frequence f) {
                return f == null ? "" : Libelles.frequence(f);
            }

            @Override
            public Frequence fromString(String s) {
                return null;
            }
        });
        frequence.setMaxWidth(Double.MAX_VALUE);
        rafraichir();
    }

    @Override
    protected void construire() {
        Tontine t = new ConsultationService().detail(tontine.getId());
        boolean cycleEnCours = new MembreService().cycleEnCours(t.getId()) != null;

        description.setText(t.getDescription() == null ? "" : t.getDescription());
        montant.setText(Aff.nombre(t.getMontantCotisation()));
        montantMax.setText(t.getMontantMaxCotisation() == null ? "" : Aff.nombre(t.getMontantMaxCotisation()));
        frequence.getSelectionModel().select(t.getFrequence());
        dureeCycle.setText(String.valueOf(t.getDureeEnCycle()));
        penalite.setText(Aff.nombre(t.getPenaliteRetard()));
        nbMax.setText(String.valueOf(t.getNbMembreMax()));
        taux.setText(t.getTauxInteret() == null ? "" : Aff.nombre(t.getTauxInteret()));
        dureeMaxPret.setText(t.getDureeMaxPret() == null ? "" : String.valueOf(t.getDureeMaxPret()));

        GridPane grille = new GridPane();
        grille.setHgap(16);
        grille.setVgap(14);
        ColumnConstraints moitie = new ColumnConstraints();
        moitie.setPercentWidth(50);
        grille.getColumnConstraints().addAll(moitie, moitie);
        grille.add(Composants.champ("Description", description), 0, 0, 2, 1);
        if (t.isFondsCommun()) {
            grille.add(Composants.champ("Montant minimum de cotisation (au moins " + Format.montant(CotisationService.MIN_FONDS_COMMUN) + ")", montant), 0, 1);
            grille.add(Composants.champ("Montant maximum de cotisation (FCFA)", montantMax), 1, 1);
            grille.add(Composants.champ("Fr\u00e9quence de cotisation", frequence), 0, 2);
            grille.add(Composants.champ("Dur\u00e9e d'un cycle (en p\u00e9riodes)", dureeCycle), 1, 2);
            grille.add(Composants.champ("P\u00e9nalit\u00e9 de retard (FCFA)", penalite), 0, 3);
            grille.add(Composants.champ("Nombre maximum de membres", nbMax), 1, 3);
            grille.add(Composants.champ("Taux d'int\u00e9r\u00eat des pr\u00eats (%)", taux), 0, 4);
            grille.add(Composants.champ("Dur\u00e9e maximale d'un pr\u00eat (jours)", dureeMaxPret), 1, 4);
        } else {
            // tour de role / tirage au sort : le gerant fixe le montant que chaque membre cotise a chaque periode
            grille.add(Composants.champ("Montant \u00e0 cotiser par membre et par p\u00e9riode (FCFA)", montant), 0, 1);
            grille.add(Composants.champ("Fr\u00e9quence de cotisation", frequence), 1, 1);
            grille.add(Composants.champ("P\u00e9nalit\u00e9 de retard ou de non-cotisation (FCFA)", penalite), 0, 2);
            grille.add(Composants.champ("Nombre maximum de membres", nbMax), 1, 2);
        }

        Label recap = Composants.sousTitre("");
        recap.setWrapText(true);
        recap.getStyleClass().add("recap-regles");
        Runnable majRecap = () -> {
            try {
                double m = Saisie.montant(montant.getText(), "Montant");
                int n = Saisie.entier(nbMax.getText(), "Membres");
                String per = frequence.getValue() == null ? "p\u00e9riode" : Libelles.periode(frequence.getValue());
                recap.setText("R\u00e9capitulatif : chaque " + per + ", chacun des " + n + " membres cotise " + Format.montant(m)
                        + ". Le b\u00e9n\u00e9ficiaire du cycle re\u00e7oit alors " + n + " \u00d7 " + Format.montant(m) + " = "
                        + Format.montant(m * n) + " (plus les p\u00e9nalit\u00e9s de retard \u00e9ventuelles).");
            } catch (RuntimeException ex) {
                recap.setText("R\u00e9capitulatif : saisissez un montant et un nombre de membres valides.");
            }
        };
        if (!t.isFondsCommun()) {
            montant.textProperty().addListener((o, x, y) -> majRecap.run());
            nbMax.textProperty().addListener((o, x, y) -> majRecap.run());
            frequence.valueProperty().addListener((o, x, y) -> majRecap.run());
            majRecap.run();
        }

        VBox carte = Composants.carte(titreSection("R\u00e8gles de la tontine \u00ab " + t.getNom() + " \u00bb"));
        if (cycleEnCours) {
            carte.getChildren().add(vide("Un cycle est en cours : les param\u00e8tres ne sont modifiables qu'entre deux cycles "
                    + "(apr\u00e8s la cl\u00f4ture)."));
        } else {
            carte.getChildren().add(vide("Ces r\u00e8gles s'appliquent \u00e0 tous les membres. "
                    + (t.isFondsCommun() ? "Pour d\u00e9marrer un cycle, d\u00e9finissez l'intervalle de cotisation (minimum 1 000 FCFA), le taux d'int\u00e9r\u00eat et la dur\u00e9e maximale des pr\u00eats."
                            : "D\u00e9finissez ici les r\u00e8gles de votre tontine, en particulier le montant que chaque membre cotise.")));
        }
        carte.getChildren().add(grille);
        if (!t.isFondsCommun()) {
            String ordre = t.isTourDeRole()
                    ? "L'ordre des b\u00e9n\u00e9ficiaires suit l'ordre d'adh\u00e9sion ; un membre peut demander un tour anticip\u00e9, que vous acceptez ou refusez entre deux cycles."
                    : "Le b\u00e9n\u00e9ficiaire de chaque cycle est tir\u00e9 au sort parmi les membres qui n'ont pas encore \u00e9t\u00e9 tir\u00e9s ; vous lancez le tirage.";
            Label regles = Composants.sousTitre("R\u00e8gles appliqu\u00e9es par la plateforme : "
                    + "le montant est fixe et identique pour tous ; chaque membre cotise une seule fois par cycle (une p\u00e9riode) ; "
                    + "une cotisation pay\u00e9e apr\u00e8s la date limite (fin de la p\u00e9riode) ajoute la p\u00e9nalit\u00e9 de retard, vers\u00e9e au b\u00e9n\u00e9ficiaire ; "
                    + "r\u00e9ciprocit\u00e9 : un membre cotise pour le b\u00e9n\u00e9ficiaire seulement si celui-ci a cotis\u00e9 pour lui lors de son tour, "
                    + "et un membre qui devait cotiser sans le faire subit la p\u00e9nalit\u00e9 fix\u00e9e ici, \u00e0 payer avec sa prochaine cotisation ; "
                    + "les nouveaux membres ne sont accept\u00e9s qu'entre deux cycles et le montant ne se modifie qu'entre deux cycles. " + ordre);
            regles.setWrapText(true);
            carte.getChildren().addAll(recap, regles);
        }

        enregistrer = Composants.primaire("Enregistrer les param\u00e8tres");
        enregistrer.setOnAction(e -> action(() -> {
            ParametresTontine p = new ParametresTontine();
            p.setDescription(description.getText());
            p.setMontantCotisation(Saisie.montant(montant.getText(), "Montant de la cotisation"));
            if (t.isFondsCommun()) {
                p.setMontantMaxCotisation(Saisie.montant(montantMax.getText(), "Montant maximum de cotisation"));
            }
            p.setFrequence(frequence.getValue());
            p.setDureeEnCycle(t.isFondsCommun() ? Saisie.entier(dureeCycle.getText(), "Dur\u00e9e d'un cycle") : Math.max(1, t.getDureeEnCycle()));
            p.setPenaliteRetard(Saisie.montant(penalite.getText(), "P\u00e9nalit\u00e9 de retard"));
            p.setNbMembreMax(Saisie.entier(nbMax.getText(), "Nombre maximum de membres"));
            if (t.isFondsCommun()) {
                p.setTauxInteret(Saisie.montant(taux.getText(), "Taux d'int\u00e9r\u00eat"));
                p.setDureeMaxPret(Saisie.entier(dureeMaxPret.getText(), "Dur\u00e9e maximale d'un pr\u00eat"));
            }
            new AdhesionService().definirParametres(gerant.getId(), t.getId(), p);
            succes("Les param\u00e8tres de la tontine ont \u00e9t\u00e9 enregistr\u00e9s.");
        }));

        for (javafx.scene.Node n : new javafx.scene.Node[] { description, montant, montantMax, frequence, dureeCycle, penalite, nbMax, taux, dureeMaxPret }) {
            n.setDisable(cycleEnCours);
        }
        enregistrer.setDisable(cycleEnCours);
        carte.getChildren().add(enregistrer);
        getChildren().add(carte);
    }

    // acces pour les tests automatiques
    void remplir(String penaliteTexte, String tauxTexte, String dureeMaxTexte) {
        penalite.setText(penaliteTexte);
        taux.setText(tauxTexte);
        dureeMaxPret.setText(dureeMaxTexte);
    }

    Button getBoutonEnregistrer() {
        return enregistrer;
    }

    boolean estVerrouille() {
        return enregistrer.isDisable();
    }
}
