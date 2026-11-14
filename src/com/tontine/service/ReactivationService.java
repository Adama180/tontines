package com.tontine.service;

import java.util.List;

import com.tontine.dao.DemandeReactivationDao;
import com.tontine.dao.UtilisateurDao;
import com.tontine.model.DemandeReactivation;
import com.tontine.model.StatutCompte;
import com.tontine.model.StatutDemande;
import com.tontine.model.Utilisateur;

/**
 * Demande de reactivation d'un compte suspendu : l'utilisateur suspendu (qui s'est identifie avec son email/telephone et son
 * mot de passe sur la page de connexion) envoie un motif ; tous les administrateurs sont prevenus par notification et
 * l'administrateur accepte (le compte est reactive) ou refuse.
 */
public class ReactivationService {

    public static final int MOTIF_MAX = 200;

    private final DemandeReactivationDao dao = new DemandeReactivationDao();
    private final UtilisateurDao utilisateurDao = new UtilisateurDao();

    /** Envoie la demande de reactivation : l'identite est re-verifiee (mot de passe), le compte doit etre suspendu. */
    public DemandeReactivation demanderReactivation(String login, String motDePasse, String motif) {
        Utilisateur u = new AuthService().verifierIdentifiants(login, motDePasse);
        Controles.exiger(u.getStatut() == StatutCompte.SUSPENDU, "Votre compte n'est pas suspendu : connectez-vous normalement.");
        String m = Controles.texte(motif, "motif de la demande");
        Controles.exiger(m.length() <= MOTIF_MAX, "Le motif ne doit pas d\u00e9passer " + MOTIF_MAX + " caract\u00e8res.");
        for (DemandeReactivation d : dao.findByUser(u.getId())) {
            Controles.exiger(d.getStatut() != StatutDemande.EN_ATTENTE,
                    "Votre demande de r\u00e9activation est d\u00e9j\u00e0 en attente : l'administrateur va la traiter.");
        }
        DemandeReactivation d = new DemandeReactivation();
        d.setUserId(u.getId());
        d.setMotif(m);
        dao.insert(d);
        String court = m.length() > 100 ? m.substring(0, 97) + "..." : m;
        for (Utilisateur a : utilisateurDao.findAll()) {
            if (a.isAdmin()) {
                NotificationService.notifier(a.getId(), "Demande de r\u00e9activation de compte : " + u.getNomComplet()
                        + ". Motif : " + court, "REACTIVATION");
            }
        }
        Controles.audit(u.getId(), "REACTIVATION_DEMANDEE", u.getNomComplet() + " (" + u.getEmail() + ")");
        return d;
    }

    public List<DemandeReactivation> demandesEnAttente(int adminId) {
        Controles.exigerAdmin(adminId);
        return dao.findByStatut(StatutDemande.EN_ATTENTE);
    }

    /** L'administrateur accepte (le compte redevient actif) ou refuse la demande. */
    public void traiter(int adminId, int demandeId, boolean accepter) {
        Controles.exigerAdmin(adminId);
        DemandeReactivation d = dao.findById(demandeId);
        Controles.exiger(d != null && d.getStatut() == StatutDemande.EN_ATTENTE, "Demande introuvable ou d\u00e9j\u00e0 trait\u00e9e.");
        Utilisateur u = utilisateurDao.findById(d.getUserId());
        Controles.exiger(u != null, "Utilisateur introuvable.");
        if (accepter) {
            utilisateurDao.changerStatut(u.getId(), StatutCompte.ACTIF);
            NotificationService.notifier(u.getId(), "Votre demande de r\u00e9activation a \u00e9t\u00e9 accept\u00e9e : votre compte est de nouveau actif.", "COMPTE");
        } else {
            NotificationService.notifier(u.getId(), "Votre demande de r\u00e9activation a \u00e9t\u00e9 refus\u00e9e. Contactez l'administrateur.", "COMPTE");
        }
        dao.traiter(d.getId(), accepter ? StatutDemande.ACCEPTEE : StatutDemande.REFUSEE, adminId);
        Controles.audit(adminId, accepter ? "REACTIVATION_ACCEPTEE" : "REACTIVATION_REFUSEE", u.getNomComplet() + " (" + u.getEmail() + ")");
    }

    /** Appele quand l'administrateur reactive lui-meme un compte : les demandes en attente de cet utilisateur sont closes. */
    void cloreDemandes(int userId, int adminId) {
        for (DemandeReactivation d : dao.findByUser(userId)) {
            if (d.getStatut() == StatutDemande.EN_ATTENTE) {
                dao.traiter(d.getId(), StatutDemande.ACCEPTEE, adminId);
            }
        }
    }
}
