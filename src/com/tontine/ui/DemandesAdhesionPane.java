package com.tontine.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.tontine.model.DemandeAdhesion;
import com.tontine.model.StatutDemande;
import com.tontine.model.Tontine;
import com.tontine.model.Utilisateur;
import com.tontine.service.AdhesionService;
import com.tontine.service.ConsultationService;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

/** Le gerant examine les demandes d'adhesion (informations + piece d'identite) et les accepte ou les refuse. */
public class DemandesAdhesionPane extends PaneGestion {

    private final Utilisateur gerant;
    private final Tontine tontine;
    private final AdhesionService adhesions = new AdhesionService();

    public DemandesAdhesionPane(Utilisateur gerant, Tontine tontine) {
        this.gerant = gerant;
        this.tontine = tontine;
        rafraichir();
    }

    @Override
    protected void construire() {
        ConsultationService consultation = new ConsultationService();
        Tontine t = consultation.detail(tontine.getId());
        getChildren().add(vide("Places : " + consultation.nombreMembres(t.getId()) + " / " + t.getNbMembreMax() + ". "
                + (t.isFondsCommun() ? "" : "Dans cette tontine, une adh\u00e9sion ne peut \u00eatre accept\u00e9e qu'entre deux tours.")));

        List<DemandeAdhesion> attente = adhesions.listerDemandes(gerant.getId(), t.getId(), StatutDemande.EN_ATTENTE);
        getChildren().add(titreSection("Demandes en attente (" + attente.size() + ")"));
        if (attente.isEmpty()) {
            getChildren().add(vide("Aucune demande en attente."));
        }
        for (DemandeAdhesion d : attente) {
            String empreinte = d.getPieceHash() == null ? "" : d.getPieceHash().substring(0, Math.min(12, d.getPieceHash().length())) + "...";
            VBox infos = texte(d.getNom(), d.getTelephone() + " - " + d.getEmail() + " - envoy\u00e9e le " + Aff.date(d.getDateEnvoi())
                    + "\nPi\u00e8ce d'identit\u00e9 : empreinte " + empreinte);
            String identite = d.getNom() + " - " + d.getTelephone() + " - " + d.getEmail();
            Node vignette = VisionneusePiece.vignette(d.getPieceChemin(), d.getNom(), "Demandeur : " + identite, this::erreur);
            Button voir = Composants.contour("Voir la pi\u00e8ce");
            voir.setOnAction(e -> VisionneusePiece.voir(d.getPieceChemin(), d.getNom(), "Demandeur : " + identite, this::erreur));
            Button accepter = Composants.primaire("Accepter");
            accepter.setOnAction(e -> action(() -> {
                if (Nav.confirmer("Accepter " + d.getNom() + " dans la tontine ?")) {
                    adhesions.validerDemandeAdhesion(gerant.getId(), d.getId(), true);
                    succes(d.getNom() + " est maintenant membre de la tontine.");
                }
            }));
            Button refuser = Composants.contour("Refuser");
            refuser.setOnAction(e -> action(() -> {
                if (Nav.confirmer("Refuser la demande de " + d.getNom() + " ?")) {
                    adhesions.validerDemandeAdhesion(gerant.getId(), d.getId(), false);
                    succes("La demande de " + d.getNom() + " a \u00e9t\u00e9 refus\u00e9e.");
                }
            }));
            getChildren().add(ligne(infos, vignette, voir, refuser, accepter));
        }

        // ---- demandes deja traitees
        List<DemandeAdhesion> traitees = new ArrayList<>();
        traitees.addAll(adhesions.listerDemandes(gerant.getId(), t.getId(), StatutDemande.ACCEPTEE));
        traitees.addAll(adhesions.listerDemandes(gerant.getId(), t.getId(), StatutDemande.REFUSEE));
        traitees.sort(Comparator.comparing(DemandeAdhesion::getDateEnvoi).reversed());
        if (!traitees.isEmpty()) {
            getChildren().add(titreSection("Demandes d\u00e9j\u00e0 trait\u00e9es"));
            for (DemandeAdhesion d : traitees.subList(0, Math.min(10, traitees.size()))) {
                getChildren().add(ligne(texte(d.getNom(), "Envoy\u00e9e le " + Aff.date(d.getDateEnvoi())), Aff.badgeStatut(d.getStatut())));
            }
        }
    }

}
