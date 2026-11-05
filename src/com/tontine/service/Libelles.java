package com.tontine.service;

import com.tontine.model.Frequence;
import com.tontine.model.ModePaiement;
import com.tontine.model.RoleAdhesion;
import com.tontine.model.StatutDemande;
import com.tontine.model.TypeTontine;

/** Textes lisibles pour l'interface (listes deroulantes, tableaux...). */
public final class Libelles {

    private Libelles() {
    }

    public static String type(TypeTontine t) {
        if (t == null) {
            return "";
        }
        switch (t) {
        case FONDS_COMMUN:
            return "Fonds commun";
        case TOUR_DE_ROLE:
            return "Tour de r\u00f4le";
        default:
            return "Tirage au sort";
        }
    }

    public static String descriptionType(TypeTontine t) {
        switch (t) {
        case FONDS_COMMUN:
            return "Chaque membre cotise une fois par p\u00e9riode (semaine, mois ou ann\u00e9e) pendant tout le cycle, "
                    + "d\u00e9marr\u00e9 une seule fois. Les membres peuvent emprunter sur ce fonds avec un int\u00e9r\u00eat ; "
                    + "toute cotisation en retard co\u00fbte une p\u00e9nalit\u00e9. \u00c0 la fin du cycle, chacun re\u00e7oit sa "
                    + "cagnotte plus une part \u00e9gale des p\u00e9nalit\u00e9s et des int\u00e9r\u00eats.";
        case TOUR_DE_ROLE:
            return "Montant fixe identique pour tous. \u00c0 chaque p\u00e9riode, un membre re\u00e7oit le total "
                    + "collect\u00e9, selon un ordre qui peut \u00eatre anticip\u00e9 sur demande au g\u00e9rant.";
        default:
            return "M\u00eame principe que le tour de r\u00f4le, mais le b\u00e9n\u00e9ficiaire de chaque p\u00e9riode est "
                    + "d\u00e9sign\u00e9 par tirage au sort parmi les membres pas encore tir\u00e9s.";
        }
    }

    public static String frequence(Frequence f) {
        if (f == null) {
            return "";
        }
        switch (f) {
        case JOURNALIERE:
            return "Journali\u00e8re";
        case HEBDOMADAIRE:
            return "Hebdomadaire";
        case MENSUELLE:
            return "Mensuelle";
        default:
            return "Annuelle";
        }
    }

    /** Cotisation affichee : intervalle pour un fonds commun, montant fixe sinon. */
    public static String cotisation(com.tontine.model.Tontine t) {
        if (t.isFondsCommun()) {
            return t.getMontantMaxCotisation() == null ? "\u00e0 d\u00e9finir par le g\u00e9rant"
                    : Format.montant(t.getMontantCotisation()) + " \u00e0 " + Format.montant(t.getMontantMaxCotisation());
        }
        return Format.montant(t.getMontantCotisation());
    }

    /** Nom de la periode de cotisation : semaine, mois ou annee. */
    public static String periode(Frequence f) {
        if (f == null) {
            return "";
        }
        switch (f) {
        case JOURNALIERE:
            return "jour";
        case HEBDOMADAIRE:
            return "semaine";
        case MENSUELLE:
            return "mois";
        default:
            return "ann\u00e9e";
        }
    }

    public static String mode(ModePaiement m) {
        if (m == null) {
            return "";
        }
        switch (m) {
        case ESPECE:
            return "Esp\u00e8ces";
        case WAVE:
            return "Wave";
        default:
            return "Orange Money";
        }
    }

    public static String role(RoleAdhesion r) {
        return r == RoleAdhesion.GERANT ? "G\u00e9rant" : "Membre";
    }

    public static String statutDemande(StatutDemande s) {
        if (s == null) {
            return "";
        }
        switch (s) {
        case EN_ATTENTE:
            return "En attente";
        case ACCEPTEE:
            return "Accept\u00e9e";
        default:
            return "Refus\u00e9e";
        }
    }

