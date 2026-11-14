package com.tontine.ui;

import java.time.LocalDate;

import com.tontine.model.TypeTontine;
import com.tontine.service.Format;
import com.tontine.service.Libelles;
import com.tontine.service.ResumePublic;
import com.tontine.service.StatsPubliquesService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/** Page d'accueil publique : la premiere impression. Aucune connexion necessaire, s'adapte a la largeur de la fenetre. */
public class AccueilView extends BorderPane {

    private final java.util.List<Region> sections = new java.util.ArrayList<>();
    private Region bandeRef;

    public AccueilView() {
        getStyleClass().add("page");
        setTop(new BarreNavigation(false));

        VBox contenu = new VBox();
        contenu.getChildren().addAll(hero(), bandeChiffres(), etapes(), types(), atouts(), pied());
        ScrollPane defilement = new ScrollPane(contenu);
        defilement.setFitToWidth(true);
        defilement.getStyleClass().add("defilement");
        setCenter(defilement);

        Adaptatif.surLargeur(this, 860, etroit -> {
            Adaptatif.classeCompact(this, etroit);
            Insets p = etroit ? new Insets(28, 16, 4, 16) : new Insets(44, 48, 8, 48);
            for (Region r : sections) {
                r.setPadding(p);
            }
            if (bandeRef != null) {
                VBox.setMargin(bandeRef, etroit ? new Insets(-40, 16, 0, 16) : new Insets(-46, 48, 0, 48));
            }
        });
    }

    // ------------------------------------------------------------------ bandeau principal

    private Node hero() {
        Label l1 = new Label("Vos tontines,");
        l1.getStyleClass().add("hero-titre");
        Label l2 = new Label("en toute transparence.");
        l2.getStyleClass().add("hero-titre-accent");
        VBox titre = new VBox(0, l1, l2);

        Label texte = new Label("Fini les cahiers perdus et les erreurs de calcul : suivez les cotisations, les pr\u00eats et les tours "
                + "de chaque tontine, avec un historique que chaque membre peut consulter.");
        texte.getStyleClass().add("hero-texte");
        texte.setWrapText(true);

        Button explorer = Composants.or("Explorer les tontines");
        explorer.setOnAction(e -> Nav.aller(PagePublique.explorer()));
        FlowPane actions = new FlowPane(12, 12, explorer);

        VBox gauche = new VBox(20, titre, texte, actions);
        gauche.setPrefWidth(520);
        gauche.setMaxWidth(560);

        FlowPane ligne = new FlowPane(56, 28, gauche, new Illustration());
        ligne.setAlignment(Pos.CENTER);
        ligne.setRowValignment(javafx.geometry.VPos.CENTER);
        ligne.getStyleClass().add("hero-contenu");
        ligne.setMinWidth(0);

        // decor : grands cercles translucides, decoupes aux bords du bandeau
        // le decor ne doit JAMAIS influencer la taille du bandeau (ses cercles suivent la taille du bandeau)
        Pane decor = new Pane() {
            @Override
            protected double computePrefHeight(double largeur) {
                return 0;
            }

            @Override
            protected double computePrefWidth(double hauteur) {
                return 0;
            }
        };
        decor.setMouseTransparent(true);
        Circle c1 = new Circle(210, Color.web("#e3a73b", 0.22));
        c1.centerXProperty().bind(decor.widthProperty().multiply(0.95));
        c1.setCenterY(40);
        Circle c2 = new Circle(170, Color.TRANSPARENT);
        c2.setStroke(Color.web("#ffffff", 0.14));
        c2.setCenterX(40);
        c2.centerYProperty().bind(decor.heightProperty().multiply(0.95));
        Circle c3 = new Circle(40, Color.web("#ffffff", 0.04));
        c3.centerXProperty().bind(decor.widthProperty().multiply(0.55));
        c3.centerYProperty().bind(decor.heightProperty().multiply(0.12));
        decor.getChildren().addAll(c1, c2, c3);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(decor.widthProperty());
        clip.heightProperty().bind(decor.heightProperty());
        decor.setClip(clip);

        StackPane hero = new StackPane(decor, ligne);
        hero.getStyleClass().add("hero");
        ligne.prefWrapLengthProperty().bind(javafx.beans.binding.Bindings.max(320, hero.widthProperty().subtract(96)));
        Effets.apparition(gauche, 0);
        return hero;
    }

