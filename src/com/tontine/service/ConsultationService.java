package com.tontine.service;

import java.util.ArrayList;
import java.util.List;

import com.tontine.dao.AdhesionDao;
import com.tontine.dao.TontineDao;
import com.tontine.model.Adhesion;
import com.tontine.model.Tontine;
import com.tontine.model.TypeTontine;

/** Consultation publique (visiteur) et liste des tontines d'un utilisateur. */
public class ConsultationService {

    private final TontineDao tontineDao = new TontineDao();
    private final AdhesionDao adhesionDao = new AdhesionDao();

    public TypeTontine[] listerTypes() {
        return TypeTontine.values();
    }

    public List<Tontine> listerTontines(TypeTontine type) {
        return tontineDao.findByType(type);
    }

    public List<Tontine> listerToutesLesTontines() {
        return tontineDao.findActives();
    }

    public Tontine detail(int tontineId) {
        return Controles.tontine(tontineId);
    }

    public int nombreMembres(int tontineId) {
        return adhesionDao.compterActifs(tontineId);
    }

    /** Regles generales visibles par tout le monde (sans historique ni soldes). */
    public List<String> reglesGenerales(Tontine t) {
        List<String> r = new ArrayList<>();
        r.add("Type : " + Libelles.type(t.getType()));
        r.add("Cotisation : " + Libelles.cotisation(t) + " (" + Libelles.frequence(t.getFrequence()) + ")");
        r.add("Date et heure de cotisation : " + Libelles.horaire(t));
        r.add(t.getPenaliteRetard() > 0
                ? "P\u00e9nalit\u00e9 de retard : " + Format.montant(t.getPenaliteRetard()) + " (retir\u00e9e du solde si la cotisation arrive avant 23 h 59 le jour limite ; au-del\u00e0, le membre est marqu\u00e9 absent, sans p\u00e9nalit\u00e9)"
                : "P\u00e9nalit\u00e9 de retard : aucune (le membre qui ne cotise pas est marqu\u00e9 absent)");
        r.add("Membres : " + nombreMembres(t.getId()) + " / " + t.getNbMembreMax());
        if (t.isFondsCommun()) {
            r.add("Taux d'int\u00e9r\u00eat des pr\u00eats : " + (t.getTauxInteret() == null ? "non d\u00e9fini" : t.getTauxInteret() + " %"));
            r.add("Dur\u00e9e maximale d'un pr\u00eat : " + (t.getDureeMaxPret() == null ? "non d\u00e9finie" : t.getDureeMaxPret() + " jours"));
        }
        return r;
    }

    /** Tontines dont l'utilisateur est membre actif (ou gerant). */
    public List<Tontine> mesTontines(int userId) {
        return tontineDao.findByMembre(userId);
    }

    /** Participation de l'utilisateur a une tontine, ou null s'il n'en fait pas partie. */
    public Adhesion monAdhesion(int userId, int tontineId) {
        return adhesionDao.findByTontineEtUser(tontineId, userId);
    }
}
