package com.tontine.ui;

import com.tontine.model.Utilisateur;
import com.tontine.service.AuthService;
import com.tontine.service.CompteSuspenduException;
import com.tontine.service.ReactivationService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Ecran de connexion (email ou telephone + mot de passe). Panneau de marque a gauche, masque sur fenetre etroite. */
public class ConnexionView extends StackPane {

    private final AuthService auth = new AuthService();

    /** @param messageSucces message a afficher en vert (ex. apres une inscription), ou null */
    public ConnexionView(String messageSucces) {
        TextField identifiant = new TextField();
        identifiant.setPromptText("Email ou num\u00e9ro de t\u00e9l\u00e9phone");
        PasswordField motDePasse = new PasswordField();
        motDePasse.setPromptText("Mot de passe");

        Label message = Composants.message();
        if (messageSucces != null) {
            Composants.montrer(message, messageSucces, false);
        }

        // zone affichee automatiquement quand le compte est suspendu : motif + bouton de demande de reactivation
        TextField motif = new TextField();
        motif.setPromptText("Motif de votre demande (obligatoire)");
        Button demander = Composants.primaire("Demander la r\u00e9activation de mon compte");
        demander.setMaxWidth(Double.MAX_VALUE);
        Label explication = Composants.sousTitre("Votre compte est suspendu. Indiquez le motif de votre demande : elle sera envoy\u00e9e \u00e0 l'administrateur, "
                + "qui vous r\u00e9pondra par notification.");
        VBox zoneReactivation = new VBox(10, explication, Composants.champ("Motif de la demande", motif), demander);
        zoneReactivation.setVisible(false);
        zoneReactivation.setManaged(false);

        Button valider = Composants.primaire("Se connecter");
        valider.setMaxWidth(Double.MAX_VALUE);
        valider.setDefaultButton(true);
        valider.setOnAction(e -> {
            Composants.cacher(message);
            zoneReactivation.setVisible(false);
            zoneReactivation.setManaged(false);
            try {
                Utilisateur u = auth.seConnecter(identifiant.getText(), motDePasse.getText());
                Nav.aller(new TableauDeBordView(u));
            } catch (CompteSuspenduException ex) {
                Composants.montrer(message, Nav.messageErreur(ex), true);
                zoneReactivation.setVisible(true); // le bouton apparait automatiquement ; le mot de passe saisi est conserve pour la demande
                zoneReactivation.setManaged(true);
            } catch (RuntimeException ex) {
                Composants.montrer(message, Nav.messageErreur(ex), true);
                motDePasse.clear();
            }
        });

        demander.setOnAction(e -> {
            Composants.cacher(message);
            try {
                new ReactivationService().demanderReactivation(identifiant.getText(), motDePasse.getText(), motif.getText());
                zoneReactivation.setVisible(false);
                zoneReactivation.setManaged(false);
                motif.clear();
                motDePasse.clear();
                Composants.montrer(message, "Votre demande de r\u00e9activation a \u00e9t\u00e9 envoy\u00e9e \u00e0 l'administrateur.", false);
            } catch (RuntimeException ex) {
                Composants.montrer(message, Nav.messageErreur(ex), true);
            }
        });

        Hyperlink oublie = new Hyperlink("Mot de passe oubli\u00e9 ?");
        oublie.setOnAction(e -> Nav.aller(new MotDePasseOublieView(identifiant.getText())));
        HBox ligneOubli = new HBox(oublie);
        ligneOubli.setAlignment(Pos.CENTER_RIGHT);

        Hyperlink inscription = new Hyperlink("Pas encore de compte ? Cr\u00e9er un compte");
        inscription.setOnAction(e -> Nav.aller(new InscriptionView()));
        Hyperlink retour = new Hyperlink("\u2190 Retour \u00e0 l'accueil");
        retour.setOnAction(e -> Nav.aller(new AccueilView()));

        Node logo = Composants.logo(false);
        Label titre = Composants.titre("Connexion");
        VBox carte = new VBox(14, logo, titre,
                Composants.sousTitre("Acc\u00e9dez \u00e0 votre espace avec votre email ou votre num\u00e9ro de t\u00e9l\u00e9phone."),
                message,
                Composants.champ("Identifiant", identifiant),
                Composants.champ("Mot de passe", motDePasse),
                ligneOubli,
                zoneReactivation,
                valider, inscription, retour);
        carte.getStyleClass().add("auth-carte");
        carte.setMaxWidth(440);
        carte.setMaxHeight(VBox.USE_PREF_SIZE);

        VBox marque = PanneauMarque.creer("Heureux de vous revoir.",
                "Retrouvez vos tontines, vos cotisations et votre historique au m\u00eame endroit.",
                "Suivi en temps r\u00e9el de chaque cotisation", "Pr\u00eats et remboursements clairs pour tous",
                "Notifications \u00e0 chaque \u00e9tape importante");
        assembler(this, marque, carte, logo);
        Effets.apparition(carte, 0);
        javafx.application.Platform.runLater(identifiant::requestFocus);
    }

    /** Disposition commune : marque a gauche (42 %), formulaire centre a droite, adaptation a la largeur. */
    static void assembler(StackPane racine, VBox marque, VBox carte, Node logoFormulaire) {
        StackPane centre = new StackPane(carte);
        centre.setPadding(new Insets(32));
        centre.setAlignment(Pos.CENTER);
        ScrollPane defilement = new ScrollPane(centre);
        defilement.setFitToWidth(true);
        defilement.setFitToHeight(true);
        defilement.getStyleClass().addAll("defilement", "auth-fond");
        HBox ligne = new HBox(marque, defilement);
        HBox.setHgrow(defilement, Priority.ALWAYS);
        marque.prefWidthProperty().bind(ligne.widthProperty().multiply(0.42));
        marque.setMinWidth(0);
        racine.getChildren().add(ligne);
        racine.getStyleClass().add("auth-fond");
        logoFormulaire.setVisible(false);
        logoFormulaire.setManaged(false);
        Adaptatif.surLargeur(racine, 880, etroit -> {
            marque.setVisible(!etroit);
            marque.setManaged(!etroit);
            logoFormulaire.setVisible(etroit);
            logoFormulaire.setManaged(etroit);
            Adaptatif.classeCompact(racine, etroit);
        });
    }
}
