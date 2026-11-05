package com.tontine.service;

import java.util.Locale;

/** Mise en forme des montants pour l'affichage. */
public final class Format {

    public static final String DEVISE = "FCFA";

    private Format() {
    }

    /** Exemple : 5266.67 -> "5 266,67 FCFA" ; 5000 -> "5 000 FCFA". */
    public static String montant(double valeur) {
        boolean entier = Math.abs(valeur - Math.rint(valeur)) < 0.005;
        String s = String.format(Locale.FRANCE, entier ? "%,.0f" : "%,.2f", valeur);
        return s.replace('\u202f', ' ').replace('\u00a0', ' ') + " " + DEVISE;
    }

    /** Exemple : 2026-10-21 -> "21/10/2026". */
    public static String date(java.time.LocalDate d) {
        return d == null ? "" : String.format("%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }

    /** Exemple : "21/10/2026 \u00e0 18:00". */
    public static String dateHeure(java.time.LocalDateTime d) {
        return d == null ? "" : date(d.toLocalDate()) + " \u00e0 " + heure(d.toLocalTime());
    }

    /** Exemple : "18:00". */
    public static String heure(java.time.LocalTime h) {
        return h == null ? "" : String.format("%02d:%02d", h.getHour(), h.getMinute());
    }
}
