package com.tontine.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Panneau de gauche des ecrans de connexion et d'inscription : message d'accueil et points forts. */
public final class PanneauMarque {

    private PanneauMarque() {
    }

    public static VBox creer(String titre, String texte, String... points) {
        Label t = new Label(titre);
        t.getStyleClass().add("marque-titre");
        t.setWrapText(true);
        Label d = new Label(texte);
        d.getStyleClass().add("marque-texte");
        d.setWrapText(true);
        VBox v = new VBox(18, Composants.logo(true), spacer(18), t, d);
        for (String p : points) {
            Label l = new Label(p);
            l.setWrapText(true);
            javafx.scene.Node ic = Icones.icone("coche", 20);
            ((javafx.scene.Group) ic).getChildren().stream().filter(n -> n instanceof javafx.scene.shape.SVGPath).forEach(n -> n.getStyleClass().add("icone-claire"));
            HBox ligne = new HBox(10, ic, l);
            ligne.setAlignment(Pos.TOP_LEFT);
            ligne.getStyleClass().add("marque-point");
            v.getChildren().add(ligne);
        }
        Region bas = new Region();
        VBox.setVgrow(bas, Priority.ALWAYS);
        v.getChildren().add(bas);
        Label paiements = new Label("Paiements pris en charge");
        paiements.getStyleClass().add("marque-texte");
        javafx.scene.layout.FlowPane modes = new javafx.scene.layout.FlowPane(8, 8, puce("Wave"), puce("Orange Money"));
        v.getChildren().addAll(paiements, modes);
        v.getStyleClass().add("panneau-marque");
        return v;
    }

    private static javafx.scene.Node puce(String texte) {
        HBox h = new HBox(new Label(texte));
        h.getStyleClass().add("hero-puce");
        return h;
    }

    private static Region spacer(double h) {
        Region r = new Region();
        r.setPrefHeight(h);
        return r;
    }
}