    // ------------------------------------------------------------------ chiffres de la plateforme

    private Node bandeChiffres() {
        FlowPane flux = new FlowPane(44, 14);
        flux.setAlignment(Pos.CENTER);
        ResumePublic r = null;
        try {
            r = new StatsPubliquesService().resume();
        } catch (RuntimeException e) {
            // la base est peut-etre indisponible : on affiche l'invitation plutot qu'une erreur
        }
        if (r != null && r.getTontinesOuvertes() > 0) {
            flux.getChildren().addAll(
                    chiffre(String.valueOf(r.getTontinesOuvertes()), r.getTontinesOuvertes() > 1 ? "tontines ouvertes" : "tontine ouverte"),
                    chiffre(String.valueOf(r.getMembresActifs()), r.getMembresActifs() > 1 ? "membres actifs" : "membre actif"),
                    chiffre(Format.montant(r.getTotalVerse()), "d\u00e9j\u00e0 vers\u00e9s aux membres"));
        } else {
            Label l = new Label("Soyez parmi les premiers : cr\u00e9ez votre tontine en quelques minutes.");
            l.getStyleClass().add("carte-titre");
            l.setWrapText(true);
            flux.getChildren().add(l);
        }
        flux.setMinWidth(0);
        StackPane bande = new StackPane(flux);
        bande.setMinWidth(0);
        bandeRef = bande;
        bande.getStyleClass().add("bande-stats");
        VBox.setMargin(bande, new Insets(-46, 48, 0, 48));
        Effets.apparition(bande, 150);
        return bande;
    }

    private Node chiffre(String valeur, String libelle) {
        Label v = new Label(valeur);
        v.getStyleClass().add("bande-stats-valeur");
        Label l = new Label(libelle);
        l.getStyleClass().add("bande-stats-libelle");
        VBox b = new VBox(2, v, l);
        b.setAlignment(Pos.CENTER);
        return b;
    }

    // ------------------------------------------------------------------ comment ca marche

    private Node etapes() {
        FlowPane cartes = new FlowPane(18, 18,
                etape(1, "recherche", "Explorez", "Comparez les tontines et leurs r\u00e8gles, sans compte et sans engagement.", 0),
                etape(2, "personne", "Rejoignez", "Envoyez votre demande avec votre pi\u00e8ce d'identit\u00e9 : le g\u00e9rant la valide.", 90),
                etape(3, "portefeuille", "Cotisez", "Payez par Wave, Orange Money ou en esp\u00e8ces. Chaque paiement est enregistr\u00e9.", 180),
                etape(4, "paiement", "Recevez", "Touchez votre cagnotte \u00e0 votre tour ou en fin de cycle, historique \u00e0 l'appui.", 270));
        cartes.setAlignment(Pos.CENTER);
        return section("Comment \u00e7a marche ?", "De la d\u00e9couverte \u00e0 la cagnotte, en quatre \u00e9tapes simples.", cartes);
    }

    private Node etape(int numero, String icone, String titre, String texte, int retard) {
        Label n = new Label(String.valueOf(numero));
        n.getStyleClass().add("etape-num-texte");
        StackPane num = new StackPane(n);
        num.getStyleClass().add("etape-num");
        HBox haut = new HBox(12, num, Composants.iconeRonde(icone, "vert"));
        haut.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label(titre);
        t.getStyleClass().add("carte-titre");
        Label d = Composants.sousTitre(texte);
        VBox c = Composants.carte(haut, t, d);
        c.getStyleClass().add("carte-survol");
        c.setPrefWidth(250);
        c.setMinWidth(220);
        Effets.apparition(c, retard);
        Effets.survol(c);
        return c;
    }

    // ------------------------------------------------------------------ les trois types

