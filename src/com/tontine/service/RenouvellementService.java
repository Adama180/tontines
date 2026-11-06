package com.tontine.service;

import java.util.ArrayList;
import java.util.List;

import com.tontine.dao.AdhesionDao;
import com.tontine.dao.CycleDao;
import com.tontine.dao.RenouvellementDao;
import com.tontine.model.Adhesion;
import com.tontine.model.Renouvellement;
import com.tontine.model.StatutAdhesion;
import com.tontine.model.StatutRenouvellement;
import com.tontine.model.Tontine;

/**
 * Relance d'une tontine terminee : le gerant envoie une alerte a chaque membre ; chacun valide (il continue) ou
 * refuse (il est retire automatiquement de la tontine). Le nouveau cycle ne peut demarrer qu'une fois toutes les
 * reponses recues.
 */
public class RenouvellementService {

    private final AdhesionDao adhesionDao = new AdhesionDao();
    private final CycleDao cycleDao = new CycleDao();
    private final RenouvellementDao dao = new RenouvellementDao();
    private final AdhesionService adhesions = new AdhesionService();

    /** Le gerant lance l'alerte de renouvellement a tous les autres membres actifs (une seule fois par cycle clos). */
    public int lancerAlerte(int gerantId, int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exigerGerant(t, gerantId);
        Controles.exiger(adhesions.estTerminee(t),
                "L'alerte de renouvellement se lance quand la tontine est termin\u00e9e (" + Libelles.cycle(t) + " cl\u00f4tur\u00e9).");
        int numero = cycleDao.dernierNumero(tontineId);
        Controles.exiger(dao.findByTontineEtCycle(tontineId, numero).isEmpty(),
                "L'alerte a d\u00e9j\u00e0 \u00e9t\u00e9 envoy\u00e9e aux membres pour ce " + Libelles.cycle(t) + ".");
        int envoyees = 0;
        for (Adhesion a : adhesionDao.findByTontine(tontineId)) {
            if (a.getStatut() != StatutAdhesion.ACTIF || a.getUserId() == gerantId) {
                continue;
            }
            Renouvellement r = new Renouvellement();
            r.setTontineId(tontineId);
            r.setUserId(a.getUserId());
            r.setCycleNumero(numero);
            dao.insert(r);
            NotificationService.notifier(a.getUserId(), "La tontine " + t.getNom() + " est termin\u00e9e. Souhaitez-vous continuer "
                    + "au prochain " + Libelles.cycle(t) + " ? R\u00e9pondez dans l'onglet Renouvellement : si vous refusez, "
                    + "vous serez retir\u00e9(e) de la tontine. Sans r\u00e9ponse de votre part, vous serez consid\u00e9r\u00e9(e) comme ne souhaitant plus participer.", "RENOUVELLEMENT");
            envoyees++;
        }
        Controles.exiger(envoyees > 0, "Il n'y a aucun autre membre actif \u00e0 qui envoyer l'alerte.");
        Controles.audit(gerantId, "RENOUVELLEMENT_ALERTE", "Tontine " + t.getNom() + " - " + envoyees + " membre(s)");
        return envoyees;
    }

    /**
     * Le membre repond a l'alerte. Valider : il reste dans la tontine. Refuser : il est retire automatiquement
     * (impossible tant qu'il a un solde ou un pret en cours : le gerant doit d'abord regler sa situation).
     */
    public Renouvellement repondre(int userId, int tontineId, boolean valider) {
        Tontine t = Controles.tontine(tontineId);
        Adhesion a = Controles.adhesionActive(tontineId, userId);
        Controles.exiger(adhesions.estTerminee(t), "Aucune alerte de renouvellement n'est en cours pour cette tontine.");
        Renouvellement r = dao.findByTontineCycleEtUser(tontineId, cycleDao.dernierNumero(tontineId), userId);
        Controles.exiger(r != null, "Le g\u00e9rant ne vous a pas envoy\u00e9 d'alerte de renouvellement.");
        Controles.exiger(r.getStatut() == StatutRenouvellement.EN_ATTENTE, "Vous avez d\u00e9j\u00e0 r\u00e9pondu \u00e0 cette alerte.");
        String nom = Controles.utilisateur(userId).getNomComplet();
        if (valider) {
            dao.repondre(r.getId(), StatutRenouvellement.VALIDE);
            NotificationService.notifier(t.getGerantId(), nom + " continue la tontine " + t.getNom() + ".", "RENOUVELLEMENT");
        } else {
            adhesions.retirer(t, a);
            dao.repondre(r.getId(), StatutRenouvellement.REFUSE);
            NotificationService.notifier(userId, "Vous avez refus\u00e9 de continuer : vous ne faites plus partie de la tontine "
                    + t.getNom() + ".", "RENOUVELLEMENT");
            NotificationService.notifier(t.getGerantId(), nom + " ne continue pas : il/elle est retir\u00e9(e) de la tontine "
                    + t.getNom() + ".", "RENOUVELLEMENT");
        }
        Controles.audit(userId, valider ? "RENOUVELLEMENT_VALIDE" : "RENOUVELLEMENT_REFUSE", "Tontine " + t.getNom());
        return dao.findById(r.getId());
    }

