package com.tontine.ui;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.tontine.model.DemandeCreation;
import com.tontine.model.Tontine;
import com.tontine.model.Utilisateur;
import com.tontine.service.AdminService;
import com.tontine.service.Format;
import com.tontine.service.Libelles;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

/** L'administrateur examine les demandes de creation de tontine : il en fait un gerant ou refuse. */
public class DemandesCreationAdminPane extends PaneGestion {

    private final Utilisateur admin;
    private final AdminService service = new AdminService();

    public DemandesCreationAdminPane(Utilisateur admin) {
        this.admin = admin;
        rafraichir();
    }

    @Override
    protected void construire() {
        Map<Integer, Utilisateur> users = service.listerUtilisateurs(admin.getId()).stream()
                .collect(Collectors.toMap(Utilisateur::getId, u -> u));
        List<DemandeCreation> attente = service.demandesCreationEnAttente(admin.getId());

        getChildren().add(titreSection("Demandes en attente (" + attente.size() + ")"));
        if (attente.isEmpty()) {
            getChildren().add(vide("Aucune demande de cr\u00e9ation en attente."));
        }
        for (DemandeCreation d : attente) {
            Utilisateur u = users.get(d.getDemandeurId());
            String demandeur = u == null ? "?" : u.getNomComplet() + " - " + u.getTelephone() + " - " + u.getEmail();
            String piece = d.getPieceHash() == null || d.getPieceHash().isEmpty()
                    ? "Aucune pi\u00e8ce jointe (demande ant\u00e9rieure \u00e0 l'ajout de la pi\u00e8ce)"
                    : "Pi\u00e8ce d'identit\u00e9 : empreinte " + d.getPieceHash().substring(0, Math.min(12, d.getPieceHash().length())) + "...";
            VBox infos = texte("\u00ab " + d.getNomSouhaite() + " \u00bb - " + Libelles.type(d.getTypeSouhaite()),
                    Libelles.reglesDemande(d)
                            + "\nDemandeur : " + demandeur + "\nEnvoy\u00e9e le " + Aff.date(d.getDateEnvoi())
                            + "\n" + piece);
            Button voir = Composants.contour("Voir la pi\u00e8ce");
            voir.setDisable(d.getPieceChemin() == null);
            String titrePiece = u == null ? d.getNomSouhaite() : u.getNomComplet();
            String infoPiece = "Demandeur : " + demandeur;
            Node vignette = VisionneusePiece.vignette(d.getPieceChemin(), titrePiece, infoPiece, this::erreur);
            voir.setOnAction(e -> VisionneusePiece.voir(d.getPieceChemin(), titrePiece, infoPiece, this::erreur));
            Button accepter = Composants.primaire("Accepter");
            accepter.setOnAction(e -> action(() -> {
                if (Nav.confirmer("Cr\u00e9er la tontine \u00ab " + d.getNomSouhaite() + " \u00bb et en nommer le demandeur g\u00e9rant ?")) {
                    Tontine t = service.validerDemandeCreation(admin.getId(), d.getId(), true);
                    succes("La tontine \u00ab " + t.getNom() + " \u00bb est cr\u00e9\u00e9e. "
                            + (u == null ? "" : u.getNomComplet()) + " en est le g\u00e9rant et en est inform\u00e9.");
                }
            }));
            Button refuser = Composants.contour("Refuser");
            refuser.setOnAction(e -> action(() -> {
                if (Nav.confirmer("Refuser la demande de cr\u00e9ation de \u00ab " + d.getNomSouhaite() + " \u00bb ?")) {
                    service.validerDemandeCreation(admin.getId(), d.getId(), false);
                    succes("La demande \u00ab " + d.getNomSouhaite() + " \u00bb a \u00e9t\u00e9 refus\u00e9e.");
                }
            }));
            getChildren().add(ligne(infos, vignette, voir, refuser, accepter));
        }

        List<DemandeCreation> traitees = service.demandesCreationTraitees(admin.getId());
        if (!traitees.isEmpty()) {
            getChildren().add(titreSection("Demandes d\u00e9j\u00e0 trait\u00e9es"));
            for (DemandeCreation d : traitees.subList(0, Math.min(10, traitees.size()))) {
                Utilisateur u = users.get(d.getDemandeurId());
                getChildren().add(ligne(texte(d.getNomSouhaite(), (u == null ? "?" : u.getNomComplet()) + " - " + Aff.date(d.getDateEnvoi())),
                        Aff.badgeStatut(d.getStatut())));
            }
        }
    }
}
