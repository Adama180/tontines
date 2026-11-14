package com.tontine.service;

import java.util.HashMap;
import java.util.Map;

import com.tontine.dao.JournalAuditDao;
import com.tontine.dao.UtilisateurDao;
import com.tontine.model.StatutCompte;
import com.tontine.model.Utilisateur;
import com.tontine.util.DaoException;
import com.tontine.util.PasswordUtil;

/** Inscription, connexion et gestion du profil. */
public class AuthService {

    private final UtilisateurDao dao = new UtilisateurDao();
    private final JournalAuditDao audit = new JournalAuditDao();

    /** Mot de passe oublie : 5 essais rates par email, puis blocage de 10 minutes. */
    static final int ESSAIS_MAX = 5;
    static final long BLOCAGE_MS = 10L * 60 * 1000;
    private static final Map<String, long[]> ESSAIS = new HashMap<>(); // email -> {nb echecs, debut de la serie}

    /** Cree un compte Utilisateur (sans tontine pour le moment). */
    public Utilisateur inscrire(String nom, String prenom, String email, String telephone, String motDePasse) {
        nom = Controles.texte(nom, "nom");
        prenom = Controles.texte(prenom, "pr\u00e9nom");
        email = Controles.email(email);
        telephone = Controles.telephone(telephone);
        Controles.motDePasse(motDePasse);
        Controles.exiger(!dao.emailExiste(email), "Cette adresse email est d\u00e9j\u00e0 utilis\u00e9e.");
        Controles.exiger(!dao.telephoneExiste(telephone), "Ce num\u00e9ro de t\u00e9l\u00e9phone est d\u00e9j\u00e0 utilis\u00e9.");

        Utilisateur u = new Utilisateur();
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setEmail(email);
        u.setTelephone(telephone);
        u.setMotDePasse(PasswordUtil.hasher(motDePasse));
        try {
            dao.insert(u);
        } catch (DaoException e) {
            if (e.estDoublon()) {
                throw new ServiceException("Email ou t\u00e9l\u00e9phone d\u00e9j\u00e0 utilis\u00e9.");
            }
            throw e;
        }
        return u;
    }

    /** Retrouve le compte par email ou telephone et verifie le mot de passe (sans tenir compte de la suspension). */
    Utilisateur verifierIdentifiants(String login, String motDePasse) {
        Controles.exiger(login != null && !login.trim().isEmpty() && motDePasse != null && !motDePasse.isEmpty(),
                "Saisissez votre identifiant et votre mot de passe.");
        Utilisateur u = dao.findByLogin(login.trim().toLowerCase());
        if (u == null) {
            u = dao.findByLogin(login.trim());
        }
        if (u == null) {
            try { // numero saisi avec l'indicatif (+221 ...) ou des espaces
                u = dao.findByLogin(Controles.telephone(login));
            } catch (ServiceException e) {
                u = null;
            }
        }
        Controles.exiger(u != null && PasswordUtil.verifier(motDePasse, u.getMotDePasse()),
                "Identifiant ou mot de passe incorrect.");
        return u;
    }

    /**
     * Connexion par email ou telephone. Memorise l'utilisateur dans la Session. Compte suspendu (identifiants corrects) :
     * CompteSuspenduException, qui declenche le bouton de demande de reactivation.
     */
    public Utilisateur seConnecter(String login, String motDePasse) {
        Utilisateur u = verifierIdentifiants(login, motDePasse);
        if (u.getStatut() != StatutCompte.ACTIF) {
            throw new CompteSuspenduException();
        }
        Session.setUtilisateur(u);
        return u;
    }

    public void deconnecter() {
        Session.deconnecter();
    }

