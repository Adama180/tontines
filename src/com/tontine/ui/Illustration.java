package com.tontine.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/** Illustration de la page d'accueil, dessinee en JavaFX (aucune image). Les chiffres sont un exemple. */
public class Illustration extends Pane {

    public Illustration() {
        setPrefSize(440, 390);
        setMinSize(440, 390);
        setMaxSize(440, 390);

        Circle halo = new Circle(240, 195, 168);
        halo.setFill(Color.web("#e3a73b", 0.16));
        Circle halo2 = new Circle(96, 322, 64);
        halo2.setFill(Color.web("#ffffff", 0.08));

        // ---- carte principale
        Label titre = texte("Cagnotte du cycle 3", 12, "#4b5768", "Inter Medium");
        Label exemple = texte("Exemple", 10, "#7a5a00", "Inter SemiBold");
        exemple.setStyle(exemple.getStyle() + "-fx-background-color: #fdf1cc; -fx-background-radius: 10; -fx-padding: 2 8 2 8;");
        HBox haut = new HBox(titre, espace(), exemple);
        haut.setAlignment(Pos.CENTER_LEFT);
        Label montant = texte("120 000 FCFA", 29, "#16294a", "Fraunces SemiBold");

        Rectangle piste = new Rectangle(266, 10);
        piste.setArcWidth(10);
        piste.setArcHeight(10);
        piste.setFill(Color.web("#e5e7eb"));
        Rectangle rempli = new Rectangle(200, 10);
        rempli.setArcWidth(10);
        rempli.setArcHeight(10);
        rempli.setFill(Color.web("#e3a73b"));
        StackPane barre = new StackPane(piste, rempli);
        StackPane.setAlignment(rempli, Pos.CENTER_LEFT);
        Label etat = texte("6 membres sur 8 ont cotis\u00e9", 12, "#4b5768", "Inter");

        HBox avatars = new HBox(-5);
        for (String nom : new String[] { "Awa Diop", "Moussa Fall", "Fatou Ba", "Omar Sy", "Aminata Ndiaye" }) {
            Node a = Composants.avatar(nom, 34);
            ((StackPane) a).setStyle("-fx-effect: dropshadow(gaussian, rgba(255,255,255,1), 0, 3, 0, 0);");
            avatars.getChildren().add(a);
        }
        Label plus = texte("+3", 11, "#6b5410", "Inter SemiBold");
        plus.setStyle(plus.getStyle() + "-fx-background-color: #f8efd0; -fx-background-radius: 20; -fx-padding: 8 10 8 10;");
        avatars.getChildren().add(plus);

        HBox modes = new HBox(8, pastille("Wave"), pastille("Orange Money"), pastille("Esp\u00e8ces"));

        VBox carte = new VBox(12, haut, montant, barre, etat, avatars, modes);
        carte.setLayoutX(62);
        carte.setLayoutY(72);
        carte.setPrefSize(314, 252);
        carte.setStyle("-fx-background-color: white; -fx-background-radius: 24; -fx-padding: 22;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.30), 30, 0, 0, 12);");

        // ---- elements flottants
        HBox c1 = new HBox(8, Icones.icone("coche", 18), texte("Cotisation confirm\u00e9e", 12, "#16294a", "Inter SemiBold"));
        c1.setAlignment(Pos.CENTER_LEFT);
        c1.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 8 16 8 12;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 16, 0, 0, 6);");
        c1.setLayoutX(6);
        c1.setLayoutY(26);
        c1.setRotate(-4);

        HBox c2 = new HBox(8, Icones.icone("portefeuille", 18), texte("Versement re\u00e7u : 20 000 FCFA", 12, "#ffffff", "Inter SemiBold"));
        c2.setAlignment(Pos.CENTER_LEFT);
        c2.setStyle("-fx-background-color: #16294a; -fx-background-radius: 20; -fx-padding: 9 16 9 12;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.30), 16, 0, 0, 6);");
        c2.getChildren().get(0).setStyle("-fx-opacity: 1;");
        c2.setLayoutX(150);
        c2.setLayoutY(330);
        c2.setRotate(3);

        Circle piece = new Circle(27);
        piece.setFill(Color.web("#e3a73b"));
        piece.setStroke(Color.web("#e3a73b"));
        piece.setStrokeWidth(4);
        Label f = texte("F", 22, "#0f1e38", "Fraunces SemiBold");
        StackPane coin = new StackPane(piece, f);
        coin.setLayoutX(362);
        coin.setLayoutY(24);
        coin.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.30), 14, 0, 0, 6);");

        getChildren().addAll(halo, halo2, carte, c1, c2, coin);
        Effets.flotter(c1, 8, 2600, 0);
        Effets.flotter(c2, 7, 3000, 400);
        Effets.flotter(coin, 10, 3400, 800);
        setMouseTransparent(true);
    }

    private static Label texte(String t, int taille, String couleur, String police) {
        Label l = new Label(t);
        l.setStyle("-fx-font-family: \"" + police + "\", \"Inter\", sans-serif; -fx-font-size: " + taille + "px; -fx-text-fill: " + couleur + ";");
        return l;
    }

    private static Label pastille(String t) {
        Label l = texte(t, 11, "#5b4a12", "Inter Medium");
        l.setStyle(l.getStyle() + "-fx-background-color: #faf7ec; -fx-background-radius: 14; -fx-padding: 4 10 4 10; -fx-border-color: #eadfb8; -fx-border-radius: 14;");
        return l;
    }

    private static javafx.scene.layout.Region espace() {
        javafx.scene.layout.Region r = new javafx.scene.layout.Region();
        HBox.setHgrow(r, javafx.scene.layout.Priority.ALWAYS);
        return r;
    }
}
