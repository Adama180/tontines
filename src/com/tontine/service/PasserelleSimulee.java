package com.tontine.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.tontine.model.ModePaiement;

/**
 * Simulation de Wave et Orange Money (aucun appel reel : pas de contrat marchand).
 * Le jour ou une vraie API sera disponible, seule cette classe est a remplacer.
 */
public final class PasserelleSimulee {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasserelleSimulee() {
    }

    /** Simule le paiement mobile et retourne la reference de transaction. */
    public static String initierPaiement(ModePaiement mode, double montant, String numero) {
        Controles.exiger(mode == ModePaiement.WAVE || mode == ModePaiement.ORANGE_MONEY,
                "Choisissez un mode de paiement mobile.");
        String n = numero == null ? "" : numero.replaceAll("[\\s.-]", "");
        Controles.exiger(n.matches("^\\+?[0-9]{8,15}$"),
                "Num\u00e9ro " + Libelles.mode(mode) + " invalide (8 \u00e0 15 chiffres).");
        Controles.exiger(montant > 0, "Le montant doit \u00eatre positif.");
        String prefixe = mode == ModePaiement.WAVE ? "WAVE" : "OM";
        return prefixe + "-" + DateTimeFormatter.ofPattern("yyMMddHHmmss").format(LocalDateTime.now())
                + "-" + (1000 + RANDOM.nextInt(9000));
    }
}