    private Node types() {
        FlowPane cartes = new FlowPane(20, 20,
                typeCarte(TypeTontine.FONDS_COMMUN, "cycle", "vert", "type-fonds",
                        "\u00c9pargne cumul\u00e9e et pr\u00eats entre membres", "Int\u00e9r\u00eat simple, partag\u00e9 en fin de cycle", 0),
                typeCarte(TypeTontine.TOUR_DE_ROLE, "echange", "or", "type-tour",
                        "Montant fixe, identique pour tous", "Ordre des b\u00e9n\u00e9ficiaires, tour anticip\u00e9 possible", 100),
                typeCarte(TypeTontine.TIRAGE_AU_SORT, "de", "teal", "type-tirage",
                        "M\u00eame principe, b\u00e9n\u00e9ficiaire tir\u00e9 au sort", "R\u00e9sultat trac\u00e9, jamais modifi\u00e9 en silence", 200));
        cartes.setAlignment(Pos.CENTER);
        return section("Trois fa\u00e7ons de tontiner", "Choisissez le fonctionnement qui ressemble \u00e0 votre groupe.", cartes);
    }

    private Node typeCarte(TypeTontine type, String icone, String variante, String classe, String point1, String point2, int retard) {
        Label nom = new Label(Libelles.type(type));
        nom.getStyleClass().add("carte-titre");
        Label desc = Composants.sousTitre(Libelles.descriptionType(type));
        VBox c = Composants.carte(Composants.iconeRonde(icone, variante), nom, desc, ligneCoche(point1), ligneCoche(point2));
        c.getStyleClass().addAll("carte-survol", classe);
        c.setPrefWidth(330);
        c.setMinWidth(260);
        Effets.apparition(c, retard);
        Effets.survol(c);
        return c;
    }

    private Node ligneCoche(String texte) {
        Label l = new Label(texte);
        l.setWrapText(true);
        l.getStyleClass().add("sous-titre");
        HBox h = new HBox(8, Icones.icone("coche", 18), l);
        h.setAlignment(Pos.TOP_LEFT);
        return h;
    }

    // ------------------------------------------------------------------ atouts

    private Node atouts() {
        FlowPane cartes = new FlowPane(18, 18,
                atout("visible", "Transparence", "Chaque membre consulte l'historique complet de sa tontine : cotisations, pr\u00eats, versements.", "vert"),
                atout("bouclier", "S\u00e9curit\u00e9", "Pi\u00e8ce d'identit\u00e9 v\u00e9rifi\u00e9e, adh\u00e9sion valid\u00e9e par le g\u00e9rant, journal d'audit.", "or"),
                atout("paiement", "Paiement simple", "Wave, Orange Money ou esp\u00e8ces confirm\u00e9es par le g\u00e9rant.", "teal"),
                atout("cloche", "Toujours inform\u00e9", "Notifications \u00e0 chaque demande, cotisation, pr\u00eat ou versement.", "vert"));
        cartes.setAlignment(Pos.CENTER);
        return section("Pourquoi NAT Tontines ?", "Pens\u00e9 pour la confiance entre membres.", cartes);
    }

    private Node atout(String icone, String titre, String texte, String variante) {
        Label t = new Label(titre);
        t.getStyleClass().add("carte-titre");
        Label d = Composants.sousTitre(texte);
        VBox c = Composants.carte(Composants.iconeRonde(icone, variante), t, d);
        c.getStyleClass().add("carte-survol");
        c.setPrefWidth(250);
        c.setMinWidth(220);
        Effets.apparition(c, 0);
        Effets.survol(c);
        return c;
    }

    // ------------------------------------------------------------------ pied de page

    private Node pied() {
        Label l = new Label("NAT Tontines \u2014 gestion de tontines  \u00b7  " + LocalDate.now().getYear());
        HBox h = new HBox(l);
        h.setAlignment(Pos.CENTER);
        h.getStyleClass().add("pied-page");
        return h;
    }

    // ------------------------------------------------------------------ outils

    private Node section(String titre, String sousTitre, Node contenu) {
        Label t = new Label(titre);
        t.getStyleClass().add("section-titre");
        t.setWrapText(true);
        Label s = new Label(sousTitre);
        s.getStyleClass().add("section-sous");
        s.setWrapText(true);
        VBox v = new VBox(8, t, s, spacer(14), contenu);
        v.setPadding(new Insets(44, 48, 8, 48));
        sections.add(v);
        VBox.setVgrow(contenu, javafx.scene.layout.Priority.NEVER);
        return v;
    }

    private Node spacer(double h) {
        javafx.scene.layout.Region r = new javafx.scene.layout.Region();
        r.setPrefHeight(h);
        r.setMinHeight(h);
        return r;
    }
}
