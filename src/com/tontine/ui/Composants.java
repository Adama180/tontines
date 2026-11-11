package com.tontine.ui;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/** Petits elements d'interface reutilises par tous les ecrans. */
public final class Composants {

    private Composants() {
    }

    public static Label titre(String texte) {
        Label l = new Label(texte);
        l.getStyleClass().add("titre");
        l.setWrapText(true);
        return l;
    }

    public static Label sousTitre(String texte) {
        Label l = new Label(texte);
        l.getStyleClass().add("sous-titre");
        l.setWrapText(true);
        return l;
    }

    public static Label etiquette(String texte) {
        Label l = new Label(texte);
        l.getStyleClass().add("champ-label");
        return l;
    }

    public static Button primaire(String texte) {
        Button b = new Button(texte);
        b.getStyleClass().add("btn-primaire");
        return b;
    }

    public static Button or(String texte) {
        Button b = new Button(texte);
        b.getStyleClass().add("btn-or");
        return b;
    }

    public static Button contour(String texte) {
        Button b = new Button(texte);
        b.getStyleClass().add("btn-contour");
        return b;
    }

    public static Button contourClair(String texte) {
        Button b = new Button(texte);
        b.getStyleClass().add("btn-contour-clair");
        return b;
    }

    /** Carte blanche arrondie avec ombre. */
    public static VBox carte(Node... enfants) {
        VBox v = new VBox(14, enfants);
        v.getStyleClass().add("carte");
        return v;
    }

    /** Espace extensible (pousse les elements suivants vers la droite d'un HBox). */
    public static Region espaceur() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    /** Message d'erreur ou de succes (cache tant qu'il n'a pas de texte). */
    public static Label message() {
        Label l = new Label();
        l.setWrapText(true);
        l.setVisible(false);
        l.setManaged(false);
        return l;
    }

    public static void montrer(Label l, String texte, boolean erreur) {
        l.getStyleClass().removeAll("message-erreur", "message-succes");
        l.getStyleClass().add(erreur ? "message-erreur" : "message-succes");
        l.setText(texte);
        l.setVisible(true);
        l.setManaged(true);
    }

    public static void cacher(Label l) {
        l.setVisible(false);
        l.setManaged(false);
    }

    /** Logo : pastille doree + nom de l'application. */
    public static Node logo(boolean clair) {
        Circle rond = new Circle(16);
        rond.getStyleClass().add("logo-rond");
        Label lettre = new Label("N");
        lettre.getStyleClass().add("logo-lettre");
        StackPane pastille = new StackPane(rond, lettre);
        Label nom = new Label("NAT Tontines");
        nom.getStyleClass().add(clair ? "logo-texte-clair" : "logo-texte");
        HBox h = new HBox(10, pastille, nom);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    /** Champ avec son etiquette au-dessus. */
    public static VBox champ(String libelle, Node saisie) {
        VBox v = new VBox(5, etiquette(libelle), saisie);
        return v;
    }

    private static final String[] COULEURS_AVATAR = { "#16294a", "#c4881f", "#1f6f54", "#2c5c8a", "#b23a2e", "#1e345c" };

    /** Pastille ronde avec les initiales (couleur stable pour un meme nom). */
    public static Node avatar(String nomComplet, double taille) {
        String nom = nomComplet == null ? "" : nomComplet.trim();
        String[] mots = nom.isEmpty() ? new String[0] : nom.split("\\s+");
        String initiales = mots.length == 0 ? "?" : mots.length == 1 ? mots[0].substring(0, 1)
                : mots[0].substring(0, 1) + mots[mots.length - 1].substring(0, 1);
        Circle rond = new Circle(taille / 2);
        rond.setFill(Color.web(COULEURS_AVATAR[Math.abs(nom.hashCode()) % COULEURS_AVATAR.length]));
        Label l = new Label(initiales.toUpperCase());
        l.getStyleClass().add("avatar-lettre");
        l.setStyle("-fx-font-size: " + Math.max(10, Math.round(taille * 0.38)) + "px;");
        return new StackPane(rond, l);
    }

    /** Icone dans un cercle colore : variante "vert", "or" ou "teal". */
    public static Node iconeRonde(String icone, String variante) {
        Node ic = Icones.icone(icone, 22);
        if ("or".equals(variante)) {
            ((Group) ic).getChildren().stream().filter(n -> n instanceof javafx.scene.shape.SVGPath).forEach(n -> n.getStyleClass().add("icone-or"));
        } else if ("teal".equals(variante)) {
            ((Group) ic).getChildren().stream().filter(n -> n instanceof javafx.scene.shape.SVGPath).forEach(n -> n.getStyleClass().add("icone-teal"));
        }
        StackPane s = new StackPane(ic);
        s.getStyleClass().add("or".equals(variante) ? "icone-rond-or" : "teal".equals(variante) ? "icone-rond-teal" : "icone-rond");
        s.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return s;
    }

    /** Bandeau de bienvenue en degrade, avec des actions facultatives a droite. */
    public static HBox banniere(String titre, String texte, Node... actions) {
        Label t = new Label(titre);
        t.getStyleClass().add("banniere-titre");
        t.setWrapText(true);
        Label d = new Label(texte);
        d.getStyleClass().add("banniere-texte");
        d.setWrapText(true);
        VBox gauche = new VBox(6, t, d);
        HBox h = new HBox(16, gauche, espaceur());
        h.getChildren().addAll(actions);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add("banniere");
        return h;
    }

    /** Etat vide soigne : icone, titre, texte et actions (jamais un ecran blanc). */
    public static VBox etatVide(String icone, String titre, String texte, Node... actions) {
        Label t = new Label(titre);
        t.getStyleClass().add("carte-titre");
        t.setWrapText(true);
        Label d = sousTitre(texte);
        VBox v = new VBox(12, iconeRonde(icone, "vert"), t, d);
        if (actions.length > 0) {
            HBox a = new HBox(12, actions);
            v.getChildren().add(a);
        }
        v.getStyleClass().add("etat-vide");
        return v;
    }
}
