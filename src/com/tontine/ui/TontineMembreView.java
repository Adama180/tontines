package com.tontine.ui;

import java.util.function.Supplier;

import com.tontine.model.Adhesion;
import com.tontine.model.Tontine;
import com.tontine.model.Utilisateur;
import com.tontine.service.ConsultationService;
import com.tontine.service.Libelles;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Espace d'un membre dans UNE tontine, en onglets. Chaque onglet est reconstruit quand on l'ouvre :
 * les donnees affichees sont toujours a jour.
 */
public class TontineMembreView extends VBox {

    protected final Utilisateur utilisateur;
    protected final Tontine tontine;
    protected final Fenetre fenetre;
    private final TabPane onglets = new TabPane();

    public TontineMembreView(Utilisateur utilisateur, Tontine tontine, Fenetre fenetre) {
        super(14);
        this.utilisateur = utilisateur;
        this.tontine = tontine;
        this.fenetre = fenetre;

        Hyperlink retour = new Hyperlink("\u2190 Mes tontines");
        retour.setOnAction(e -> fenetre.afficher("Mes tontines", new MesTontinesView(utilisateur, fenetre)));

        Adhesion a = new ConsultationService().monAdhesion(utilisateur.getId(), tontine.getId());
        Label nom = Composants.titre(tontine.getNom());
        Label role = new Label(a == null ? "" : Libelles.role(a.getRole()));
        role.getStyleClass().add("badge");
        HBox entete = new HBox(12, nom, Aff.badge(Libelles.type(tontine.getType())), role);
        entete.setAlignment(Pos.CENTER_LEFT);

        ajouterOnglet("Vue d'ensemble", () -> new VueEnsemblePane(utilisateur, tontine));
        ajouterOnglet("Cotiser", () -> new CotiserPane(utilisateur, tontine));
        ajouterOnglet("Historique", () -> new HistoriquePane(utilisateur, tontine));
        if (tontine.isFondsCommun()) {
            ajouterOnglet("Pr\u00eats", () -> new PretsPane(utilisateur, tontine));
        }
        if (tontine.isTourDeRole()) {
            ajouterOnglet("Tour de r\u00f4le", () -> new TourPane(utilisateur, tontine));
        }
        if (tontine.isTirageAuSort()) {
            ajouterOnglet("Tirage au sort", () -> new TiragePane(utilisateur, tontine));
        }
        ajouterOnglet("Ma cagnotte", () -> new CagnottePane(utilisateur, tontine));
        ajouterOngletsSupplementaires();

        onglets.getSelectionModel().selectedItemProperty().addListener((o, ancien, tab) -> charger(tab));
        charger(onglets.getSelectionModel().getSelectedItem());
        getChildren().addAll(retour, entete, onglets);
    }

    /** Onglets reserves au gerant de la tontine (partie 5d). */
    protected void ajouterOngletsSupplementaires() {
        if (tontine.getGerantId() != utilisateur.getId()) {
            return;
        }
        ajouterOnglet("Demandes d'adh\u00e9sion", () -> new DemandesAdhesionPane(utilisateur, tontine));
        ajouterOnglet("Membres", () -> new MembresGerantPane(utilisateur, tontine));
        ajouterOnglet("Esp\u00e8ces", () -> new EspecesPane(utilisateur, tontine));
        if (tontine.isFondsCommun()) {
            ajouterOnglet("Pr\u00eats \u00e0 valider", () -> new PretsAValiderPane(utilisateur, tontine));
        }
        ajouterOnglet(tontine.isFondsCommun() ? "Cycles" : "Tours", () -> new CyclesPane(utilisateur, tontine));
    }

    protected final void ajouterOnglet(String titre, Supplier<Node> contenu) {
        Tab tab = new Tab(titre);
        tab.setClosable(false);
        tab.setUserData(contenu);
        onglets.getTabs().add(tab);
    }

    @SuppressWarnings("unchecked")
    private void charger(Tab tab) {
        if (tab == null) {
            return;
        }
        Node noeud;
        try {
            noeud = ((Supplier<Node>) tab.getUserData()).get();
        } catch (RuntimeException e) {
            Label erreur = new Label(Nav.messageErreur(e));
            erreur.setWrapText(true);
            erreur.getStyleClass().add("message-erreur");
            noeud = erreur;
        }
        tab.setContent(noeud);
    }

    /** Ouvre l'onglet portant ce titre (utile pour les tests et la navigation interne). */
    public void ouvrirOnglet(String titre) {
        for (Tab t : onglets.getTabs()) {
            if (t.getText().equals(titre)) {
                onglets.getSelectionModel().select(t);
                charger(t);
                return;
            }
        }
    }
}
