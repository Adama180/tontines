package com.tontine.service;

import java.util.ArrayList;
import java.util.List;

import com.tontine.dao.AdhesionDao;
import com.tontine.dao.CycleDao;
import com.tontine.dao.DemandeAdhesionDao;
import com.tontine.dao.DemandeDepartDao;
import com.tontine.dao.PretDao;
import com.tontine.dao.TontineDao;
import com.tontine.dao.UtilisateurDao;
import com.tontine.model.Adhesion;
import com.tontine.model.DemandeAdhesion;
import com.tontine.model.DemandeDepart;
import com.tontine.model.StatutAdhesion;
import com.tontine.model.StatutDemande;
import com.tontine.model.StatutPret;
import com.tontine.model.StatutTontine;
import com.tontine.model.Tontine;
import com.tontine.model.Utilisateur;

/** Actions du gerant sur sa tontine : demandes d'adhesion, membres, parametres. */
public class AdhesionService {

    private final AdhesionDao adhesionDao = new AdhesionDao();
    private final DemandeAdhesionDao demandeDao = new DemandeAdhesionDao();
    private final DemandeDepartDao departDao = new DemandeDepartDao();
    private final TontineDao tontineDao = new TontineDao();
    private final CycleDao cycleDao = new CycleDao();
    private final PretDao pretDao = new PretDao();
    private final UtilisateurDao utilisateurDao = new UtilisateurDao();

