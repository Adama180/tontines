package com.tontine.ui;

import java.time.LocalTime;

import com.tontine.model.Frequence;
import com.tontine.model.TypeTontine;
import com.tontine.model.Utilisateur;
import com.tontine.service.CotisationService;
import com.tontine.service.DemandeService;
import com.tontine.service.Format;
import com.tontine.service.Libelles;
import com.tontine.service.ParametresTontine;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Demande pour devenir gerant et creer sa propre tontine (validee par l'administrateur).
 * TOUS les parametres de la tontine sont definis ici, avant l'envoi : le gerant n'a plus de page "Parametres".
 * Visiteur : cree aussi son compte. Utilisateur connecte : envoie juste la demande.
 */
public class CreerTontineView extends VBox {

    private final DemandeService demandes = new DemandeService();
    private final FormIdentite identite;
    private final TextField nom = new TextField();
    private final TextField description = new TextField();
    private final ComboBox<TypeTontine> type = new ComboBox<>();
    private final ComboBox<Frequence> frequence = new ComboBox<>();
    private final TextField montant = new TextField();
    private final TextField montantMax = new TextField();
    private final TextField dureeCycle = new TextField();
    private final TextField penalite = new TextField();
    private final TextField nbMembres = new TextField();
    private final TextField taux = new TextField();
    private final TextField dureeMaxPret = new TextField();
    private final ComboBox<Integer> jourSemaine = new ComboBox<>();
    private final ComboBox<Integer> jourMois = new ComboBox<>();
    private final ComboBox<Integer> mois = new ComboBox<>();
    private final ComboBox<Integer> heure = new ComboBox<>();
    private final ComboBox<Integer> minute = new ComboBox<>();
    private final ChoixPiece piece = new ChoixPiece();
    private final GridPane grille = new GridPane();
    private final Label recap = Composants.sousTitre("");

    public CreerTontineView(Utilisateur utilisateur, Fenetre fenetre) {
        super(14);

        type.getItems().addAll(TypeTontine.values());
        type.setConverter(new StringConverter<TypeTontine>() {
            @Override
            public String toString(TypeTontine t) {
                return t == null ? "" : Libelles.type(t);
            }

            @Override
            public TypeTontine fromString(String s) {
                return null;
            }
        });
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
        for (int j = 1; j <= 7; j++) {
            jourSemaine.getItems().add(j);
        }
        jourSemaine.setConverter(texteInt(Libelles::jourSemaine));
        for (int j = 1; j <= 31; j++) {
            jourMois.getItems().add(j);
        }
        for (int m = 1; m <= 12; m++) {
            mois.getItems().add(m);
        }
        mois.setConverter(texteInt(Libelles::mois));
        for (int h = 0; h < 24; h++) {
            heure.getItems().add(h);
        }
        for (int m = 0; m < 60; m += 5) {
            minute.getItems().add(m);
        }
        minute.getItems().add(59);
        heure.setConverter(texteInt(h -> String.format("%02d h", h)));
        minute.setConverter(texteInt(m -> String.format("%02d min", m)));
        for (ComboBox<?> c : new ComboBox<?>[] { type, frequence, jourSemaine, jourMois, mois, heure, minute }) {
            c.setMaxWidth(Double.MAX_VALUE);
        }
        type.getSelectionModel().select(TypeTontine.FONDS_COMMUN);
        frequence.getSelectionModel().select(Frequence.MENSUELLE);
        jourSemaine.getSelectionModel().select(Integer.valueOf(1));
        jourMois.getSelectionModel().select(Integer.valueOf(5));
        mois.getSelectionModel().select(Integer.valueOf(1));
        heure.getSelectionModel().select(Integer.valueOf(18));
        minute.getSelectionModel().select(Integer.valueOf(0));
        dureeCycle.setText("12");
        penalite.setText("0");

        nom.setPromptText("Nom unique dans toute l'application");
        description.setPromptText("Facultatif : objectif de la tontine");
        montant.setPromptText("ex. 5000");
        montantMax.setPromptText("ex. 20000");
        nbMembres.setPromptText("ex. 10");
        taux.setPromptText("ex. 10");
        dureeMaxPret.setPromptText("ex. 30");

        Label descriptionType = Composants.sousTitre(Libelles.descriptionType(TypeTontine.FONDS_COMMUN));
        descriptionType.setWrapText(true);
        recap.setWrapText(true);
        type.valueProperty().addListener((o, a, b) -> {
            if (b != null) {
                descriptionType.setText(Libelles.descriptionType(b));
                construireGrille();
            }
        });
        frequence.valueProperty().addListener((o, a, b) -> construireGrille());
        for (TextField tf : new TextField[] { montant, montantMax, nbMembres, dureeCycle }) {
            tf.textProperty().addListener((o, a, b) -> majRecap());
        }
        for (ComboBox<?> c : new ComboBox<?>[] { jourSemaine, jourMois, mois, heure, minute }) {
            c.valueProperty().addListener((o, a, b) -> majRecap());
        }

        grille.setHgap(16);
        grille.setVgap(14);
        ColumnConstraints moitie = new ColumnConstraints();
        moitie.setPercentWidth(50);
        grille.getColumnConstraints().addAll(moitie, moitie);

        VBox carte = Composants.carte(
                Composants.titre("Cr\u00e9er ma propre tontine"),
                Composants.sousTitre("D\u00e9finissez ici TOUS les param\u00e8tres de votre tontine : montants, fr\u00e9quence, date et heure de "
                        + "cotisation, p\u00e9nalit\u00e9, pr\u00eats... Votre demande est ensuite examin\u00e9e par l'administrateur ; si elle est "
                        + "accept\u00e9e, la tontine est cr\u00e9\u00e9e telle que vous l'avez d\u00e9finie et vous en devenez le g\u00e9rant "
                        + "(vous cotiserez vous aussi)."));

        if (utilisateur == null) {
            identite = new FormIdentite();
            carte.getChildren().addAll(Composants.sousTitre("Un compte sera cr\u00e9\u00e9 avec ces informations."), identite, piece);
        } else {
            identite = null;
            if (demandes.aPieceEnregistree(utilisateur.getId())) {
                carte.getChildren().add(Composants.sousTitre("Votre pi\u00e8ce d'identit\u00e9 d\u00e9j\u00e0 enregistr\u00e9e sera "
                        + "utilis\u00e9e pour cette demande : inutile de la renvoyer."));
            } else {
                carte.getChildren().add(piece);
            }
        }
        carte.getChildren().addAll(descriptionType, grille, recap);
        construireGrille();

        Label message = Composants.message();
        Button envoyer = Composants.primaire("Envoyer ma demande");
        envoyer.setOnAction(e -> {
            Composants.cacher(message);
            try {
                ParametresTontine p = lireParametres();
                if (utilisateur == null) {
                    String erreur = identite.erreurConfirmation();
                    if (erreur != null) {
                        Composants.montrer(message, erreur, true);
                        return;
                    }
                    demandes.inscrireEtDemanderCreation(identite.getNom(), identite.getPrenom(), identite.getEmail(),
                            identite.getTelephone(), identite.getMotDePasse(), nom.getText(), type.getValue(), p,
                            piece.getFichier());
                    Nav.aller(new ConnexionView("Votre compte est cr\u00e9\u00e9 et votre demande de cr\u00e9ation a \u00e9t\u00e9 "
                            + "envoy\u00e9e \u00e0 l'administrateur. Connectez-vous pour suivre son \u00e9tat."));
                } else {
                    demandes.soumettreDemandeCreation(utilisateur.getId(), nom.getText(), type.getValue(), p,
                            piece.getFichier());
                    fenetre.afficher("Mes demandes", new MesDemandesView(utilisateur));
                }
            } catch (RuntimeException ex) {
                Composants.montrer(message, Nav.messageErreur(ex), true);
            }
        });
        carte.getChildren().addAll(message, new HBox(12, envoyer));
        getChildren().add(carte);
    }

    private static <T> StringConverter<T> texteInt(java.util.function.Function<T, String> f) {
        return new StringConverter<T>() {
            @Override
            public String toString(T v) {
                return v == null ? "" : f.apply(v);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
    }

    /** Affiche les champs utiles selon le type (fonds commun / tour de role / tirage) et la frequence. */
    private void construireGrille() {
        grille.getChildren().clear();
        TypeTontine t = type.getValue();
        Frequence f = frequence.getValue();
        if (t == null || f == null) {
            return;
        }
        boolean fc = t == TypeTontine.FONDS_COMMUN;
        int r = 0;
        grille.add(Composants.champ("Nom de la tontine", nom), 0, r, 2, 1);
        r++;
        grille.add(Composants.champ("Description", description), 0, r, 2, 1);
        r++;
        grille.add(Composants.champ("Type de tontine", type), 0, r);
        grille.add(Composants.champ("Fr\u00e9quence de cotisation", frequence), 1, r);
        r++;
        if (fc) {
            grille.add(Composants.champ("Montant minimum de cotisation (au moins " + Format.montant(CotisationService.MIN_FONDS_COMMUN) + ")", montant), 0, r);
            grille.add(Composants.champ("Montant maximum de cotisation (FCFA)", montantMax), 1, r);
        } else {
            grille.add(Composants.champ("Montant \u00e0 cotiser par membre et par p\u00e9riode (FCFA)", montant), 0, r);
            grille.add(Composants.champ("Nombre maximum de membres", nbMembres), 1, r);
        }
        r++;
        // date et heure de cotisation selon la frequence
        switch (f) {
        case JOURNALIERE:
            grille.add(Composants.champ("Heure limite de cotisation : heures", heure), 0, r);
            grille.add(Composants.champ("Heure limite de cotisation : minutes", minute), 1, r);
            break;
        case HEBDOMADAIRE:
            grille.add(Composants.champ("Jour de cotisation (jour de la semaine)", jourSemaine), 0, r);
            grille.add(heureBox(), 1, r);
            break;
        case MENSUELLE:
            grille.add(Composants.champ("Jour de cotisation (jour du mois)", jourMois), 0, r);
            grille.add(heureBox(), 1, r);
            break;
        default:
            grille.add(Composants.champ("Mois de cotisation", mois), 0, r);
            grille.add(Composants.champ("Jour du mois", jourMois), 1, r);
            r++;
            grille.add(heureBox(), 0, r);
        }
        r++;
        if (fc) {
            grille.add(Composants.champ("Dur\u00e9e d'un cycle (en p\u00e9riodes)", dureeCycle), 0, r);
            grille.add(Composants.champ("Nombre maximum de membres", nbMembres), 1, r);
            r++;
            grille.add(Composants.champ("P\u00e9nalit\u00e9 de retard (FCFA)", penalite), 0, r);
            grille.add(Composants.champ("Taux d'int\u00e9r\u00eat des pr\u00eats (%)", taux), 1, r);
            r++;
            grille.add(Composants.champ("Dur\u00e9e maximale d'un pr\u00eat (jours)", dureeMaxPret), 0, r);
        } else {
            grille.add(Composants.champ("P\u00e9nalit\u00e9 de retard ou de non-cotisation (FCFA)", penalite), 0, r);
        }
        majRecap();
    }

    private HBox heureBox() {
        HBox h = new HBox(8, Composants.champ("Heure limite", heure), Composants.champ("Minutes", minute));
        HBox.setHgrow(h.getChildren().get(0), javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(h.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);
        return h;
    }

    private LocalTime heureChoisie() {
        return LocalTime.of(heure.getValue(), minute.getValue());
    }

    private String horaireChoisi() {
        Frequence f = frequence.getValue();
        Integer jour = f == Frequence.HEBDOMADAIRE ? jourSemaine.getValue()
                : f == Frequence.MENSUELLE || f == Frequence.ANNUELLE ? jourMois.getValue() : null;
        return Libelles.horaire(f, jour, f == Frequence.ANNUELLE ? mois.getValue() : null, heureChoisie());
    }

    private void majRecap() {
        if (type.getValue() == null || frequence.getValue() == null || heure.getValue() == null || minute.getValue() == null) {
            return;
        }
        String texte = "R\u00e9capitulatif : cotisation " + horaireChoisi() + ".";
        try {
            double m = Saisie.montant(montant.getText(), "Montant");
            if (type.getValue() == TypeTontine.FONDS_COMMUN) {
                texte += " Chaque membre cotise une fois par " + Libelles.periode(frequence.getValue()) + ", au moins "
                        + Format.montant(m) + ".";
            } else {
                int n = Saisie.entier(nbMembres.getText(), "Membres");
                texte += " Chacun des " + n + " membres cotise " + Format.montant(m) + " ; le b\u00e9n\u00e9ficiaire du tour re\u00e7oit "
                        + Format.montant(m * n) + " (plus les p\u00e9nalit\u00e9s \u00e9ventuelles).";
            }
        } catch (RuntimeException ex) {
            // montant pas encore saisi : on n'affiche que l'horaire
        }
        recap.setText(texte);
    }

    /** Lit tous les champs du formulaire ; les erreurs de format deviennent des messages clairs. */
    private ParametresTontine lireParametres() {
        TypeTontine t = type.getValue();
        Frequence f = frequence.getValue();
        boolean fc = t == TypeTontine.FONDS_COMMUN;
        ParametresTontine p = new ParametresTontine();
        p.setDescription(description.getText());
        p.setFrequence(f);
        p.setMontantCotisation(Saisie.montant(montant.getText(), fc ? "Montant minimum" : "Montant de la cotisation"));
        p.setNbMembreMax(Saisie.entier(nbMembres.getText(), "Nombre maximum de membres"));
        p.setPenaliteRetard(Saisie.montant(penalite.getText(), "P\u00e9nalit\u00e9 de retard"));
        if (fc) {
            p.setMontantMaxCotisation(Saisie.montant(montantMax.getText(), "Montant maximum"));
            p.setDureeEnCycle(Saisie.entier(dureeCycle.getText(), "Dur\u00e9e d'un cycle"));
            p.setTauxInteret(Saisie.montant(taux.getText(), "Taux d'int\u00e9r\u00eat"));
            p.setDureeMaxPret(Saisie.entier(dureeMaxPret.getText(), "Dur\u00e9e maximale d'un pr\u00eat"));
        } else {
            p.setDureeEnCycle(1);
        }
        p.setHeureCotisation(heureChoisie());
        if (f == Frequence.HEBDOMADAIRE) {
            p.setJourCotisation(jourSemaine.getValue());
        } else if (f == Frequence.MENSUELLE) {
            p.setJourCotisation(jourMois.getValue());
        } else if (f == Frequence.ANNUELLE) {
            p.setMoisCotisation(mois.getValue());
            p.setJourCotisation(jourMois.getValue());
        }
        return p;
    }

    // acces pour les tests automatiques
    void remplirTontine(String n, double m, int nb) {
        nom.setText(n);
        montant.setText(String.valueOf(m));
        nbMembres.setText(String.valueOf(nb));
        montantMax.setText(String.valueOf(m * 4));
        taux.setText("10");
        dureeMaxPret.setText("30");
    }

    void choisir(TypeTontine t, Frequence f) {
        type.setValue(t);
        frequence.setValue(f);
    }

    FormIdentite getIdentite() {
        return identite;
    }

    ChoixPiece getPiece() {
        return piece;
    }
}
