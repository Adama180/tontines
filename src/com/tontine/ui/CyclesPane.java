package com.tontine.ui;

import java.util.List;
import java.util.Map;

import com.tontine.model.Cycle;
import com.tontine.model.DemandeTourAnticipe;
import com.tontine.model.Distribution;
import com.tontine.model.StatutCycle;
import com.tontine.model.Tirage;
import com.tontine.model.Tontine;
import com.tontine.model.Utilisateur;
import com.tontine.service.ConsultationService;
import com.tontine.service.CotisationService;
import com.tontine.service.CycleService;
import com.tontine.service.Format;
import com.tontine.service.Libelles;
import com.tontine.service.MembreService;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Gestion des cycles : demarrer, cloturer (versements), tirage au sort, tour de role et tours anticipes. */
public class CyclesPane extends PaneGestion {

    private final Utilisateur gerant;
    private final Tontine tontine;
    private final CycleService cycles = new CycleService();
    private final MembreService membre = new MembreService();
    private Button boutonDemarrer;
    private Button boutonCloturer;
    private Button boutonTirage;

    public CyclesPane(Utilisateur gerant, Tontine tontine) {
        this.gerant = gerant;
        this.tontine = tontine;
        rafraichir();
    }

    private String mot() {
        return Libelles.cycle(tontine);
    }

    private String motMaj() {
        return Libelles.cycleMaj(tontine);
    }