    /** Alertes du dernier cycle clos (vue du gerant : qui a repondu quoi). */
    public List<Renouvellement> alertes(int gerantId, int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exigerGerant(t, gerantId);
        return dao.findByTontineEtCycle(tontineId, cycleDao.dernierNumero(tontineId));
    }

    /** L'alerte du membre pour le dernier cycle clos, ou null s'il n'en a pas recu. */
    public Renouvellement monAlerte(int userId, int tontineId) {
        Controles.tontine(tontineId);
        return dao.findByTontineCycleEtUser(tontineId, cycleDao.dernierNumero(tontineId), userId);
    }

    /** Le gerant retire tout de suite les membres qui n'ont pas repondu (ils sont consideres comme ne voulant plus participer). */
    public int retirerSansReponse(int gerantId, int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exigerGerant(t, gerantId);
        return retirerSansReponse(t, gerantId);
    }

    /**
     * Pas de reponse = refus : les membres qui n'ont pas repondu a l'alerte sont retires de la tontine.
     * Retourne le nombre de membres retires.
     */
    int retirerSansReponse(Tontine t, int gerantId) {
        int tontineId = t.getId();
        List<String> echecs = new ArrayList<>();
        int retires = 0;
        for (Renouvellement r : dao.findByTontineEtCycle(tontineId, cycleDao.dernierNumero(tontineId))) {
            if (r.getStatut() != StatutRenouvellement.EN_ATTENTE) {
                continue;
            }
            Adhesion a = adhesionDao.findByTontineEtUser(tontineId, r.getUserId());
            String nom = Controles.utilisateur(r.getUserId()).getNomComplet();
            try {
                adhesions.retirer(t, a);
            } catch (ServiceException e) {
                echecs.add(nom + " (" + e.getMessage() + ")");
                continue;
            }
            dao.repondre(r.getId(), StatutRenouvellement.REFUSE);
            NotificationService.notifier(r.getUserId(), "Sans r\u00e9ponse \u00e0 l'alerte de renouvellement, vous \u00eates consid\u00e9r\u00e9(e) "
                    + "comme ne souhaitant plus participer : vous avez \u00e9t\u00e9 retir\u00e9(e) de la tontine " + t.getNom() + ".", "RENOUVELLEMENT");
            retires++;
        }
        Controles.audit(gerantId, "RENOUVELLEMENT_SANS_REPONSE", "Tontine " + t.getNom() + " - " + retires + " retir\u00e9(s)");
        Controles.exiger(echecs.isEmpty(), "Certains membres n'ont pas pu \u00eatre retir\u00e9s : " + String.join(" ; ", echecs));
        return retires;
    }

    /**
     * Appele avant de relancer une tontine terminee : l'alerte doit avoir ete envoyee ; les membres qui n'ont pas
     * repondu sont consideres comme ne voulant plus participer et sont retires.
     */
    void preparerRelance(Tontine t, int gerantId) {
        List<Renouvellement> liste = dao.findByTontineEtCycle(t.getId(), cycleDao.dernierNumero(t.getId()));
        Controles.exiger(!liste.isEmpty(), "Avant de relancer la tontine, envoyez l'alerte de renouvellement aux membres "
                + "(onglet Renouvellement) : chacun doit valider ou refuser de continuer.");
        retirerSansReponse(t, gerantId);
    }
}
