package com.tontine.service;

import java.util.List;

import com.tontine.dao.AdhesionDao;
import com.tontine.dao.CycleDao;
import com.tontine.dao.DemandeAdhesionDao;
import com.tontine.dao.DemandeCreationDao;
import com.tontine.dao.DistributionDao;
import com.tontine.dao.PretDao;
import com.tontine.dao.TontineDao;
import com.tontine.dao.TransactionDao;
import com.tontine.dao.UtilisateurDao;
import com.tontine.model.Adhesion;
import com.tontine.model.Cycle;
import com.tontine.model.StatutCompte;
import com.tontine.model.StatutDemande;
import com.tontine.model.StatutPret;
import com.tontine.model.Tontine;
import com.tontine.model.TypeTontine;
import com.tontine.model.TypeTransaction;
import com.tontine.model.Utilisateur;

/** Tableaux de bord : activite d'un membre, d'une tontine, de toute la plateforme. */
public class StatistiquesService {

    private final AdhesionDao adhesionDao = new AdhesionDao();
    private final CycleDao cycleDao = new CycleDao();
    private final TransactionDao transactionDao = new TransactionDao();
    private final PretDao pretDao = new PretDao();
    private final DemandeAdhesionDao demandeAdhesionDao = new DemandeAdhesionDao();
    private final DemandeCreationDao demandeCreationDao = new DemandeCreationDao();
    private final TontineDao tontineDao = new TontineDao();
    private final UtilisateurDao utilisateurDao = new UtilisateurDao();
    private final DistributionDao distributionDao = new DistributionDao();

    /** "Voir votre activite" : situation personnelle d'un membre dans une tontine. */
    public Indicateurs activiteMembre(int userId, int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Adhesion a = Controles.adhesionActive(tontineId, userId);
        Indicateurs i = new Indicateurs();
        i.ajouter("Tontine", t.getNom());
        i.ajouter("R\u00f4le", Libelles.role(a.getRole()));
        i.ajouterMontant("Mon solde dans la tontine", a.getSolde());
        i.ajouterMontant("Total cotise", a.getTotalCotise());
        i.ajouter("Cotisations effectu\u00e9es", a.getCotisationsFaites());
        Cycle c = cycleDao.findEnCours(tontineId);
        i.ajouter(Libelles.cycleMaj(t) + " en cours", c == null ? "aucun" : Libelles.cycle(t) + " " + c.getNumero());
        if (t.isTourDeRole() && a.getOrdreTour() != null) {
            i.ajouter("Mon rang dans le tour", a.getOrdreTour());
        }
        return i;
    }

    /** Tableau de bord d'une tontine (membres, gerant, administrateur). */
    public Indicateurs tontine(int userId, int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exigerParticipant(t, userId);
        Indicateurs i = new Indicateurs();
        i.ajouter("Tontine", t.getNom() + " (" + Libelles.type(t.getType()) + ")");
        i.ajouter("Membres actifs", adhesionDao.compterActifs(tontineId) + " / " + t.getNbMembreMax());
        Cycle c = cycleDao.findEnCours(tontineId);
        i.ajouter(Libelles.cycleMaj(t) + " en cours", c == null ? "aucun" : Libelles.cycle(t) + " " + c.getNumero() + " (depuis le " + c.getDateDebut() + ")");
        i.ajouterMontant("Total des cotisations", transactionDao.totalParType(tontineId, TypeTransaction.COTISATION));
        i.ajouterMontant("Total des p\u00e9nalit\u00e9s", transactionDao.totalParType(tontineId, TypeTransaction.PENALITE));
        i.ajouterMontant("Total vers\u00e9 aux b\u00e9n\u00e9ficiaires", transactionDao.totalParType(tontineId, TypeTransaction.DISTRIBUTION));
        if (t.isFondsCommun()) {
            i.ajouterMontant("Fonds disponible", new PretService().fondsDisponible(tontineId));
            i.ajouter("Pr\u00eats en cours", pretDao.findByTontineEtStatut(tontineId, StatutPret.ACCORDE).size());
            i.ajouter("Pr\u00eats en attente", pretDao.findByTontineEtStatut(tontineId, StatutPret.EN_ATTENTE).size());
        }
        if (t.getGerantId() == userId) {
            i.ajouter("Demandes d'adh\u00e9sion en attente",
                    demandeAdhesionDao.findByTontineEtStatut(tontineId, StatutDemande.EN_ATTENTE).size());
        }
        return i;
    }

    /** "Suivre l'activite globale" (administrateur). */
    public Indicateurs globales(int adminId) {
        Controles.exigerAdmin(adminId);
        List<Utilisateur> users = utilisateurDao.findAll();
        long suspendus = users.stream().filter(u -> u.getStatut() == StatutCompte.SUSPENDU).count();
        List<Tontine> tontines = tontineDao.findAll();
        Indicateurs i = new Indicateurs();
        i.ajouter("Utilisateurs", users.size() + " (dont " + suspendus + " suspendus)");
        i.ajouter("Tontines", tontines.size());
        for (TypeTontine type : TypeTontine.values()) {
            long n = tontines.stream().filter(t -> t.getType() == type).count();
            i.ajouter("  - " + Libelles.type(type), n);
        }
        double cotisations = 0;
        for (Tontine t : tontines) {
            cotisations += transactionDao.totalParType(t.getId(), TypeTransaction.COTISATION);
        }
        i.ajouterMontant("Total des cotisations", cotisations);
        i.ajouter("Versements effectu\u00e9s", distributionDao.findAll().size());
        i.ajouter("Demandes de cr\u00e9ation en attente", demandeCreationDao.findByStatut(StatutDemande.EN_ATTENTE).size());
        i.ajouter("Pr\u00eats de g\u00e9rants \u00e0 valider", pretDao.findEnAttentePourGerants().size());
        return i;
    }
}