    private static final String[] JOURS = { "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche" };
    private static final String[] MOIS = { "janvier", "f\u00e9vrier", "mars", "avril", "mai", "juin", "juillet",
            "ao\u00fbt", "septembre", "octobre", "novembre", "d\u00e9cembre" };

    public static String jourSemaine(int j) {
        return JOURS[Math.max(1, Math.min(7, j)) - 1];
    }

    public static String mois(int m) {
        return MOIS[Math.max(1, Math.min(12, m)) - 1];
    }

    /** Date et heure de cotisation en clair, ex. "chaque lundi avant 18:00". */
    public static String horaire(com.tontine.model.Frequence f, Integer jour, Integer mois, java.time.LocalTime heure) {
        if (f == null) {
            return "";
        }
        String h = Format.heure(heure == null ? java.time.LocalTime.of(23, 59) : heure);
        switch (f) {
        case JOURNALIERE:
            return "chaque jour avant " + h;
        case HEBDOMADAIRE:
            return jour == null ? "en fin de semaine, avant " + h : "chaque " + jourSemaine(jour) + " avant " + h;
        case MENSUELLE:
            return jour == null ? "en fin de mois, avant " + h
                    : "le " + (jour == 1 ? "1er" : String.valueOf(jour)) + " de chaque mois avant " + h;
        default:
            return jour == null || mois == null ? "en fin d'ann\u00e9e, avant " + h
                    : "le " + (jour == 1 ? "1er" : String.valueOf(jour)) + " " + mois(mois) + " de chaque ann\u00e9e avant " + h;
        }
    }

    public static String horaire(com.tontine.model.Tontine t) {
        return horaire(t.getFrequence(), t.getJourCotisation(), t.getMoisCotisation(), t.getHeureCotisation());
    }

    public static String horaire(com.tontine.model.DemandeCreation d) {
        return horaire(d.getFrequence(), d.getJourCotisation(), d.getMoisCotisation(), d.getHeureCotisation());
    }

    /** Resume de toutes les regles d'une demande de creation, pour l'administrateur et le demandeur. */
    public static String reglesDemande(com.tontine.model.DemandeCreation d) {
        boolean fc = d.getTypeSouhaite() == com.tontine.model.TypeTontine.FONDS_COMMUN;
        StringBuilder sb = new StringBuilder();
        if (fc) {
            sb.append("Cotisation entre ").append(Format.montant(d.getMontant())).append(" et ")
                    .append(Format.montant(d.getMontantMax() == null ? d.getMontant() : d.getMontantMax()));
        } else {
            sb.append("Cotisation de ").append(Format.montant(d.getMontant()));
        }
        sb.append(" (").append(frequence(d.getFrequence()).toLowerCase()).append(", ")
                .append(horaire(d)).append(") - ").append(d.getNbMembre()).append(" membres max.");
        if (fc) {
            sb.append(" Cycle de ").append(d.getDureeEnCycle()).append(" p\u00e9riode(s).");
        }
        sb.append(" P\u00e9nalit\u00e9 : ").append(Format.montant(d.getPenaliteRetard())).append(".");
        if (fc && d.getTauxInteret() != null) {
            sb.append(" Pr\u00eats \u00e0 ").append(d.getTauxInteret()).append(" % sur ").append(d.getDureeMaxPret())
                    .append(" jour(s) maximum.");
        }
        return sb.toString();
    }

    /** Fonds commun : "cycle" ; tour de role et tirage au sort : "tour" (on demarre et on cloture un tour). */
    public static String cycle(com.tontine.model.Tontine t) {
        return t != null && !t.isFondsCommun() ? "tour" : "cycle";
    }

    public static String cycleMaj(com.tontine.model.Tontine t) {
        return t != null && !t.isFondsCommun() ? "Tour" : "Cycle";
    }
}
