package com.tontine.ui;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.tontine.model.StatutTontine;
import com.tontine.model.Tontine;
import com.tontine.model.Utilisateur;
import com.tontine.service.AdminService;
import com.tontine.service.ConsultationService;
import com.tontine.service.Format;
import com.tontine.service.Indicateurs;
import com.tontine.service.Libelles;
import com.tontine.service.StatistiquesService;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Suivi de toutes les tontines de la plateforme, avec le detail de chacune. */
public class TontinesAdminPane extends PaneGestion {

    private final Utilisateur admin;
    private final Set<Integer> ouvertes = new HashSet<>();

    public TontinesAdminPane(Utilisateur admin) {
        this.admin = admin;
        rafraichir();
    }

    @Override
    protected void construire() {
        AdminService service = new AdminService();
        ConsultationService consultation = new ConsultationService();
        Map<Integer, Utilisateur> users = service.listerUtilisateurs(admin.getId()).stream()
                .collect(Collectors.toMap(Utilisateur::getId, u -> u));
        List<Tontine> tontines = service.listerToutesTontines(admin.getId());

        getChildren().add(titreSection("Tontines (" + tontines.size() + ")"));
        if (tontines.isEmpty()) {
            getChildren().add(vide("Aucune tontine n'a encore \u00e9t\u00e9 cr\u00e9\u00e9e."));
        }
        for (Tontine t : tontines) {
            Utilisateur g = users.get(t.getGerantId());
            boolean cloturee = t.getStatut() == StatutTontine.CLOTUREE;
            Label statut = new Label(cloturee ? "Termin\u00e9e" : "Ouverte");
            statut.getStyleClass().addAll("badge-statut", cloturee ? "statut-refusee" : "statut-acceptee");
            VBox infos = texte(t.getNom() + " - " + Libelles.type(t.getType()),
                    "G\u00e9rant : " + (g == null ? "?" : g.getNomComplet()) + " - cotisation " + Libelles.cotisation(t)
                            + " (" + Libelles.frequence(t.getFrequence()) + ") - " + consultation.nombreMembres(t.getId()) + " / "
                            + t.getNbMembreMax() + " membres");
            boolean ouvert = ouvertes.contains(t.getId());
            Button details = Composants.contour(ouvert ? "Masquer" : "D\u00e9tails");
            details.setOnAction(e -> {
                if (!ouvertes.remove(t.getId())) {
                    ouvertes.add(t.getId());
                }
                rafraichir();
            });
            getChildren().add(ligne(infos, statut, details));
            if (ouvert) {
                Indicateurs ind = new StatistiquesService().tontine(admin.getId(), t.getId());
                VBox detail = new VBox(6);
                detail.getStyleClass().add("carte");
                ind.getValeurs().forEach((k, v) -> {
                    Label l = new Label(k + " : " + v);
                    l.setWrapText(true);
                    detail.getChildren().add(l);
                });
                getChildren().add(detail);
            }
        }
    }

    // acces pour les tests automatiques
    Button premierBoutonDetails() {
        for (javafx.scene.Node n : getChildren()) {
            if (n instanceof javafx.scene.Parent) {
                for (javafx.scene.Node c : ((javafx.scene.Parent) n).getChildrenUnmodifiable()) {
                    if (c instanceof Button) {
                        return (Button) c;
                    }
                }
            }
        }
        return null;
    }
}
