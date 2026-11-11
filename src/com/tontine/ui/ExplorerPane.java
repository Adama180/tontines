package com.tontine.ui;

import java.util.List;

import com.tontine.model.Tontine;
import com.tontine.model.TypeTontine;
import com.tontine.model.Utilisateur;
import com.tontine.service.ConsultationService;
import com.tontine.service.Format;
import com.tontine.service.Libelles;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Liste des tontines ouvertes : consultable par un visiteur comme par un utilisateur connecte. */
public class ExplorerPane extends VBox {

    private static final String TOUS = "Tous les types";

    private final Utilisateur utilisateur; // null = visiteur
    private final Fenetre fenetre;
    private final ConsultationService consultation = new ConsultationService();
    private final ChoiceBox<String> filtreType = new ChoiceBox<>();
    private final TextField recherche = new TextField();
    private final FlowPane cartes = new FlowPane(16, 16);

    public ExplorerPane(Utilisateur utilisateur, Fenetre fenetre) {
        super(16);
        this.utilisateur = utilisateur;
        this.fenetre = fenetre;

        filtreType.getItems().add(TOUS);
        for (TypeTontine t : consultation.listerTypes()) {
            filtreType.getItems().add(Libelles.type(t));
        }
        filtreType.getSelectionModel().selectFirst();
        recherche.setPromptText("Rechercher une tontine par son nom");
        recherche.setPrefWidth(320);

        Button creer = Composants.or("Cr\u00e9er ma propre tontine");
        creer.setOnAction(e -> {
            if (utilisateur == null) {
                Nav.aller(PagePublique.creerTontine());
            } else {
                fenetre.afficher("Cr\u00e9er ma tontine", new CreerTontineView(utilisateur, fenetre));
            }
        });
        HBox filtres = new HBox(12, recherche, filtreType, Composants.espaceur(), creer);
        filtres.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(Composants.titre("Explorer les tontines"),
                Composants.sousTitre("Comparez les tontines et leurs r\u00e8gles avant de vous engager. "
                        + "L'historique des transactions reste r\u00e9serv\u00e9 aux membres accept\u00e9s."),
                filtres, cartes);

        filtreType.getSelectionModel().selectedIndexProperty().addListener((o, a, b) -> rafraichir());
        recherche.textProperty().addListener((o, a, b) -> rafraichir());
        rafraichir();
    }

    private void rafraichir() {
        cartes.getChildren().clear();
        List<Tontine> liste;
        try {
            int i = filtreType.getSelectionModel().getSelectedIndex();
            liste = i <= 0 ? consultation.listerToutesLesTontines()
                    : consultation.listerTontines(TypeTontine.values()[i - 1]);
        } catch (RuntimeException e) {
            Label l = new Label(Nav.messageErreur(e));
            l.getStyleClass().add("message-erreur");
            cartes.getChildren().add(l);
            return;
        }
        String q = recherche.getText() == null ? "" : recherche.getText().trim().toLowerCase();
        int affichees = 0;
        for (Tontine t : liste) {
            if (!q.isEmpty() && !t.getNom().toLowerCase().contains(q)) {
                continue;
            }
            cartes.getChildren().add(carte(t));
            affichees++;
        }
        if (affichees == 0) {
            cartes.getChildren().add(Composants.etatVide("recherche", "Aucune tontine \u00e0 afficher", liste.isEmpty()
                    ? "Aucune tontine n'est encore ouverte. Vous pouvez cr\u00e9er la premi\u00e8re !"
                    : "Aucune tontine ne correspond \u00e0 votre recherche."));
        }
    }

    private static String classeType(TypeTontine type) {
        return type == TypeTontine.FONDS_COMMUN ? "type-fonds" : type == TypeTontine.TOUR_DE_ROLE ? "type-tour" : "type-tirage";
    }

    private VBox carte(Tontine t) {
        Label nom = new Label(t.getNom());
        nom.getStyleClass().add("carte-titre");
        nom.setWrapText(true);
        int membres = consultation.nombreMembres(t.getId());
        Label cotisation = Composants.sousTitre("Cotisation : " + Libelles.cotisation(t)
                + " (" + Libelles.frequence(t.getFrequence()) + ")");
        Label places = Composants.sousTitre("Membres : " + membres + " / " + t.getNbMembreMax()
                + (membres >= t.getNbMembreMax() ? "  - complet" : ""));
        Button voir = Composants.primaire("Voir les r\u00e8gles");
        voir.setOnAction(e -> fenetre.afficher(t.getNom(), new DetailTontineView(utilisateur, fenetre, t)));
        VBox c = Composants.carte(nom, Aff.badge(Libelles.type(t.getType())), cotisation, places, voir);
        c.getStyleClass().addAll("carte-survol", classeType(t.getType()));
        Effets.survol(c);
        c.setPrefWidth(300);
        c.setMinWidth(260);
        HBox.setHgrow(c, Priority.NEVER);
        return c;
    }
}
