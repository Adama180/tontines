package com.tontine.service;

import com.tontine.dao.AdhesionDao;
import com.tontine.dao.CycleDao;
import com.tontine.dao.JournalAuditDao;
import com.tontine.dao.TontineDao;
import com.tontine.dao.UtilisateurDao;
import com.tontine.model.Adhesion;
import com.tontine.model.Cycle;
import com.tontine.model.StatutAdhesion;
import com.tontine.model.StatutCompte;
import com.tontine.model.Tontine;
import com.tontine.model.Utilisateur;

/** Verifications communes a tous les services (droits, existence, formats). */
final class Controles {

    private static final UtilisateurDao UTILISATEURS = new UtilisateurDao();
    private static final TontineDao TONTINES = new TontineDao();
    private static final AdhesionDao ADHESIONS = new AdhesionDao();
    private static final CycleDao CYCLES = new CycleDao();

    private Controles() {
    }

    static void exiger(boolean condition, String message) {
        if (!condition) {
            throw new ServiceException(message);
        }
    }

    static String texte(String valeur, String champ) {
        exiger(valeur != null && !valeur.trim().isEmpty(), "Le champ '" + champ + "' est obligatoire.");
        return valeur.trim();
    }

    static String email(String valeur) {
        String v = texte(valeur, "email").toLowerCase();
        exiger(v.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$"), "Adresse email invalide.");
        return v;
    }

    /**
     * Numero de telephone du Senegal. Formats acceptes : 77 123 45 67, +221 77 123 45 67, 00221771234567, 221771234567.
     * Retourne la forme nationale a 9 chiffres (ex. 771234567). Operateurs : 70, 75, 76, 77, 78 (mobiles) et 30, 33 (fixes).
     */
    static String telephone(String valeur) {
        String v = texte(valeur, "t\u00e9l\u00e9phone").replaceAll("[\\s.\\-()]", "");
        exiger(v.matches("^\\+?[0-9]+$"), MSG_TEL);
        if (v.startsWith("+221")) {
            v = v.substring(4);
        } else if (v.startsWith("00221")) {
            v = v.substring(5);
        } else if (v.startsWith("221") && v.length() == 12) {
            v = v.substring(3);
        }
        exiger(v.matches("^(7[05678]|3[03])[0-9]{7}$"), MSG_TEL);
        return v;
    }

    private static final String MSG_TEL = "Le num\u00e9ro doit \u00eatre un num\u00e9ro du S\u00e9n\u00e9gal (indicatif +221) : "
            + "9 chiffres commen\u00e7ant par 70, 75, 76, 77, 78, 30 ou 33.";

    static void motDePasse(String valeur) {
        exiger(valeur != null && valeur.length() >= 6, "Le mot de passe doit contenir au moins 6 caract\u00e8res.");
    }

    static double arrondir(double valeur) {
        return Math.round(valeur * 100.0) / 100.0;
    }

    /** Charge un utilisateur existant dont le compte n'est pas suspendu. */
    static Utilisateur utilisateur(int id) {
        Utilisateur u = UTILISATEURS.findById(id);
        exiger(u != null, "Utilisateur introuvable.");
        exiger(u.getStatut() == StatutCompte.ACTIF, "Ce compte est suspendu.");
        return u;
    }

    static Tontine tontine(int id) {
        Tontine t = TONTINES.findById(id);
        exiger(t != null, "Tontine introuvable.");
        return t;
    }

    static Adhesion adhesionActive(int tontineId, int userId) {
        Adhesion a = ADHESIONS.findByTontineEtUser(tontineId, userId);
        exiger(a != null && a.getStatut() == StatutAdhesion.ACTIF, "Vous n'\u00eates pas membre de cette tontine.");
        return a;
    }

    static void exigerGerant(Tontine t, int userId) {
        exiger(t.getGerantId() == userId, "Action r\u00e9serv\u00e9e au g\u00e9rant de cette tontine.");
    }

    static void exigerAdmin(int userId) {
        Utilisateur u = utilisateur(userId);
        exiger(u.isAdmin(), "Action r\u00e9serv\u00e9e \u00e0 l'administrateur.");
    }

    /** Membre actif, gerant ou administrateur : peut consulter la tontine. */
    static void exigerParticipant(Tontine t, int userId) {
        Utilisateur u = utilisateur(userId);
        if (u.isAdmin()) {
            return;
        }
        adhesionActive(t.getId(), userId);
    }

    static Cycle cycleEnCours(Tontine t) {
        Cycle c = CYCLES.findEnCours(t.getId());
        exiger(c != null, "Aucun " + Libelles.cycle(t) + " en cours pour cette tontine. Le g\u00e9rant doit d'abord d\u00e9marrer un " + Libelles.cycle(t) + ".");
        return c;
    }

    static void audit(Integer userId, String action, String details) {
        try {
            new JournalAuditDao().log(userId, action, details);
        } catch (RuntimeException e) {
            // l'audit ne doit jamais bloquer l'operation principale
        }
    }
}