    @Override
    protected void construire() {
        boutonDemarrer = null;
        boutonCloturer = null;
        boutonTirage = null;
        Tontine t = new ConsultationService().detail(tontine.getId());
        Cycle cycle = membre.cycleEnCours(t.getId());
        Map<Integer, String> noms = membre.nomsMembres(gerant.getId(), t.getId());
        Map<Integer, Integer> numeros = membre.numerosDeCycle(t.getId());

        // ---- etat du cycle
        VBox etat;
        if (cycle == null) {
            boutonDemarrer = Composants.primaire("D\u00e9marrer un nouveau " + mot());
            boutonDemarrer.setOnAction(e -> action(() -> {
                Cycle c = cycles.demarrerCycle(gerant.getId(), t.getId());
                succes("Le " + mot() + " " + c.getNumero() + " a d\u00e9marr\u00e9. Les membres sont pr\u00e9venus.");
            }));
            etat = Composants.carte(titreSection("Aucun " + mot() + " en cours"),
                    vide((t.isFondsCommun()
                            ? "Le " + mot() + " se d\u00e9marre une seule fois : il dure " + t.getDureeEnCycle() + " p\u00e9riode(s) ("
                                    + Libelles.periode(t.getFrequence()) + ") et chaque membre y cotise une fois par "
                                    + Libelles.periode(t.getFrequence()) + ". Les membres pourront aussi emprunter."
                            : "D\u00e9marrez un " + mot() + " pour que les membres puissent cotiser.")),
                    boutonDemarrer);
        } else {
            boutonCloturer = Composants.primaire("Cl\u00f4turer le " + mot() + " " + cycle.getNumero());
            boutonCloturer.setOnAction(e -> action(() -> {
                if (!Nav.confirmer("Cl\u00f4turer le " + mot() + " " + cycle.getNumero() + " ? Les sommes seront vers\u00e9es aux b\u00e9n\u00e9ficiaires.")) {
                    return;
                }
                List<Distribution> v = cycles.cloturerCycle(gerant.getId(), t.getId());
                StringBuilder sb = new StringBuilder(motMaj() + " " + cycle.getNumero() + " cl\u00f4tur\u00e9. Versements : ");
                for (int i = 0; i < v.size(); i++) {
                    sb.append(i == 0 ? "" : " ; ").append(noms.getOrDefault(v.get(i).getBeneficiaireId(), "?"))
                            .append(" ").append(Format.montant(v.get(i).getMontantTotal()));
                }
                if (v.isEmpty()) {
                    sb.append("aucun (aucune cotisation \u00e0 verser).");
                }
                succes(sb.toString());
            }));
            etat = Composants.carte(titreSection(motMaj() + " " + cycle.getNumero() + " en cours depuis le " + cycle.getDateDebut()),
                    vide(t.isFondsCommun()
                            ? "Fin pr\u00e9vue le " + Format.date(CotisationService.finDeCycle(cycle, t)) + ". \u00c0 la fin, chaque membre re\u00e7oit "
                                    + "sa cagnotte plus une part \u00e9gale des p\u00e9nalit\u00e9s de retard et des int\u00e9r\u00eats. "
                                    + "Tous les pr\u00eats doivent \u00eatre rembours\u00e9s et les paiements en esp\u00e8ces confirm\u00e9s."
                            : "\u00c0 la cl\u00f4ture, le b\u00e9n\u00e9ficiaire du " + mot() + " re\u00e7oit le total collect\u00e9. "
                                    + "Les paiements en esp\u00e8ces doivent \u00eatre confirm\u00e9s."),
                    boutonCloturer);
        }
        getChildren().add(etat);

        // ---- membres qui n'ont pas cotise
        if (cycle != null) {
            List<Utilisateur> sans = new CotisationService().membresSansCotisation(t.getId());
            getChildren().add(titreSection("Cotisations du " + mot()));
            if (sans.isEmpty()) {
                getChildren().add(vide("Tous les membres ont cotis\u00e9 dans ce " + mot() + "."));
            } else {
                StringBuilder sb = new StringBuilder("Pas encore cotis\u00e9 (" + sans.size() + ") : ");
                for (int i = 0; i < sans.size(); i++) {
                    sb.append(i == 0 ? "" : ", ").append(sans.get(i).getNomComplet());
                }
                getChildren().add(vide(sb.toString()));
            }
        }

        // ---- tirage au sort
        if (t.isTirageAuSort()) {
            getChildren().add(titreSection("Tirage au sort"));
            List<Tirage> tirages = cycles.listerTirages(gerant.getId(), t.getId());
            Tirage ducycle = null;
            for (Tirage ti : tirages) {
                if (cycle != null && ti.getSemaine() == cycle.getNumero()) {
                    ducycle = ti;
                }
            }
            if (cycle == null) {
                getChildren().add(vide("Le tirage se fait pendant un " + mot() + " en cours."));
            } else if (ducycle != null) {
                getChildren().add(ligne(texte("Tirage du " + mot() + " " + cycle.getNumero() + " effectu\u00e9",
                        "B\u00e9n\u00e9ficiaire : " + noms.getOrDefault(ducycle.getBeneficiaireId(), "?")
                                + ". Le r\u00e9sultat est d\u00e9finitif : seul l'administrateur peut le corriger, avec une trace d'audit.")));
            } else {
                boutonTirage = Composants.or("Lancer le tirage au sort");
                boutonTirage.setOnAction(e -> action(() -> {
                    if (!Nav.confirmer("Lancer le tirage au sort du " + mot() + " " + cycle.getNumero() + " ? Le r\u00e9sultat est d\u00e9finitif.")) {
                        return;
                    }
                    Tirage ti = cycles.lancerTirage(gerant.getId(), t.getId());
                    succes("Tirage effectu\u00e9 : " + noms.getOrDefault(ti.getBeneficiaireId(), "?") + " recevra la cagnotte du " + mot() + " "
                            + cycle.getNumero() + ".");
                }));
                getChildren().add(ligne(texte("Tirage du " + mot() + " " + cycle.getNumero() + " pas encore fait",
                        "Le gagnant est tir\u00e9 parmi les membres qui n'ont pas encore \u00e9t\u00e9 tir\u00e9s dans le tour en cours."), boutonTirage));
            }
        }

        // ---- tour de role
        if (t.isTourDeRole()) {
            getChildren().add(titreSection("Tour de r\u00f4le"));
            try {
                getChildren().add(ligne(texte("Prochain b\u00e9n\u00e9ficiaire", cycles.prochainBeneficiaire(t.getId()).getNomComplet())));
            } catch (RuntimeException e) {
                getChildren().add(vide("Aucun membre actif."));
            }
            List<DemandeTourAnticipe> demandes = cycles.demandesTourAnticipe(gerant.getId(), t.getId());
            getChildren().add(titreSection("Demandes de tour anticip\u00e9 (" + demandes.size() + ")"));
            if (demandes.isEmpty()) {
                getChildren().add(vide("Aucune demande en attente. Une demande ne peut \u00eatre accept\u00e9e qu'entre deux " + mot() + "s."));
            }
            for (DemandeTourAnticipe d : demandes) {
                Button accepter = Composants.primaire("Accepter");
                accepter.setOnAction(e -> action(() -> {
                    cycles.traiterDemandeTourAnticipe(gerant.getId(), d.getId(), true);
                    succes("Tour anticip\u00e9 accord\u00e9 \u00e0 " + noms.getOrDefault(d.getUserId(), "?") + " : il passe avant le prochain b\u00e9n\u00e9ficiaire.");
                }));
                Button refuser = Composants.contour("Refuser");
                refuser.setOnAction(e -> action(() -> {
                    cycles.traiterDemandeTourAnticipe(gerant.getId(), d.getId(), false);
                    succes("La demande de tour anticip\u00e9 a \u00e9t\u00e9 refus\u00e9e.");
                }));
                getChildren().add(ligne(texte(noms.getOrDefault(d.getUserId(), "?"), d.getMotif() + " - " + Aff.date(d.getDateDemande())),
                        refuser, accepter));
            }
        }

        // ---- versements effectues
        List<Distribution> versements = cycles.listerVersements(gerant.getId(), t.getId());
        getChildren().add(titreSection("Versements effectu\u00e9s"));
        if (versements.isEmpty()) {
            getChildren().add(vide("Aucun versement pour le moment."));
        }
        for (Distribution d : versements.subList(0, Math.min(10, versements.size()))) {
            Label montant = new Label(Format.montant(d.getMontantTotal()));
            montant.getStyleClass().add("carte-titre");
            getChildren().add(ligne(texte(noms.getOrDefault(d.getBeneficiaireId(), "?"),
                    motMaj() + " " + numeros.getOrDefault(d.getCycleId(), 0) + " - " + Aff.date(d.getDate())), montant));
        }

        // ---- historique des cycles
        getChildren().add(titreSection("Historique des " + mot() + "s"));
        List<Cycle> tous = cycles.listerCycles(gerant.getId(), t.getId());
        if (tous.isEmpty()) {
            getChildren().add(vide("Aucun " + mot() + " pour le moment."));
        }
        for (int i = tous.size() - 1; i >= 0; i--) {
            Cycle c = tous.get(i);
            Label statut = new Label(c.getStatut() == StatutCycle.EN_COURS ? "En cours" : "Cl\u00f4tur\u00e9");
            statut.getStyleClass().addAll("badge-statut", c.getStatut() == StatutCycle.EN_COURS ? "statut-attente" : "statut-acceptee");
            getChildren().add(ligne(texte(motMaj() + " " + c.getNumero(), "Du " + c.getDateDebut()
                    + (c.getDateFin() == null ? "" : " au " + c.getDateFin())), statut));
        }
    }

    // acces pour les tests automatiques
    Button getBoutonDemarrer() { return boutonDemarrer; }
    Button getBoutonCloturer() { return boutonCloturer; }
    Button getBoutonTirage() { return boutonTirage; }
}
