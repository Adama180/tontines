package com.tontine.ui;

import java.io.InputStream;

import javafx.scene.text.Font;

/** Charge les polices Fraunces (titres) et Inter (texte) (licence SIL OFL, voir polices/OFL.txt). Sans elle, le systeme utilise sa police par defaut. */
public final class Polices {

    private static boolean chargees;

    private Polices() {
    }

    public static synchronized void charger() {
        if (chargees) {
            return;
        }
        chargees = true;
        for (String nom : new String[] { "Fraunces-Regular", "Fraunces-SemiBold", "Fraunces-Bold", "Fraunces-SemiBoldItalic",
                "Inter-Regular", "Inter-Medium", "Inter-SemiBold", "Inter-Bold" }) {
            try (InputStream in = Polices.class.getResourceAsStream("polices/" + nom + ".ttf")) {
                if (in == null || Font.loadFont(in, 14) == null) {
                    System.err.println("Police " + nom + " introuvable : la police par defaut du systeme sera utilisee.");
                }
            } catch (Exception e) {
                System.err.println("Police " + nom + " non chargee : " + e.getMessage());
            }
        }
    }
}
