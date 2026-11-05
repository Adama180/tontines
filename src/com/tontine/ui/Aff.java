package com.tontine.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.tontine.model.StatutDemande;
import com.tontine.service.Libelles;

import javafx.scene.control.Label;

/** Mise en forme pour l'affichage (dates, pastilles de statut). */
public final class Aff {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Aff() {
    }

    public static String date(LocalDateTime d) {
        return d == null ? "" : DATE.format(d);
    }

    /** Pastille de couleur : en attente (or), acceptee (vert), refusee (rouge). */
    public static Label badgeStatut(StatutDemande s) {
        Label l = new Label(Libelles.statutDemande(s));
        l.getStyleClass().add("badge-statut");
        l.getStyleClass().add(s == StatutDemande.EN_ATTENTE ? "statut-attente"
                : s == StatutDemande.ACCEPTEE ? "statut-acceptee" : "statut-refusee");
        return l;
    }

    public static String typeTransaction(com.tontine.model.TypeTransaction t) {
        if (t == null) {
            return "";
        }
        switch (t) {
        case COTISATION:
            return "Cotisation";
        case PRET:
            return "Pr\u00eat accord\u00e9";
        case REMBOURSEMENT:
            return "Remboursement";
        case DISTRIBUTION:
            return "Versement";
        default:
            return "P\u00e9nalit\u00e9";
        }
    }

    public static String statutTransaction(com.tontine.model.StatutTransaction s) {
        if (s == null) {
            return "";
        }
        switch (s) {
        case CONFIRMEE:
            return "Confirm\u00e9e";
        case EN_ATTENTE:
            return "En attente";
        default:
            return "\u00c9chou\u00e9e";
        }
    }

    /** Pastille de statut d'un pret : en attente, accorde, refuse, rembourse. */
    public static Label badgePret(com.tontine.model.StatutPret s) {
        String texte;
        String classe;
        switch (s) {
        case EN_ATTENTE:
            texte = "En attente";
            classe = "statut-attente";
            break;
        case ACCORDE:
            texte = "Accord\u00e9";
            classe = "statut-acceptee";
            break;
        case REFUSE:
            texte = "Refus\u00e9";
            classe = "statut-refusee";
            break;
        default:
            texte = "Rembours\u00e9";
            classe = "statut-rembourse";
            break;
        }
        Label l = new Label(texte);
        l.getStyleClass().addAll("badge-statut", classe);
        return l;
    }

    /** 5000.0 -> "5000" ; 266.67 -> "266,67". */
    public static String nombre(double v) {
        return Math.abs(v - Math.rint(v)) < 0.005 ? String.valueOf((long) Math.rint(v))
                : String.format(java.util.Locale.FRANCE, "%.2f", v);
    }

    /** Texte lisible d'une action du journal d'audit. */
    public static String actionAudit(String code) {
        if (code == null) {
            return "";
        }
        switch (code) {
        case "ADHESION_ACCEPTEE":
            return "Adh\u00e9sion accept\u00e9e";
        case "ADHESION_REFUSEE":
            return "Adh\u00e9sion refus\u00e9e";
        case "MEMBRE_DESACTIVE":
            return "Membre retir\u00e9";
        case "PARAMETRES_MODIFIES":
            return "Param\u00e8tres modifi\u00e9s";
        case "CYCLE_DEMARRE":
            return "Cycle / tour d\u00e9marr\u00e9";
        case "CYCLE_CLOTURE":
            return "Cycle / tour cl\u00f4tur\u00e9";
        case "TOUR_ANTICIPE_ACCEPTE":
            return "Tour anticip\u00e9 accept\u00e9";
        case "TOUR_ANTICIPE_REFUSE":
            return "Tour anticip\u00e9 refus\u00e9";
        case "TIRAGE_AU_SORT":
            return "Tirage au sort";
        case "CORRECTION_TIRAGE":
            return "Correction d'un tirage";
        case "COMPTE_SUSPENDU":
            return "Compte suspendu";
        case "COMPTE_REACTIVE":
            return "Compte r\u00e9activ\u00e9";
        case "CREATION_ACCEPTEE":
            return "Cr\u00e9ation de tontine accept\u00e9e";
        case "CREATION_REFUSEE":
            return "Cr\u00e9ation de tontine refus\u00e9e";
        case "PRET_ACCORDE":
            return "Pr\u00eat accord\u00e9";
        case "PRET_REFUSE":
            return "Pr\u00eat refus\u00e9";
        case "PAIEMENT_GERANT_CONFIRME":
            return "Paiement du g\u00e9rant confirm\u00e9";
        case "PAIEMENT_GERANT_REFUSE":
            return "Paiement du g\u00e9rant refus\u00e9";
        default:
            String t = code.toLowerCase().replace('_', ' ');
            return Character.toUpperCase(t.charAt(0)) + t.substring(1);
        }
    }

    public static Label badge(String texte) {
        Label l = new Label(texte);
        l.getStyleClass().add("badge-type");
        return l;
    }
}
