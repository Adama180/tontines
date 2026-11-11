package com.tontine.ui;

import com.tontine.model.Cycle;
import com.tontine.model.ModePaiement;
import com.tontine.model.Tontine;
import com.tontine.model.Transaction;
import com.tontine.model.Utilisateur;
import com.tontine.service.ConsultationService;
import com.tontine.service.CotisationService;
import com.tontine.service.EtatCotisation;
import com.tontine.service.Format;
import com.tontine.service.Libelles;
import com.tontine.service.MembreService;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Effectuer une cotisation : montant, mode de paiement (Wave, Orange Money, especes), confirmation. */
public class CotiserPane extends VBox {

    private final TextField montant = new TextField();
    private final ChoixPaiement paiement = new ChoixPaiement();
    private final Button payer = Composants.primaire("Payer ma cotisation");

    public CotiserPane(Utilisateur u, Tontine tontine) {
        super(14);
        MembreService membre = new MembreService();
        Tontine t = new ConsultationService().detail(tontine.getId());
        Cycle cycle = membre.cycleEnCours(t.getId());

        if (cycle == null) {
            Label titre = new Label("Aucun " + Libelles.cycle(t) + " en cours");
            titre.getStyleClass().add("carte-titre");
            getChildren().add(Composants.carte(titre, Composants.sousTitre(
                    "Vous pourrez cotiser d\u00e8s que le g\u00e9rant aura d\u00e9marr\u00e9 un nouveau " + Libelles.cycle(t) + ".")));
            return;
        }
        boolean fondsCommun = t.isFondsCommun();
        EtatCotisation etat = null;
        if (fondsCommun) {
            etat = new CotisationService().etatCotisation(u.getId(), t.getId());
            if (etat.termine() || !etat.peutCotiser) {
                Label titre = new Label(etat.termine() ? "Toutes vos cotisations du " + Libelles.cycle(t) + " sont faites"
                        : "Cotisation de cette p\u00e9riode d\u00e9j\u00e0 effectu\u00e9e");
                titre.getStyleClass().add("carte-titre");
                String detail = etat.termine()
                        ? "Vous avez cotis\u00e9 " + etat.faites + " fois sur " + etat.attendues + ". "
                                + "Le " + Libelles.cycle(t) + " se termine le " + Format.date(etat.finCycle) + " : chacun recevra alors sa cagnotte "
                                + "et une part des p\u00e9nalit\u00e9s."
                        : "Vous avez cotis\u00e9 " + etat.faites + " fois sur " + etat.attendues + ". Une seule cotisation par "
                                + Libelles.periode(t.getFrequence()) + " : prochaine cotisation \u00e0 partir du "
                                + Format.date(etat.disponibleLe) + ".";
                getChildren().add(Composants.carte(titre, Composants.sousTitre(detail)));
                return;
            }
        } else if (etatTour(u, t).tirageAttendu) {
            Label titre = new Label("En attente du tirage au sort");
            titre.getStyleClass().add("carte-titre");
            getChildren().add(Composants.carte(titre, Composants.sousTitre(
                    "Dans cette tontine, vous cotisez pour le b\u00e9n\u00e9ficiaire du " + Libelles.cycle(t) + ". Le tirage au sort doit d'abord avoir lieu : "
                            + "le g\u00e9rant va le lancer, puis vous pourrez cotiser.")));
            return;
        } else if (etatTour(u, t).exemption != null) {
            Label titre = new Label("Vous n'avez pas \u00e0 cotiser ce " + Libelles.cycle(t));
            titre.getStyleClass().add("carte-titre");
            getChildren().add(Composants.carte(titre, Composants.sousTitre(etatTour(u, t).exemption
                    + " (r\u00e8gle de r\u00e9ciprocit\u00e9 : on cotise pour ceux qui ont cotis\u00e9 pour soi).")));
            return;
        } else if (membre.aDejaCotiseDansCycle(u.getId(), t.getId())) {
            Label titre = new Label("Cotisation du " + Libelles.cycle(t) + " " + cycle.getNumero() + " d\u00e9j\u00e0 effectu\u00e9e");
            titre.getStyleClass().add("carte-titre");
            getChildren().add(Composants.carte(titre, Composants.sousTitre(
                    "Dans une tontine \u00e0 montant fixe, on cotise une seule fois par " + Libelles.cycle(t) + ". "
                            + "Retrouvez votre paiement dans l'onglet Historique.")));
            return;
        }
        boolean fixe = !fondsCommun;

        Label titre = new Label("Cotiser - " + Libelles.cycle(t) + " " + cycle.getNumero());
        titre.getStyleClass().add("carte-titre");
        String texteRegle;
        if (fondsCommun) {
            texteRegle = "Cotisation " + etat.rang + " sur " + etat.attendues + " (une par " + Libelles.periode(t.getFrequence())
                    + "). Date de cotisation : " + Libelles.horaire(t) + ". Choisissez votre montant, entre " + Format.montant(t.getMontantCotisation()) + " et "
                    + Format.montant(t.getMontantMaxCotisation() == null ? t.getMontantCotisation() : t.getMontantMaxCotisation()) + ", selon vos moyens. \u00c0 payer avant le "
                    + Format.dateHeure(etat.limite) + ".";
            if (t.getPenaliteRetard() > 0) {
                texteRegle += " P\u00e9nalit\u00e9 : " + Format.montant(t.getPenaliteRetard())
                        + ", retir\u00e9e de votre solde si vous cotisez apr\u00e8s la date limite mais avant 23 h 59 le m\u00eame jour. Au-del\u00e0, vous \u00eates marqu\u00e9(e) absent(e), sans p\u00e9nalit\u00e9. Les p\u00e9nalit\u00e9s sont partag\u00e9es entre tous les membres \u00e0 la fin du " + Libelles.cycle(t) + ".";
            }
            if (etat.absent) {
                texteRegle += " Date limite d\u00e9pass\u00e9e de plus de 23 h 59 : vous \u00eates marqu\u00e9(e) absent(e) pour cette cotisation (sans p\u00e9nalit\u00e9).";
            }
        } else {
            EtatCotisation et = etatTour(u, t);
            texteRegle = "Montant fixe : " + Format.montant(t.getMontantCotisation()) + " (une seule cotisation par " + Libelles.cycle(t) + ")."
                    + (et.beneficiaire == null ? "" : " Vous cotisez pour " + et.beneficiaire + ", b\u00e9n\u00e9ficiaire de ce " + Libelles.cycle(t) + " ; "
                            + "il/elle devra cotiser pour vous quand viendra votre tour. \u00c0 payer avant le " + Format.dateHeure(et.limite) + " (date de cotisation : " + Libelles.horaire(t) + ").");
            if (t.getPenaliteRetard() > 0) {
                texteRegle += " P\u00e9nalit\u00e9 : " + Format.montant(t.getPenaliteRetard())
                        + ", retir\u00e9e de votre solde si vous cotisez apr\u00e8s la date limite mais avant 23 h 59 le m\u00eame jour (vous ne payez que la cotisation). Au-del\u00e0, vous \u00eates marqu\u00e9(e) absent(e), sans p\u00e9nalit\u00e9.";
            }
            if (et.absent) {
                texteRegle += " Date limite d\u00e9pass\u00e9e de plus de 23 h 59 : vous \u00eates marqu\u00e9(e) absent(e) pour ce " + Libelles.cycle(t) + " (sans p\u00e9nalit\u00e9).";
            }
        }
        Label regle = Composants.sousTitre(texteRegle);
        montant.setText(Aff.nombre(t.getMontantCotisation()));
        montant.setEditable(!fixe);

        Label message = Composants.message();
        payer.setOnAction(e -> {
            Composants.cacher(message);
            try {
                double m = Saisie.montant(montant.getText(), "Montant");
                ModePaiement mode = paiement.getMode();
                if (!Nav.confirmer("Confirmer le paiement de " + Format.montant(m) + " par " + Libelles.mode(mode) + " ?")) {
                    return;
                }
                Transaction tx = new CotisationService().payerCotisation(u.getId(), t.getId(), m, mode, paiement.getNumero());
                if (mode == ModePaiement.ESPECE) {
                    Composants.montrer(message, "Paiement de " + Format.montant(m) + " enregistr\u00e9 en esp\u00e8ces. "
                            + "Il sera confirm\u00e9 par le g\u00e9rant quand il recevra l'argent.", false);
                } else {
                    Composants.montrer(message, "Cotisation de " + Format.montant(m) + " enregistr\u00e9e par " + Libelles.mode(mode)
                            + " (r\u00e9f\u00e9rence " + tx.getReference() + ").", false);
                }
                payer.setDisable(true);
            } catch (RuntimeException ex) {
                Composants.montrer(message, Nav.messageErreur(ex), true);
            }
        });

        getChildren().add(Composants.carte(titre, regle, Composants.champ("Montant (FCFA)", montant), paiement, message, payer));
    }

    private static EtatCotisation etatTour(Utilisateur u, Tontine t) {
        return new CotisationService().etatTour(u.getId(), t.getId());
    }

    // acces pour les tests automatiques
    ChoixPaiement getPaiement() {
        return paiement;
    }

    Button getBoutonPayer() {
        return payer;
    }
}