    public List<DemandeAdhesion> listerDemandes(int gerantId, int tontineId, StatutDemande statut) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exigerGerant(t, gerantId);
        return demandeDao.findByTontineEtStatut(tontineId, statut);
    }

    /** Accepte ou refuse une demande. Retourne la nouvelle adhesion (null si refusee). */
    public Adhesion validerDemandeAdhesion(int gerantId, int demandeId, boolean accepter) {
        DemandeAdhesion d = demandeDao.findById(demandeId);
        Controles.exiger(d != null, "Demande introuvable.");
        Controles.exiger(d.getStatut() == StatutDemande.EN_ATTENTE, "Cette demande a d\u00e9j\u00e0 \u00e9t\u00e9 trait\u00e9e.");
        Tontine t = Controles.tontine(d.getTontineId());
        Controles.exigerGerant(t, gerantId);

        if (!accepter) {
            demandeDao.changerStatut(d.getId(), StatutDemande.REFUSEE);
            NotificationService.notifier(d.getUserId(),
                    "Votre demande pour la tontine " + t.getNom() + " a \u00e9t\u00e9 refus\u00e9e.", "ADHESION");
            Controles.audit(gerantId, "ADHESION_REFUSEE", "Demande " + d.getId() + " - tontine " + t.getNom());
            return null;
        }

        if (!t.isFondsCommun()) {
            Controles.exiger(cycleDao.findEnCours(t.getId()) == null,
                    "Dans une tontine \u00e0 tour de r\u00f4le ou \u00e0 tirage au sort, un membre ne peut \u00eatre accept\u00e9 "
                            + "qu'entre deux tours. Attendez la cl\u00f4ture du tour en cours.");
        }
        Controles.exiger(adhesionDao.compterActifs(t.getId()) < t.getNbMembreMax(),
                "La tontine est compl\u00e8te (" + t.getNbMembreMax() + " membres).");
        Utilisateur u = Controles.utilisateur(d.getUserId());

        int ordre = prochainOrdre(t.getId());
        Adhesion a = adhesionDao.findByTontineEtUser(t.getId(), u.getId());
        if (a != null) {
            Controles.exiger(a.getStatut() == StatutAdhesion.INACTIF, "Cette personne est d\u00e9j\u00e0 membre.");
            adhesionDao.changerStatut(a.getId(), StatutAdhesion.ACTIF);
            adhesionDao.definirOrdreTour(a.getId(), ordre);
            a = adhesionDao.findById(a.getId());
        } else {
            a = new Adhesion();
            a.setTontineId(t.getId());
            a.setUserId(u.getId());
            a.setOrdreTour(ordre);
            adhesionDao.insert(a);
        }
        demandeDao.changerStatut(d.getId(), StatutDemande.ACCEPTEE);
        NotificationService.notifier(u.getId(),
                "Bienvenue ! Vous \u00eates maintenant membre de la tontine " + t.getNom() + ".", "ADHESION");
        Controles.audit(gerantId, "ADHESION_ACCEPTEE", u.getNomComplet() + " - tontine " + t.getNom());
        return a;
    }

    /** Membres de la tontine (visibles par les membres, le gerant et l'administrateur). */
    public List<LigneMembre> listerMembres(int userId, int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exigerParticipant(t, userId);
        List<LigneMembre> lignes = new ArrayList<>();
        for (Adhesion a : adhesionDao.findByTontine(tontineId)) {
            lignes.add(new LigneMembre(a, utilisateurDao.findById(a.getUserId())));
        }
        return lignes;
    }

    /** Desactive un membre. Impossible s'il a un solde, un pret non rembourse, ou est le gerant. */
    public void desactiverMembre(int gerantId, int adhesionId) {
        Adhesion a = adhesionDao.findById(adhesionId);
        Controles.exiger(a != null, "Adh\u00e9sion introuvable.");
        Tontine t = Controles.tontine(a.getTontineId());
        Controles.exigerGerant(t, gerantId);
        Controles.exiger(a.getUserId() != gerantId, "Le g\u00e9rant ne peut pas se d\u00e9sactiver lui-m\u00eame.");
        retirer(t, a);
        NotificationService.notifier(a.getUserId(),
                "Vous avez \u00e9t\u00e9 retir\u00e9 de la tontine " + t.getNom() + ".", "ADHESION");
        Controles.audit(gerantId, "MEMBRE_DESACTIVE", "Adh\u00e9sion " + a.getId() + " - tontine " + t.getNom());
    }

    /** Controles communs puis desactivation d'une adhesion (retrait par le gerant ou depart accepte). */
    void retirer(Tontine t, Adhesion a) {
        Controles.exiger(a.getStatut() == StatutAdhesion.ACTIF, "Ce membre est d\u00e9j\u00e0 d\u00e9sactiv\u00e9.");
        Controles.exiger(Math.abs(adhesionDao.findById(a.getId()).getSolde()) < 0.005,
                "Ce membre a un solde dans la tontine : il doit \u00eatre r\u00e9gl\u00e9 avant la d\u00e9sactivation.");
        Controles.exiger(pretDao.findEnCours(t.getId(), a.getUserId()).isEmpty()
                && pretDao.findByTontineEtStatut(t.getId(), StatutPret.EN_ATTENTE).stream()
                        .noneMatch(p -> p.getUserId() == a.getUserId()),
                "Ce membre a un pr\u00eat en cours.");
        if (!t.isFondsCommun()) {
            Controles.exiger(cycleDao.findEnCours(t.getId()) == null,
                    "Un membre ne peut \u00eatre d\u00e9sactiv\u00e9 qu'entre deux tours.");
        }
        adhesionDao.changerStatut(a.getId(), StatutAdhesion.INACTIF);
    }

    // ---------------------------------------------------------------- depart d'un membre (tontine terminee)

    /**
     * Un membre demande a quitter la tontine : possible seulement quand elle est terminee (tous les membres ont recu
     * leur cagnotte) et avant qu'elle soit relancee. Le gerant ne peut pas quitter sa propre tontine.
     */
    public DemandeDepart demanderDepart(int userId, int tontineId, String motif) {
        Tontine t = Controles.tontine(tontineId);
        Adhesion a = Controles.adhesionActive(tontineId, userId);
        Controles.exiger(t.getGerantId() != userId, "Le g\u00e9rant ne peut pas quitter sa propre tontine.");
        Controles.exiger(estTerminee(t),
                "Vous pourrez demander \u00e0 quitter la tontine quand elle sera termin\u00e9e (tous les membres ont re\u00e7u leur cagnotte), "
                        + "avant qu'elle soit relanc\u00e9e.");
        for (DemandeDepart d : departDao.findByUser(userId)) {
            Controles.exiger(d.getTontineId() != tontineId || d.getStatut() != StatutDemande.EN_ATTENTE,
                    "Vous avez d\u00e9j\u00e0 une demande de d\u00e9part en attente.");
        }
        DemandeDepart d = new DemandeDepart();
        d.setTontineId(tontineId);
        d.setUserId(userId);
        d.setMotif(motif == null || motif.trim().isEmpty() ? "Aucun motif indiqu\u00e9" : motif.trim());
        departDao.insert(d);
        NotificationService.notifier(t.getGerantId(), "Demande de d\u00e9part de "
                + Controles.utilisateur(a.getUserId()).getNomComplet() + " (tontine " + t.getNom() + ").", "ADHESION");
        return d;
    }

    /** Vrai si la tontine est terminee et en attente de relance (aucun cycle en cours). */
    public boolean estTerminee(Tontine t) {
        Tontine frais = tontineDao.findById(t.getId());
        return frais != null && frais.getStatut() == StatutTontine.CLOTUREE && cycleDao.findEnCours(t.getId()) == null;
    }

    public List<DemandeDepart> demandesDepart(int gerantId, int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exigerGerant(t, gerantId);
        return departDao.findByTontineEtStatut(tontineId, StatutDemande.EN_ATTENTE);
    }

    public List<DemandeDepart> mesDemandesDepart(int userId, int tontineId) {
        List<DemandeDepart> liste = new ArrayList<>();
        for (DemandeDepart d : departDao.findByUser(userId)) {
            if (d.getTontineId() == tontineId) {
                liste.add(d);
            }
        }
        return liste;
    }

    /** Le gerant accepte (le membre est retire de la tontine) ou refuse une demande de depart. */
    public void traiterDemandeDepart(int gerantId, int demandeId, boolean accepter) {
        DemandeDepart d = departDao.findById(demandeId);
        Controles.exiger(d != null && d.getStatut() == StatutDemande.EN_ATTENTE, "Demande introuvable ou d\u00e9j\u00e0 trait\u00e9e.");
        Tontine t = Controles.tontine(d.getTontineId());
        Controles.exigerGerant(t, gerantId);
        if (accepter) {
            Controles.exiger(estTerminee(t), "La tontine a d\u00e9j\u00e0 \u00e9t\u00e9 relanc\u00e9e : le d\u00e9part n'est plus possible.");
            Adhesion a = adhesionDao.findByTontineEtUser(t.getId(), d.getUserId());
            Controles.exiger(a != null, "Ce membre n'est plus dans la tontine.");
            retirer(t, a);
        }
        departDao.changerStatut(d.getId(), accepter ? StatutDemande.ACCEPTEE : StatutDemande.REFUSEE);
        NotificationService.notifier(d.getUserId(), accepter
                ? "Votre d\u00e9part de la tontine " + t.getNom() + " a \u00e9t\u00e9 accept\u00e9 : vous n'en faites plus partie."
                : "Votre demande de d\u00e9part de la tontine " + t.getNom() + " a \u00e9t\u00e9 refus\u00e9e.", "ADHESION");
        Controles.audit(gerantId, accepter ? "DEPART_ACCEPTE" : "DEPART_REFUSE", "Demande " + d.getId() + " - tontine " + t.getNom());
    }

    /** Appele a la relance de la tontine : les demandes de depart encore en attente ne sont plus possibles. */
    void refuserDepartsEnAttente(Tontine t) {
        for (DemandeDepart d : departDao.findByTontineEtStatut(t.getId(), StatutDemande.EN_ATTENTE)) {
            departDao.changerStatut(d.getId(), StatutDemande.REFUSEE);
            NotificationService.notifier(d.getUserId(), "La tontine " + t.getNom()
                    + " a \u00e9t\u00e9 relanc\u00e9e avant le traitement de votre demande de d\u00e9part : elle est refus\u00e9e.", "ADHESION");
        }
    }

    /** Definit les regles de la tontine (entre deux cycles uniquement). */
    public Tontine definirParametres(int gerantId, int tontineId, ParametresTontine p) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exigerGerant(t, gerantId);
        Controles.exiger(cycleDao.findEnCours(tontineId) == null,
                "Les param\u00e8tres ne peuvent \u00eatre modifi\u00e9s qu'entre deux " + Libelles.cycle(t) + "s.");
        p.controler(t.getType(), adhesionDao.compterActifs(tontineId));
        if (t.isFondsCommun()) {
            t.setTauxInteret(p.getTauxInteret());
            t.setDureeMaxPret(p.getDureeMaxPret());
        } else {
            t.setTauxInteret(null);
            t.setDureeMaxPret(null);
        }
        t.setDescription(p.getDescription());
        t.setMontantCotisation(p.getMontantCotisation());
        t.setMontantMaxCotisation(t.isFondsCommun() ? p.getMontantMaxCotisation() : null);
        t.setFrequence(p.getFrequence());
        t.setDureeEnCycle(p.getDureeEnCycle());
        t.setNbMembreMax(p.getNbMembreMax());
        t.setPenaliteRetard(p.getPenaliteRetard());
        t.setJourCotisation(p.getJourCotisation());
        t.setHeureCotisation(p.getHeureCotisation());
        tontineDao.update(t);
        Controles.audit(gerantId, "PARAMETRES_MODIFIES", "Tontine " + t.getNom());
        return t;
    }

    private int prochainOrdre(int tontineId) {
        int max = 0;
        for (Adhesion a : adhesionDao.findByTontine(tontineId)) {
            if (a.getOrdreTour() != null && a.getOrdreTour() > max) {
                max = a.getOrdreTour();
            }
        }
        return max + 1;
    }
}