    public void modifierProfil(int userId, String email, String telephone) {
        Utilisateur u = Controles.utilisateur(userId);
        email = Controles.email(email);
        telephone = Controles.telephone(telephone);
        Utilisateur autre = dao.findByEmail(email);
        Controles.exiger(autre == null || autre.getId() == userId, "Cette adresse email est d\u00e9j\u00e0 utilis\u00e9e.");
        autre = dao.findByTelephone(telephone);
        Controles.exiger(autre == null || autre.getId() == userId, "Ce num\u00e9ro de t\u00e9l\u00e9phone est d\u00e9j\u00e0 utilis\u00e9.");
        dao.changerCoordonnees(userId, email, telephone);
        u.setEmail(email);
        u.setTelephone(telephone);
        if (Session.getUtilisateur() != null && Session.getUtilisateur().getId() == userId) {
            Session.setUtilisateur(u);
        }
    }

    public void changerMotDePasse(int userId, String ancien, String nouveau) {
        Utilisateur u = Controles.utilisateur(userId);
        Controles.exiger(PasswordUtil.verifier(ancien == null ? "" : ancien, u.getMotDePasse()),
                "L'ancien mot de passe est incorrect.");
        Controles.motDePasse(nouveau);
        dao.changerMotDePasse(userId, PasswordUtil.hasher(nouveau));
    }

    /**
     * Mot de passe oublie. L'utilisateur s'authentifie en prouvant qu'il connait les informations de son compte
     * (email, telephone et nom saisis a l'inscription), puis choisit un nouveau mot de passe. Les messages d'erreur
     * ne disent pas quelle information est fausse, et les essais sont limites.
     */
    public void reinitialiserMotDePasse(String email, String telephone, String nom, String nouveau, String confirmation) {
        String cle = email == null ? "" : email.trim().toLowerCase();
        verifierNonBloque(cle);
        Controles.exiger(nouveau != null && nouveau.equals(confirmation), "Les deux mots de passe ne sont pas identiques.");
        Controles.motDePasse(nouveau);
        Utilisateur u = null;
        try {
            String tel = Controles.telephone(telephone);
            Utilisateur candidat = dao.findByEmail(Controles.email(email));
            if (candidat != null && tel.equals(candidat.getTelephone())
                    && nom != null && nom.trim().equalsIgnoreCase(candidat.getNom())) {
                u = candidat;
            }
        } catch (ServiceException e) {
            u = null; // format invalide : meme reponse generique
        }
        if (u == null) {
            enregistrerEchec(cle);
            throw new ServiceException("Ces informations ne correspondent \u00e0 aucun compte. V\u00e9rifiez l'email, le t\u00e9l\u00e9phone et le nom saisis \u00e0 l'inscription.");
        }
        Controles.exiger(u.getStatut() == StatutCompte.ACTIF, "Votre compte est suspendu. Contactez l'administrateur.");
        dao.changerMotDePasse(u.getId(), PasswordUtil.hasher(nouveau));
        synchronized (ESSAIS) {
            ESSAIS.remove(cle);
        }
        audit.log(u.getId(), "MOT_DE_PASSE_REINITIALISE", "R\u00e9initialisation par la proc\u00e9dure \u00ab mot de passe oubli\u00e9 \u00bb");
        NotificationService.notifier(u.getId(), "Votre mot de passe a \u00e9t\u00e9 r\u00e9initialis\u00e9. Si ce n'est pas vous, contactez l'administrateur.", "COMPTE");
    }

    private static void verifierNonBloque(String cle) {
        synchronized (ESSAIS) {
            long[] e = ESSAIS.get(cle);
            if (e != null && e[0] >= ESSAIS_MAX) {
                long reste = e[1] + BLOCAGE_MS - System.currentTimeMillis();
                if (reste > 0) {
                    throw new ServiceException("Trop de tentatives. R\u00e9essayez dans " + (reste / 60000 + 1) + " minute(s).");
                }
                ESSAIS.remove(cle);
            }
        }
    }

    private static void enregistrerEchec(String cle) {
        synchronized (ESSAIS) {
            long[] e = ESSAIS.get(cle);
            if (e == null || System.currentTimeMillis() - e[1] > BLOCAGE_MS) {
                e = new long[] { 0, System.currentTimeMillis() };
                ESSAIS.put(cle, e);
            }
            e[0]++;
        }
    }
}
