package com.tontine.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tontine.dao.AdhesionDao;
import com.tontine.dao.CycleDao;
import com.tontine.dao.DemandeTourAnticipeDao;
import com.tontine.dao.TransactionDao;
import com.tontine.dao.UtilisateurDao;
import com.tontine.model.Adhesion;
import com.tontine.model.Cycle;
import com.tontine.model.DemandeTourAnticipe;
import com.tontine.model.Distribution;
import com.tontine.model.StatutTransaction;
import com.tontine.model.Tontine;
import com.tontine.model.Transaction;
import com.tontine.model.TypeTransaction;
import com.tontine.model.Utilisateur;

/** Lectures utiles a l'espace membre (noms des membres, cycle en cours, versements recus...). */
public class MembreService {

    private final AdhesionDao adhesionDao = new AdhesionDao();
    private final UtilisateurDao utilisateurDao = new UtilisateurDao();
    private final CycleDao cycleDao = new CycleDao();
    private final TransactionDao transactionDao = new TransactionDao();
    private final DemandeTourAnticipeDao tourAnticipeDao = new DemandeTourAnticipeDao();

    /** Identifiant -> nom complet des membres de la tontine (reserve aux participants). */
    public Map<Integer, String> nomsMembres(int userId, int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exigerParticipant(t, userId);
        Map<Integer, String> noms = new HashMap<>();
        for (Adhesion a : adhesionDao.findByTontine(tontineId)) {
            Utilisateur u = utilisateurDao.findById(a.getUserId());
            noms.put(a.getUserId(), u == null ? "?" : u.getNomComplet());
        }
        return noms;
    }

    /** Cycle en cours, ou null s'il n'y en a pas. */
    public Cycle cycleEnCours(int tontineId) {
        Tontine t = new com.tontine.dao.TontineDao().findById(tontineId);
        if (t != null && !t.isFondsCommun()) {
            new CycleService().assurerTourEnCours(tontineId);
        }
        return cycleDao.findEnCours(tontineId);
    }

    /** Identifiant de cycle -> numero du cycle. */
    public Map<Integer, Integer> numerosDeCycle(int tontineId) {
        Map<Integer, Integer> numeros = new HashMap<>();
        for (Cycle c : cycleDao.findByTontine(tontineId)) {
            numeros.put(c.getId(), c.getNumero());
        }
        return numeros;
    }

    /** Vrai si le membre a deja cotise (ou a un paiement en attente) dans le cycle en cours. */
    public boolean aDejaCotiseDansCycle(int userId, int tontineId) {
        Cycle c = cycleDao.findEnCours(tontineId);
        if (c == null) {
            return false;
        }
        for (Transaction tx : transactionDao.findByCycle(c.getId())) {
            if (tx.getUserId() == userId && tx.getType() == TypeTransaction.COTISATION
                    && tx.getStatut() != StatutTransaction.ECHOUEE) {
                return true;
            }
        }
        return false;
    }

    public List<DemandeTourAnticipe> mesDemandesTourAnticipe(int userId, int tontineId) {
        List<DemandeTourAnticipe> liste = new ArrayList<>();
        for (DemandeTourAnticipe d : tourAnticipeDao.findByUser(userId)) {
            if (d.getTontineId() == tontineId) {
                liste.add(d);
            }
        }
        return liste;
    }

    /** Versements (cagnottes) recus par le membre dans cette tontine. */
    public List<Distribution> mesVersements(int userId, int tontineId) {
        List<Distribution> liste = new ArrayList<>();
        for (Distribution d : new CycleService().listerVersements(userId, tontineId)) {
            if (d.getBeneficiaireId() != null && d.getBeneficiaireId() == userId) {
                liste.add(d);
            }
        }
        return liste;
    }
}
