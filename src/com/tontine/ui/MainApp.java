package com.tontine.ui;

import com.tontine.util.ConnexionBD;
import com.tontine.util.InitBase;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Screen;
import javafx.stage.Stage;

/** Fenetre principale de l'application. */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        Polices.charger();
        stage.setTitle("NAT Tontines - Gestion de tontines");

        // taille adaptee a l'ecran (portable 1366 x 768 comme grand ecran)
        Rectangle2D ecran = Screen.getPrimary().getVisualBounds();
        double largeur = Math.min(1280, ecran.getWidth() * 0.94);
        double hauteur = Math.min(820, ecran.getHeight() * 0.94);
        stage.setMinWidth(Math.min(760, largeur));
        stage.setMinHeight(Math.min(560, hauteur));
        Nav.init(stage, largeur, hauteur);
        Image icone = icone();
        if (icone != null) {
            stage.getIcons().add(icone);
        }

        if (!ConnexionBD.tester()) {
            Nav.aller(vueBaseInaccessible());
        } else {
            try {
                InitBase.creerAdminParDefaut(); // garantit qu'un compte administrateur existe
            } catch (RuntimeException e) {
                System.err.println("Initialisation du compte administrateur impossible : " + e.getMessage());
            }
            Nav.aller(new AccueilView());
            demarrerRetraitAutomatique();
        }
        stage.show();
    }

    /**
     * Renouvellement des tontines : toutes les minutes, les membres qui n'ont pas repondu a l'alerte avant la date
     * limite sont retires automatiquement (sans action du gerant).
     */
    private void demarrerRetraitAutomatique() {
        Runnable tache = () -> {
            try {
                new com.tontine.service.RenouvellementService().appliquerEcheances();
            } catch (RuntimeException e) {
                System.err.println("Retrait automatique impossible pour l'instant : " + e.getMessage());
            }
        };
        tache.run();
        javafx.animation.Timeline minuteur = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.minutes(1), e -> tache.run()));
        minuteur.setCycleCount(javafx.animation.Animation.INDEFINITE);
        minuteur.play();
    }

    /** Icone de la fenetre : la pastille doree du logo, dessinee a l'execution (aucun fichier image). */
    private Image icone() {
        try {
            Circle rond = new Circle(30, Color.web("#e3a73b"));
            Label n = new Label("N");
            n.setStyle("-fx-font-family: \"Fraunces SemiBold\", sans-serif; -fx-font-size: 30px; -fx-text-fill: #0f1e38;");
            StackPane p = new StackPane(rond, n);
            p.setPrefSize(64, 64);
            p.setMinSize(64, 64);
            p.setMaxSize(64, 64);
            SnapshotParameters sp = new SnapshotParameters();
            sp.setFill(Color.TRANSPARENT);
            return p.snapshot(sp, null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Ecran affiche si MySQL n'est pas joignable (XAMPP non demarre, base absente...). */
    private StackPane vueBaseInaccessible() {
        Label titre = new Label("Base de donn\u00e9es inaccessible");
        titre.getStyleClass().add("titre");
        Label texte = new Label("Ouvrez le panneau XAMPP, d\u00e9marrez MySQL (bouton Start), puis cliquez sur R\u00e9essayer.\n"
                + "V\u00e9rifiez aussi que la base 'tontine' existe dans phpMyAdmin.");
        texte.setWrapText(true);
        texte.getStyleClass().add("sous-titre");
        Button reessayer = Composants.primaire("R\u00e9essayer");
        reessayer.setOnAction(e -> {
            if (ConnexionBD.tester()) {
                try {
                    InitBase.creerAdminParDefaut();
                } catch (RuntimeException ex) {
                    System.err.println(ex.getMessage());
                }
                Nav.aller(new AccueilView());
            }
        });
        VBox carte = Composants.carte(Composants.iconeRonde("eclair", "or"), titre, texte, reessayer);
        carte.setMaxWidth(500);
        carte.setMaxHeight(VBox.USE_PREF_SIZE);
        StackPane fond = new StackPane(carte);
        fond.getStyleClass().add("fond");
        fond.setAlignment(Pos.CENTER);
        return fond;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
