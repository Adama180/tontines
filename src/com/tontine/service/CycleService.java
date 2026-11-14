package com.tontine.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tontine.dao.AdhesionDao;
import com.tontine.dao.CycleDao;
import com.tontine.dao.DemandeTourAnticipeDao;
import com.tontine.dao.DistributionDao;
import com.tontine.dao.PretDao;
import com.tontine.dao.TirageDao;
import com.tontine.dao.TontineDao;
import com.tontine.dao.TransactionDao;
import com.tontine.dao.UtilisateurDao;
import com.tontine.model.Adhesion;
import com.tontine.model.Cycle;
import com.tontine.model.DemandeTourAnticipe;
import com.tontine.model.Distribution;
import com.tontine.model.StatutAdhesion;
import com.tontine.model.StatutDemande;
import com.tontine.model.StatutPret;
import com.tontine.model.StatutTontine;
import com.tontine.model.StatutTransaction;
import com.tontine.model.Tirage;
import com.tontine.model.Tontine;
import com.tontine.model.Transaction;
import com.tontine.model.TypeTransaction;
import com.tontine.model.Utilisateur;

/**
 * Cycles, versements, tour de role et tirage au sort.
 * Fonds commun : le cycle est demarre une seule fois et dure duree x periode ; a sa fin, chacun recoit ses
 * cotisations cumulees + une part egale des interets et de toutes les penalites de retard. Tour de role / tirage au sort : un cycle = une periode ; le beneficiaire
 * recoit le total collecte (cotisations + penalites) ; quand tous ont recu, un nouveau tour recommence.
 */
public class CycleService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AdhesionDao adhesionDao = new AdhesionDao();
    private final CycleDao cycleDao = new CycleDao();
    private final DistributionDao distributionDao = new DistributionDao();
    private final TirageDao tirageDao = new TirageDao();
    private final TontineDao tontineDao = new TontineDao();
    private final TransactionDao transactionDao = new TransactionDao();
    private final PretDao pretDao = new PretDao();
    private final UtilisateurDao utilisateurDao = new UtilisateurDao();
    private final DemandeTourAnticipeDao tourAnticipeDao = new DemandeTourAnticipeDao();

    // ---------------------------------------------------------------- cycles

    public Cycle demarrerCycle(int gerantId, int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exigerGerant(t, gerantId);
        Controles.exiger(t.isFondsCommun() || t.getStatut() == StatutTontine.CLOTUREE,
                "Les tours d\u00e9marrent automatiquement selon la fr\u00e9quence de cotisation : vous n'avez rien \u00e0 d\u00e9marrer.");
        Controles.exiger(cycleDao.findEnCours(tontineId) == null, "Un " + Libelles.cycle(t) + " est d\u00e9j\u00e0 en cours.");
        if (t.getStatut() == StatutTontine.CLOTUREE) { // relance : ceux qui n'ont pas repondu a l'alerte sont retires
            new RenouvellementService().preparerRelance(t, gerantId);
        }
        Controles.exiger(adhesionDao.compterActifs(tontineId) >= 2,
                "Il faut au moins 2 membres actifs pour d\u00e9marrer un " + Libelles.cycle(t) + ".");
        if (t.isFondsCommun()) {
            Controles.exiger(t.getMontantMaxCotisation() != null && t.getMontantMaxCotisation() >= t.getMontantCotisation()
                    && t.getMontantCotisation() >= CotisationService.MIN_FONDS_COMMUN,
                    "D\u00e9finissez d'abord l'intervalle de cotisation (montant minimum et maximum) dans les param\u00e8tres de la tontine.");
            Controles.exiger(t.getTauxInteret() != null && t.getDureeMaxPret() != null,
                    "D\u00e9finissez d'abord le taux d'int\u00e9r\u00eat et la dur\u00e9e maximale des pr\u00eats (param\u00e8tres de la tontine).");
        }

        Cycle c = new Cycle();
        c.setTontineId(tontineId);
        c.setNumero(cycleDao.dernierNumero(tontineId) + 1);
        c.setDateDebut(LocalDate.now());
        cycleDao.insert(c);
        if (t.getStatut() == StatutTontine.CLOTUREE) { // relance d'une tontine terminee : elle redevient visible
            tontineDao.changerStatut(tontineId, StatutTontine.ACTIVE);
            new AdhesionService().refuserDepartsEnAttente(t);
        }

        NotificationService.notifierMembres(tontineId, "Le " + Libelles.cycle(t) + " " + c.getNumero() + " de la tontine "
                + t.getNom() + " a d\u00e9marr\u00e9. Pensez \u00e0 cotiser !", "CYCLE", null);
        Controles.audit(gerantId, "CYCLE_DEMARRE", "Tontine " + t.getNom() + " - " + Libelles.cycle(t) + " " + c.getNumero());
        return c;
    }

    /** Cloture le cycle en cours et verse les sommes dues. Retourne les versements effectues. */
    public List<Distribution> cloturerCycle(int gerantId, int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exigerGerant(t, gerantId);
        if (!t.isFondsCommun()) {
            Cycle c = cycleDao.findEnCours(tontineId);
            Controles.exiger(c != null, "Aucun " + Libelles.cycle(t) + " en cours.");
            Controles.exiger(peutVerser(tontineId), "Le versement est automatique \u00e0 la date limite de cotisation (23 h 59). "
                    + "Le bouton Verser n'est disponible que lorsque tous les membres ont cotis\u00e9 (et, au tirage au sort, apr\u00e8s le tirage).");
        }
        return cloturer(t, gerantId);
    }

    /** Versement automatique du systeme : tour de role / tirage au sort dont la date limite (23 h 59) est depassee. */
    public List<Distribution> verserAutomatiquement(int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Cycle c = cycleDao.findEnCours(tontineId);
        Controles.exiger(!t.isFondsCommun() && c != null, "Aucun tour en cours.");
        Controles.exiger(java.time.LocalDateTime.now().isAfter(CotisationService.finDeJourLimite(CotisationService.limiteTour(t, c))),
                "La date limite de cotisation n'est pas encore pass\u00e9e.");
        return cloturer(t, t.getGerantId());
    }

    /**
     * Tour de role / tirage au sort : le gerant peut verser la cagnotte avant l'echeance seulement si tous les membres
     * tenus de cotiser l'ont fait (et, au tirage au sort, apres le tirage). Sinon le systeme verse tout seul a 23 h 59.
     */
    public boolean peutVerser(int tontineId) {
        Tontine t = tontineDao.findById(tontineId);
        Cycle c = cycleDao.findEnCours(tontineId);
        if (t == null || c == null || t.isFondsCommun() || !tourTire(t, c)) {
            return false;
        }
        return new CotisationService().membresSansCotisation(tontineId).isEmpty()
                && transactionDao.totalParTypeEtCycle(c.getId(), TypeTransaction.COTISATION) > 0;
    }

    // ---------------------------------------------------------------- tours automatiques

    /** Parcourt les tontines a tour de role / tirage au sort actives (appele chaque minute par l'application). */
    public void avancerTours() {
        for (Tontine t : tontineDao.findAll()) {
            if (!t.isFondsCommun() && t.getStatut() == StatutTontine.ACTIVE) {
                try {
                    assurerTourEnCours(t.getId());
                } catch (RuntimeException e) {
                    // une tontine en erreur ne doit pas bloquer les autres
                }
            }
        }
    }

    /**
     * Tour de role / tirage au sort : sans action du gerant, cloture le tour dont la periode est finie
     * (tour de role, ou tirage deja fait) puis ouvre le tour suivant a la date prevue par la frequence.
     * Idempotent : peut etre appele a tout moment.
     */
    public void assurerTourEnCours(int tontineId) {
        Tontine t = tontineDao.findById(tontineId);
        if (t == null || t.isFondsCommun() || t.getStatut() != StatutTontine.ACTIVE) {
            return;
        }
        Cycle c = cycleDao.findEnCours(tontineId);
        LocalDate aujourdhui = LocalDate.now();
        if (c != null) {
            boolean periodeFinie = java.time.LocalDateTime.now().isAfter(
                    CotisationService.finDeJourLimite(CotisationService.limiteTour(t, c)));
            boolean pret = t.isTourDeRole() || tourTire(t, c);
            if (!periodeFinie || !pret) {
                return;
            }
            try {
                cloturer(t, t.getGerantId());
            } catch (ServiceException e) {
                return; // ex. paiement en attente : le tour reste ouvert
            }
            t = tontineDao.findById(tontineId);
            if (t.getStatut() != StatutTontine.ACTIVE) {
                return; // tontine terminee : renouvellement
            }
        }
        if (cycleDao.findEnCours(tontineId) != null || adhesionDao.compterActifs(tontineId) < 2) {
            return;
        }
        List<Cycle> cycles = cycleDao.findByTontine(tontineId);
        Cycle dernier = null;
        for (Cycle x : cycles) {
            if (dernier == null || x.getNumero() > dernier.getNumero()) {
                dernier = x;
            }
        }
        if (dernier != null && aujourdhui.isBefore(CotisationService.echeance(dernier, t.getFrequence()))) {
            return;
        }
        Cycle n = new Cycle();
        n.setTontineId(tontineId);
        n.setNumero(cycleDao.dernierNumero(tontineId) + 1);
        n.setDateDebut(aujourdhui);
        cycleDao.insert(n);
        String suite = t.isTirageAuSort() ? " Le g\u00e9rant va lancer le tirage, puis vous pourrez cotiser." : " Pensez \u00e0 cotiser !";
        NotificationService.notifierMembres(tontineId, "Le " + Libelles.cycle(t) + " " + n.getNumero() + " de la tontine "
                + t.getNom() + " a d\u00e9marr\u00e9 automatiquement." + suite, "CYCLE", null);
    }

    private boolean tourTire(Tontine t, Cycle c) {
        if (!t.isTirageAuSort()) {
            return true;
        }
        for (Tirage ti : tirageDao.findByTontine(t.getId())) {
            if (ti.getSemaine() == c.getNumero()) {
                return true;
            }
        }
        return false;
    }

    private List<Distribution> cloturer(Tontine t, int gerantId) {
        int tontineId = t.getId();
        Cycle c = cycleDao.findEnCours(tontineId);
        Controles.exiger(c != null, "Aucun " + Libelles.cycle(t) + " en cours \u00e0 cl\u00f4turer.");
        for (Transaction tx : transactionDao.findByCycle(c.getId())) {
            Controles.exiger(tx.getStatut() != StatutTransaction.EN_ATTENTE,
                    "Des paiements en esp\u00e8ces attendent encore votre confirmation.");
        }
        List<Distribution> versements = t.isFondsCommun() ? cloturerFondsCommun(t, c) : cloturerTourEnCours(t, c);
        cycleDao.cloturer(c.getId(), LocalDate.now());
        NotificationService.notifierMembres(tontineId, "Le " + Libelles.cycle(t) + " " + c.getNumero() + " de la tontine "
                + t.getNom() + " est cl\u00f4tur\u00e9.", "CYCLE", null);
        // tontine terminee : fonds commun apres son cycle ; tour de role / tirage quand tous les membres ont recu leur cagnotte
        boolean terminee = t.isFondsCommun() || tontineDao.findById(tontineId).getTourActuelIndex() == 0;
        if (terminee) {
            tontineDao.changerStatut(tontineId, StatutTontine.CLOTUREE);
            NotificationService.notifierMembres(tontineId, "La tontine " + t.getNom() + " est termin\u00e9e : "
                    + (t.isFondsCommun() ? "chaque membre a re\u00e7u sa cagnotte" : "tous les membres ont re\u00e7u leur cagnotte")
                    + ". Le g\u00e9rant va vous envoyer une alerte de renouvellement : vous pourrez valider (continuer) ou refuser (quitter la tontine).", "CYCLE", null);
        }
        Controles.audit(gerantId, "CYCLE_CLOTURE", "Tontine " + t.getNom() + " - " + Libelles.cycle(t) + " " + c.getNumero());
        return versements;
    }

    public List<Cycle> listerCycles(int userId, int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exigerParticipant(t, userId);
        return cycleDao.findByTontine(tontineId);
    }

    public List<Distribution> listerVersements(int userId, int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exigerParticipant(t, userId);
        return distributionDao.findByTontine(tontineId);
    }

    // ---------------------------------------------------------------- tour de role

    /** Prochain beneficiaire d'une tontine a tour de role. */
    public Utilisateur prochainBeneficiaire(int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exiger(t.isTourDeRole(), "Cette tontine n'est pas \u00e0 tour de r\u00f4le.");
        List<Adhesion> ordre = adhesionDao.findByTontineOrdreTour(tontineId);
        Controles.exiger(!ordre.isEmpty(), "Aucun membre actif.");
        return utilisateurDao.findById(ordre.get(t.getTourActuelIndex() % ordre.size()).getUserId());
    }

    public DemandeTourAnticipe demanderTourAnticipe(int userId, int tontineId, String motif) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exiger(t.isTourDeRole(), "Le tour anticip\u00e9 n'existe que dans les tontines \u00e0 tour de r\u00f4le.");
        Controles.adhesionActive(tontineId, userId);
        Controles.exiger(motif != null && !motif.trim().isEmpty(), "Indiquez le motif de votre demande.");
        for (DemandeTourAnticipe d : tourAnticipeDao.findByUser(userId)) {
            Controles.exiger(d.getTontineId() != tontineId || d.getStatut() != StatutDemande.EN_ATTENTE,
                    "Vous avez d\u00e9j\u00e0 une demande de tour anticip\u00e9 en attente.");
        }
        DemandeTourAnticipe d = new DemandeTourAnticipe();
        d.setTontineId(tontineId);
        d.setUserId(userId);
        d.setMotif(motif.trim());
        tourAnticipeDao.insert(d);
        NotificationService.notifier(t.getGerantId(), "Demande de tour anticip\u00e9 de "
                + Controles.utilisateur(userId).getNomComplet() + " (tontine " + t.getNom() + ").", "TOUR");
        return d;
    }

    public List<DemandeTourAnticipe> demandesTourAnticipe(int gerantId, int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exigerGerant(t, gerantId);
        return tourAnticipeDao.findByTontineEtStatut(tontineId, StatutDemande.EN_ATTENTE);
    }

    /**
     * Accepte ou refuse un tour anticipe : le membre prend la place du prochain beneficiaire
     * (echange d'ordre). Entre deux cycles uniquement, et pas si le membre a deja recu dans ce tour.
     */
    public void traiterDemandeTourAnticipe(int gerantId, int demandeId, boolean accepter) {
        DemandeTourAnticipe d = tourAnticipeDao.findById(demandeId);
        Controles.exiger(d != null && d.getStatut() == StatutDemande.EN_ATTENTE, "Demande introuvable ou d\u00e9j\u00e0 trait\u00e9e.");
        Tontine t = Controles.tontine(d.getTontineId());
        Controles.exigerGerant(t, gerantId);

        if (accepter) {
            Controles.exiger(cycleDao.findEnCours(t.getId()) == null,
                    "Traitez cette demande entre deux " + Libelles.cycle(t) + "s (apr\u00e8s la cl\u00f4ture du " + Libelles.cycle(t) + " en cours).");
            List<Adhesion> ordre = adhesionDao.findByTontineOrdreTour(t.getId());
            int index = t.getTourActuelIndex() % Math.max(1, ordre.size());
            int position = -1;
            for (int i = 0; i < ordre.size(); i++) {
                if (ordre.get(i).getUserId() == d.getUserId()) {
                    position = i;
                }
            }
            Controles.exiger(position >= 0, "Ce membre n'est plus actif.");
            Controles.exiger(position >= index, "Ce membre a d\u00e9j\u00e0 re\u00e7u son tour dans ce tour de r\u00f4le.");
            if (position != index) {
                Adhesion suivant = ordre.get(index);
                Adhesion demandeur = ordre.get(position);
                Integer ordreSuivant = suivant.getOrdreTour();
                adhesionDao.definirOrdreTour(suivant.getId(), demandeur.getOrdreTour());
                adhesionDao.definirOrdreTour(demandeur.getId(), ordreSuivant);
            }
        }
        tourAnticipeDao.changerStatut(d.getId(), accepter ? StatutDemande.ACCEPTEE : StatutDemande.REFUSEE);
        NotificationService.notifier(d.getUserId(), "Votre demande de tour anticip\u00e9 a \u00e9t\u00e9 "
                + (accepter ? "accept\u00e9e." : "refus\u00e9e."), "TOUR");
        Controles.audit(gerantId, accepter ? "TOUR_ANTICIPE_ACCEPTE" : "TOUR_ANTICIPE_REFUSE",
                "Demande " + d.getId() + " - tontine " + t.getNom());
    }

    // ---------------------------------------------------------------- tirage au sort

    /** Tire au sort le beneficiaire du cycle en cours parmi les membres pas encore tires dans ce tour. */
    public Tirage lancerTirage(int gerantId, int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exigerGerant(t, gerantId);
        Controles.exiger(t.isTirageAuSort(), "Cette tontine n'est pas \u00e0 tirage au sort.");
        Cycle c = Controles.cycleEnCours(t);
        for (Tirage deja : tirageDao.findByTontine(tontineId)) {
            Controles.exiger(deja.getSemaine() != c.getNumero(),
                    "Le tirage de ce " + Libelles.cycle(t) + " a d\u00e9j\u00e0 eu lieu : son r\u00e9sultat ne peut pas \u00eatre refait.");
        }

        List<Adhesion> actifs = actifs(tontineId);
        Set<Integer> dejaTires = tiresDansLeTourEnCours(t, c);
        List<Adhesion> eligibles = new ArrayList<>();
        for (Adhesion a : actifs) {
            if (!dejaTires.contains(a.getUserId())) {
                eligibles.add(a);
            }
        }
        if (eligibles.isEmpty()) { // tout le monde a deja ete tire : nouveau tour
            tontineDao.changerTourActuel(tontineId, 0);
            eligibles = actifs;
        }
        Adhesion gagnant = eligibles.get(RANDOM.nextInt(eligibles.size()));

        Tirage tirage = new Tirage();
        tirage.setTontineId(tontineId);
        tirage.setSemaine(c.getNumero());
        tirage.setBeneficiaireId(gagnant.getUserId());
        tirageDao.insert(tirage);

        Utilisateur u = utilisateurDao.findById(gagnant.getUserId());
        NotificationService.notifierMembres(tontineId, "Tirage au sort du " + Libelles.cycle(t) + " " + c.getNumero() + " : "
                + u.getNomComplet() + " recevra la cagnotte (tontine " + t.getNom() + ").", "TIRAGE", null);
        Controles.audit(gerantId, "TIRAGE_AU_SORT", "Tontine " + t.getNom() + " - " + Libelles.cycle(t) + " " + c.getNumero()
                + " - gagnant " + u.getNomComplet());
        return tirage;
    }

    public List<Tirage> listerTirages(int userId, int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exigerParticipant(t, userId);
        return tirageDao.findByTontine(tontineId);
    }

    // ---------------------------------------------------------------- cloture : fonds commun

    private List<Distribution> cloturerFondsCommun(Tontine t, Cycle c) {
        LocalDate fin = CotisationService.finDeCycle(c, t);
        Controles.exiger(!LocalDate.now().isBefore(fin), "Le " + Libelles.cycle(t) + " se termine le " + Format.date(fin)
                + " (" + t.getDureeEnCycle() + " p\u00e9riode(s) de cotisation). Vous pourrez le cl\u00f4turer \u00e0 partir de cette date.");
        Controles.exiger(pretDao.findByTontineEtStatut(t.getId(), StatutPret.EN_ATTENTE).isEmpty()
                && pretDao.findByTontineEtStatut(t.getId(), StatutPret.ACCORDE).isEmpty(),
                "Des pr\u00eats sont encore en attente ou non rembours\u00e9s : le " + Libelles.cycle(t) + " ne peut pas \u00eatre cl\u00f4tur\u00e9.");

        double rembourse = transactionDao.totalParTypeEtCycle(c.getId(), TypeTransaction.REMBOURSEMENT);
        double prete = transactionDao.totalParTypeEtCycle(c.getId(), TypeTransaction.PRET);
        double penalites = transactionDao.totalParTypeEtCycle(c.getId(), TypeTransaction.PENALITE);
        double pot = Controles.arrondir(Math.max(0, rembourse - prete) + penalites); // interets + penalites

        List<Adhesion> actifs = actifs(t.getId());

        // cotisations jamais payees : le membre est marque absent (aucune penalite)
        CotisationService cotisations = new CotisationService();
        for (Adhesion a : actifs) {
            int manquantes = cotisations.cotisationsManquantes(t, c, a);
            if (manquantes > 0) {
                NotificationService.notifier(a.getUserId(), "Absent(e) \u00e0 " + manquantes + " cotisation(s) de ce " + Libelles.cycle(t)
                        + " (tontine " + t.getNom() + ") : aucune p\u00e9nalit\u00e9, mais ces absences sont not\u00e9es.", "ABSENCE");
            }
        }

        double partEgale = actifs.isEmpty() ? 0 : Math.floor(pot * 100.0 / actifs.size()) / 100.0;
        double reste = pot;
        int restants = actifs.size();

        List<Distribution> versements = new ArrayList<>();
        for (Adhesion a : adhesionDao.findByTontine(t.getId())) {
            boolean actif = a.getStatut() == StatutAdhesion.ACTIF;
            double part = 0;
            if (actif) {
                restants--;
                part = restants == 0 ? Controles.arrondir(reste) : partEgale; // le dernier recoit l'arrondi
                reste = Controles.arrondir(reste - part);
            }
            double montant = Controles.arrondir(Math.max(0, a.getSolde()) + part);
            if (montant > 0) {
                versements.add(verser(t, c, a.getUserId(), montant));
            }
            if (a.getSolde() != 0) {
                adhesionDao.ajouterAuSolde(a.getId(), -a.getSolde());
            }
        }
        return versements;
    }

    // ---------------------------------------------------------------- cloture : tour de role / tirage

    private List<Distribution> cloturerTourEnCours(Tontine t, Cycle c) {
        int beneficiaireId;
        if (t.isTourDeRole()) {
            beneficiaireId = prochainBeneficiaire(t.getId()).getId();
        } else {
            Integer trouve = null;
            for (Tirage ti : tirageDao.findByTontine(t.getId())) {
                if (ti.getSemaine() == c.getNumero()) {
                    trouve = ti.getBeneficiaireId();
                }
            }
            Controles.exiger(trouve != null, "Lancez d'abord le tirage au sort de ce " + Libelles.cycle(t) + ".");
            beneficiaireId = trouve;
        }

        Controles.exiger(transactionDao.totalParTypeEtCycle(c.getId(), TypeTransaction.COTISATION) > 0,
                "Aucune cotisation n'a \u00e9t\u00e9 collect\u00e9e dans ce " + Libelles.cycle(t) + ".");
        // non-cotisation : le membre est marque absent (aucune penalite)
        new CotisationService().signalerAbsents(t, c);
        double pot = Controles.arrondir(transactionDao.totalParTypeEtCycle(c.getId(), TypeTransaction.COTISATION)
                + transactionDao.totalParTypeEtCycle(c.getId(), TypeTransaction.PENALITE));

        // chacun a "consomme" sa cotisation du cycle : son solde diminue d'autant
        Map<Integer, Double> cotise = new HashMap<>();
        for (Transaction tx : transactionDao.findByCycle(c.getId())) {
            if (tx.getType() == TypeTransaction.COTISATION && tx.getStatut() == StatutTransaction.CONFIRMEE) {
                cotise.merge(tx.getUserId(), tx.getMontant(), Double::sum);
            }
        }
        for (Adhesion a : adhesionDao.findByTontine(t.getId())) {
            Double somme = cotise.get(a.getUserId());
            if (somme != null) {
                adhesionDao.ajouterAuSolde(a.getId(), -somme);
            }
        }

        // ce que le beneficiaire doit encore (solde negatif : penalites deja retirees de son solde) est deduit de son versement
        Adhesion benef = adhesionDao.findByTontineEtUser(t.getId(), beneficiaireId);
        double dette = benef == null ? 0 : Math.max(0, -adhesionDao.findById(benef.getId()).getSolde());
        double retenueDette = Controles.arrondir(Math.min(dette, pot));
        if (retenueDette > 0) {
            adhesionDao.ajouterAuSolde(benef.getId(), retenueDette);
            NotificationService.notifier(beneficiaireId, "Vos p\u00e9nalit\u00e9s de " + Format.montant(retenueDette)
                    + " sont d\u00e9duites de votre versement (tontine " + t.getNom() + ").", "PENALITE");
        }
        List<Distribution> versements = new ArrayList<>();
        if (pot - retenueDette > 0) {
            versements.add(verser(t, c, beneficiaireId, Controles.arrondir(pot - retenueDette)));
        }

        int suivant = t.getTourActuelIndex() + 1;
        tontineDao.changerTourActuel(t.getId(), suivant >= adhesionDao.compterActifs(t.getId()) ? 0 : suivant);
        return versements;
    }

    // ---------------------------------------------------------------- outils

    /** Enregistre un versement : ligne de distribution + transaction + notification au beneficiaire. */
    private Distribution verser(Tontine t, Cycle c, int beneficiaireId, double montant) {
        Distribution d = new Distribution();
        d.setCycleId(c.getId());
        d.setBeneficiaireId(beneficiaireId);
        d.setMontantTotal(montant);
        distributionDao.insert(d);

        Transaction tx = new Transaction();
        tx.setTontineId(t.getId());
        tx.setUserId(beneficiaireId);
        tx.setCycleId(c.getId());
        tx.setType(TypeTransaction.DISTRIBUTION);
        tx.setMontant(montant);
        tx.setStatut(StatutTransaction.CONFIRMEE);
        tx.setReference("DIST-" + d.getId());
        transactionDao.insert(tx);

        NotificationService.notifier(beneficiaireId, "Vous recevez la cagnotte : " + Format.montant(montant)
                + " (tontine " + t.getNom() + ", " + Libelles.cycle(t) + " " + c.getNumero() + ").", "VERSEMENT");
        return d;
    }

    private List<Adhesion> actifs(int tontineId) {
        List<Adhesion> liste = new ArrayList<>();
        for (Adhesion a : adhesionDao.findByTontine(tontineId)) {
            if (a.getStatut() == StatutAdhesion.ACTIF) {
                liste.add(a);
            }
        }
        return liste;
    }

    /** Beneficiaires deja tires dans le tour en cours : les derniers tirages (hors cycle courant). */
    private Set<Integer> tiresDansLeTourEnCours(Tontine t, Cycle c) {
        List<Tirage> passes = new ArrayList<>();
        for (Tirage ti : tirageDao.findByTontine(t.getId())) {
            if (ti.getSemaine() < c.getNumero()) {
                passes.add(ti);
            }
        }
        int n = Math.min(t.getTourActuelIndex(), passes.size());
        Set<Integer> ids = new HashSet<>();
        for (Tirage ti : passes.subList(passes.size() - n, passes.size())) {
            ids.add(ti.getBeneficiaireId());
        }
        return ids;
    }
}
