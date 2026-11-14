package com.tontine.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import com.tontine.dao.AdhesionDao;
import com.tontine.dao.CycleDao;
import com.tontine.dao.DistributionDao;
import com.tontine.dao.TirageDao;
import com.tontine.dao.TransactionDao;
import com.tontine.dao.UtilisateurDao;
import com.tontine.model.Adhesion;
import com.tontine.model.Cycle;
import com.tontine.model.Distribution;
import com.tontine.model.Frequence;
import com.tontine.model.ModePaiement;
import com.tontine.model.StatutAdhesion;
import com.tontine.model.StatutCycle;
import com.tontine.model.StatutTransaction;
import com.tontine.model.Tirage;
import com.tontine.model.Tontine;
import com.tontine.model.Transaction;
import com.tontine.model.TypeTransaction;
import com.tontine.model.Utilisateur;

/**
 * Cotisations (Wave et Orange Money uniquement : aucune cotisation en especes), confirmation des remboursements de pret en especes et historique.
 * Fonds commun : montant libre dans l'intervalle defini par le gerant (minimum 1 000 FCFA), une cotisation par periode (jour, semaine ou mois) pendant tout le cycle,
 * penalite de retard si elle est payee apres la fin de sa periode. Tour de role / tirage au sort : montant fixe, une par cycle.
 */
public class CotisationService {

    /** Fonds commun : le gerant definit l'intervalle [min, max] ; le minimum ne peut pas descendre sous cette valeur. */
    public static final double MIN_FONDS_COMMUN = 1000;

    private final AdhesionDao adhesionDao = new AdhesionDao();
    private final CycleDao cycleDao = new CycleDao();
    private final DistributionDao distributionDao = new DistributionDao();
    private final TirageDao tirageDao = new TirageDao();
    private final TransactionDao transactionDao = new TransactionDao();
    private final UtilisateurDao utilisateurDao = new UtilisateurDao();

    /**
     * Le membre cotise. Wave / Orange Money : confirme tout de suite (simulation).
     * Les especes ne sont pas acceptees pour les cotisations (membres comme gerant).
     */
    public Transaction payerCotisation(int userId, int tontineId, double montant, ModePaiement mode,
            String numeroMobile) {
        Controles.utilisateur(userId);
        Tontine t = Controles.tontine(tontineId);
        Adhesion a = Controles.adhesionActive(tontineId, userId);
        Cycle c = Controles.cycleEnCours(t);
        Controles.exiger(mode != null, "Choisissez un mode de paiement.");
        Controles.exiger(mode != ModePaiement.ESPECE, "Les cotisations en esp\u00e8ces ne sont pas accept\u00e9es : payez par Wave ou Orange Money.");
        controlerMontant(t, c, userId, montant);

        Transaction tx = nouvelleTransaction(t, c, userId, montant, mode);
        tx.setReference(PasserelleSimulee.initierPaiement(mode, Controles.arrondir(montant), numeroMobile));
        tx.setStatut(StatutTransaction.CONFIRMEE);
        transactionDao.insert(tx);
        appliquer(tx, t, c, a);
        return tx;
    }

    /**
     * Confirme (ou refuse) un paiement en especes en attente : cotisation ou remboursement de pret.
     * Fait par le gerant (jamais pour son propre paiement) ou par l'administrateur.
     */
    public Transaction confirmerPaiementEspece(int acteurId, int transactionId, boolean accepter) {
        Transaction tx = transactionDao.findById(transactionId);
        Controles.exiger(tx != null, "Transaction introuvable.");
        Controles.exiger(tx.getStatut() == StatutTransaction.EN_ATTENTE && tx.getModePaiement() == ModePaiement.ESPECE,
                "Ce paiement n'est pas en attente de confirmation.");
        Tontine t = Controles.tontine(tx.getTontineId());
        Utilisateur acteur = Controles.utilisateur(acteurId);
        if (!acteur.isAdmin()) {
            Controles.exigerGerant(t, acteurId);
            Controles.exiger(tx.getUserId() != acteurId,
                    "Vous ne pouvez pas confirmer votre propre paiement : l'administrateur doit le faire.");
        }

        if (!accepter) {
            transactionDao.changerStatut(tx.getId(), StatutTransaction.ECHOUEE);
            tx.setStatut(StatutTransaction.ECHOUEE);
            NotificationService.notifier(tx.getUserId(), "Votre paiement en esp\u00e8ces de "
                    + Format.montant(tx.getMontant()) + " n'a pas \u00e9t\u00e9 confirm\u00e9 (tontine " + t.getNom() + ").",
                    "COTISATION");
            return tx;
        }

        Cycle c = tx.getCycleId() == null ? null : cycleDao.findById(tx.getCycleId());
        Controles.exiger(c != null && c.getStatut() == StatutCycle.EN_COURS, "Le " + Libelles.cycle(t) + " de ce paiement est d\u00e9j\u00e0 cl\u00f4tur\u00e9.");
        transactionDao.changerStatut(tx.getId(), StatutTransaction.CONFIRMEE);
        tx.setStatut(StatutTransaction.CONFIRMEE);
        if (tx.getType() == TypeTransaction.COTISATION) {
            appliquer(tx, t, c, Controles.adhesionActive(t.getId(), tx.getUserId()));
        } else if (tx.getType() == TypeTransaction.REMBOURSEMENT) {
            new PretService().appliquerRemboursement(tx);
        }
        NotificationService.notifier(tx.getUserId(), "Votre paiement en esp\u00e8ces de "
                + Format.montant(tx.getMontant()) + " a \u00e9t\u00e9 confirm\u00e9 (tontine " + t.getNom() + ").", "COTISATION");
        return tx;
    }

    /** Paiements en especes a confirmer dans une tontine (vue du gerant). */
    public List<Transaction> paiementsEnAttente(int gerantId, int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exigerGerant(t, gerantId);
        List<Transaction> liste = new ArrayList<>();
        for (Transaction tx : transactionDao.findByTontine(tontineId)) {
            if (tx.getStatut() == StatutTransaction.EN_ATTENTE && tx.getModePaiement() == ModePaiement.ESPECE) {
                liste.add(tx);
            }
        }
        return liste;
    }

    /** Historique personnel d'un membre dans une tontine. */
    public List<Transaction> historiquePersonnel(int userId, int tontineId) {
        Controles.adhesionActive(tontineId, userId);
        return transactionDao.findByTontineEtUser(tontineId, userId);
    }

    /** Historique complet de la tontine, en lecture seule (regle de transparence). */
    public List<Transaction> historiqueTontine(int userId, int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exigerParticipant(t, userId);
        return transactionDao.findByTontine(tontineId);
    }

    /** Membres qui n'ont pas encore cotise dans le cycle en cours (fonds commun : pour la periode en cours). */
    public List<Utilisateur> membresSansCotisation(int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Cycle c = Controles.cycleEnCours(t);
        List<Utilisateur> liste = new ArrayList<>();
        for (Adhesion a : adhesionDao.findByTontine(tontineId)) {
            if (a.getStatut() != StatutAdhesion.ACTIF) {
                continue;
            }
            boolean manque;
            if (t.isFondsCommun()) {
                int dues = Math.min(Math.max(0, t.getDureeEnCycle() - decalage(t, c, a)),
                        periodesEcoulees(c, t.getFrequence(), LocalDate.now()) - decalage(t, c, a) + 1);
                manque = cotisationsFaites(c, a.getUserId()) < dues;
            } else {
                manque = cotisationDuCycle(c, a.getUserId()) <= 0 && motifExemption(t, c, a.getUserId()) == null;
            }
            if (manque) {
                liste.add(utilisateurDao.findById(a.getUserId()));
            }
        }
        return liste;
    }

    // ---------------------------------------------------------------- interne

    private void controlerMontant(Tontine t, Cycle c, int userId, double montant) {
        Controles.exiger(montant > 0, "Le montant doit \u00eatre positif.");
        if (t.isFondsCommun()) {
            Controles.exiger(t.getMontantMaxCotisation() != null, "L'intervalle de cotisation n'est pas d\u00e9fini : contactez le g\u00e9rant.");
            Controles.exiger(montant >= t.getMontantCotisation() - 0.005 && montant <= t.getMontantMaxCotisation() + 0.005,
                    "Le montant de la cotisation doit \u00eatre compris entre " + Format.montant(t.getMontantCotisation())
                            + " et " + Format.montant(t.getMontantMaxCotisation()) + ".");
        } else {
            Controles.exiger(Math.abs(montant - t.getMontantCotisation()) < 0.005,
                    "Le montant de la cotisation est fixe : " + Format.montant(t.getMontantCotisation()) + ".");
        }
        if (t.isFondsCommun()) {
            EtatCotisation e = etatCotisation(userId, t.getId());
            Controles.exiger(!e.termine(), "Vous avez d\u00e9j\u00e0 fait toutes vos cotisations de ce " + Libelles.cycle(t) + ".");
            Controles.exiger(e.peutCotiser, "Vous avez d\u00e9j\u00e0 cotis\u00e9 pour cette p\u00e9riode. "
                    + "Prochaine cotisation possible \u00e0 partir du " + Format.date(e.disponibleLe) + ".");
        } else {
            Controles.exiger(cotisationDuCycle(c, userId) <= 0,
                    "Vous avez d\u00e9j\u00e0 cotis\u00e9 (ou un paiement est en attente) pour ce " + Libelles.cycle(t) + ".");
            Controles.exiger(beneficiaireDuCycle(t, c) != null,
                    "Le tirage au sort de ce " + Libelles.cycle(t) + " doit avoir lieu avant les cotisations : vous cotisez pour le b\u00e9n\u00e9ficiaire tir\u00e9. "
                            + "Attendez que le g\u00e9rant lance le tirage.");
            String motif = motifExemption(t, c, userId);
            Controles.exiger(motif == null, motif);
        }
    }

    /** Somme des cotisations de l'utilisateur dans le cycle, hors paiements refuses. */
    private double cotisationDuCycle(Cycle c, int userId) {
        double somme = 0;
        for (Transaction tx : transactionDao.findByCycle(c.getId())) {
            if (tx.getUserId() == userId && tx.getType() == TypeTransaction.COTISATION
                    && tx.getStatut() != StatutTransaction.ECHOUEE) {
                somme += tx.getMontant();
            }
        }
        return somme;
    }

    private Transaction nouvelleTransaction(Tontine t, Cycle c, int userId, double montant, ModePaiement mode) {
        Transaction tx = new Transaction();
        tx.setTontineId(t.getId());
        tx.setUserId(userId);
        tx.setCycleId(c.getId());
        tx.setType(TypeTransaction.COTISATION);
        tx.setMontant(Controles.arrondir(montant));
        tx.setModePaiement(mode);
        return tx;
    }

    /**
     * Retire une penalite du solde du membre (aucun paiement en plus). Fonds commun : jamais plus que le solde
     * positif du membre (le fonds reste equilibre). Tour de role / tirage : le solde peut devenir negatif (ce que
     * le membre doit) ; la dette est retiree de ce qu'il recevra a son tour. Retourne le montant reellement preleve.
     */
    double prelever(Tontine t, Cycle c, Adhesion a, double montant, String reference, ModePaiement mode) {
        double soldeActuel = adhesionDao.findById(a.getId()).getSolde();
        double retrait = Controles.arrondir(t.isFondsCommun() ? Math.min(montant, Math.max(0, soldeActuel)) : montant);
        if (retrait <= 0) {
            return 0;
        }
        Transaction p = new Transaction();
        p.setTontineId(t.getId());
        p.setUserId(a.getUserId());
        p.setCycleId(c == null ? null : c.getId());
        p.setType(TypeTransaction.PENALITE);
        p.setMontant(retrait);
        p.setModePaiement(mode);
        p.setStatut(StatutTransaction.CONFIRMEE);
        p.setReference(reference);
        transactionDao.insert(p);
        adhesionDao.ajouterAuSolde(a.getId(), -retrait);
        return retrait;
    }

    /** Une cotisation confirmee : met a jour le solde et applique la penalite si elle est en retard. */
    private void appliquer(Transaction tx, Tontine t, Cycle c, Adhesion a) {
        adhesionDao.enregistrerCotisation(a.getId(), tx.getMontant());
        LocalDateTime limite = t.isFondsCommun() ? echeanceMembre(t, c, a, tx)
                : limiteTour(t, c);
        if (t.getPenaliteRetard() > 0 && retardPenalise(limite, tx.getDate())) {
            // la penalite n'est pas payee en plus : elle est retiree du solde du membre
            double prelevee = prelever(t, c, a, t.getPenaliteRetard(), "PEN-" + tx.getId(), tx.getModePaiement());
            if (prelevee > 0) {
                NotificationService.notifier(tx.getUserId(), "P\u00e9nalit\u00e9 de retard de "
                        + Format.montant(prelevee) + " d\u00e9duite de votre solde (tontine " + t.getNom() + ").", "PENALITE");
            }
        }
    }

    // ---------------------------------------------------------------- tour de role : reciprocite

    /**
     * Regle de reciprocite du tour de role : si A a cotise pour B (au tour de B), B doit cotiser pour A (au tour de A).
     * Si A n'a pas cotise pour B, B n'a pas a cotiser pour A. Retourne le motif de dispense du membre pour le cycle en
     * cours, ou null s'il doit cotiser. S'applique aux cycles du tour en cours.
     */
    String motifExemption(Tontine t, Cycle c, int userId) {
        if (t.isFondsCommun()) {
            return null;
        }
        Utilisateur beneficiaire = beneficiaireDuCycle(t, c);
        if (beneficiaire == null || beneficiaire.getId() == userId) {
            return null; // tirage pas encore lance, ou le membre est lui-meme le beneficiaire
        }
        for (Distribution d : distributionsDuTour(t)) {
            if (d.getBeneficiaireId() == userId && !aCotiseConfirme(d.getCycleId(), beneficiaire.getId())) {
                return beneficiaire.getNomComplet() + " n'a pas cotis\u00e9 pour vous lors de votre tour : "
                        + "vous n'avez pas \u00e0 cotiser pour lui/elle ce " + Libelles.cycle(t) + ".";
            }
        }
        return null;
    }

    /** Beneficiaire du cycle en cours : tour de role = prochain de l'ordre ; tirage au sort = resultat du tirage (null s'il n'a pas eu lieu). */
    Utilisateur beneficiaireDuCycle(Tontine t, Cycle c) {
        if (t.isTourDeRole()) {
            return new CycleService().prochainBeneficiaire(t.getId());
        }
        for (Tirage ti : tirageDao.findByTontine(t.getId())) {
            if (ti.getSemaine() == c.getNumero()) {
                return utilisateurDao.findById(ti.getBeneficiaireId());
            }
        }
        return null;
    }

    /** Cycles clos du tour en cours (les plus recents, au nombre de tourActuelIndex), du plus ancien au plus recent. */
    private List<Distribution> distributionsDuTour(Tontine t) {
        List<Distribution> toutes = new ArrayList<>(distributionDao.findByTontine(t.getId()));
        toutes.sort((x, y) -> Integer.compare(x.getCycleId(), y.getCycleId()));
        int n = Math.min(Math.max(0, t.getTourActuelIndex()), toutes.size());
        return toutes.subList(toutes.size() - n, toutes.size());
    }

    private boolean aCotiseConfirme(int cycleId, int userId) {
        for (Transaction tx : transactionDao.findByCycle(cycleId)) {
            if (tx.getUserId() == userId && tx.getType() == TypeTransaction.COTISATION
                    && tx.getStatut() == StatutTransaction.CONFIRMEE) {
                return true;
            }
        }
        return false;
    }

    /** Fin du jour de la date limite (23 h 59) : au-dela, le membre n'est plus "en retard" mais "absent". */
    public static LocalDateTime finDeJourLimite(LocalDateTime limite) {
        return limite.toLocalDate().atTime(23, 59, 59);
    }

    /** Retard penalisable : apres la date et l'heure limites mais au plus tard a 23 h 59 du meme jour. */
    public static boolean retardPenalise(LocalDateTime limite, LocalDateTime instant) {
        return instant.isAfter(limite) && !instant.isAfter(finDeJourLimite(limite));
    }

    /** Penalite qui sera retiree du solde du membre s'il cotise maintenant (retard). */
    private double penalitesPrevues(Tontine t, Cycle c, int userId) {
        if (t.isFondsCommun()) {
            return etatCotisation(userId, t.getId()).penalite;
        }
        double total = 0;
        if (t.getPenaliteRetard() > 0 && retardPenalise(limiteTour(t, c), LocalDateTime.now())) {
            total += t.getPenaliteRetard();
        }
        return Controles.arrondir(total);
    }

    /** Situation d'un membre d'un tour de role / tirage au sort (beneficiaire du cycle, reciprocite, : dispense, arrieres, retard. */
    public EtatCotisation etatTour(int userId, int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Controles.adhesionActive(tontineId, userId);
        Cycle c = Controles.cycleEnCours(t);
        EtatCotisation e = new EtatCotisation();
        Utilisateur beneficiaire = beneficiaireDuCycle(t, c);
        e.beneficiaire = beneficiaire == null ? null : beneficiaire.getNomComplet();
        e.tirageAttendu = beneficiaire == null;
        e.exemption = motifExemption(t, c, userId);
        e.limite = limiteTour(t, c);
        e.echeance = e.limite.toLocalDate();
        e.enRetard = LocalDateTime.now().isAfter(e.limite);
        e.absent = LocalDateTime.now().isAfter(finDeJourLimite(e.limite)) && cotisationDuCycle(c, userId) <= 0 && e.exemption == null;
        e.penalite = penalitesPrevues(t, c, userId);
        e.faites = cotisationDuCycle(c, userId) > 0 ? 1 : 0;
        e.attendues = e.exemption == null ? 1 : 0;
        e.peutCotiser = e.exemption == null && e.faites == 0 && !e.tirageAttendu;
        return e;
    }

    /**
     * Cloture d'un tour de role / tirage : un membre qui devait cotiser et ne l'a pas fait est marque ABSENT (simple
     * notification, AUCUNE penalite : la penalite ne vise que le retard de moins de 24 h apres la date limite).
     */
    void signalerAbsents(Tontine t, Cycle c) {
        if (t.isFondsCommun()) {
            return;
        }
        for (Adhesion a : adhesionDao.findByTontine(t.getId())) {
            if (a.getStatut() == StatutAdhesion.ACTIF && cotisationDuCycle(c, a.getUserId()) <= 0
                    && motifExemption(t, c, a.getUserId()) == null) {
                NotificationService.notifier(a.getUserId(), "Vous n'avez pas cotis\u00e9 au " + Libelles.cycle(t) + " " + c.getNumero()
                        + " : vous \u00eates marqu\u00e9(e) absent(e) (tontine " + t.getNom() + ").", "ABSENCE");
            }
        }
    }

    // ---------------------------------------------------------------- fonds commun : une cotisation par periode

    /** Date de debut de la periode numero p (0 = debut du cycle) : debut + p jours, semaines ou mois. */
    static LocalDate debutPeriode(Cycle c, Frequence f, int p) {
        switch (f) {
        case JOURNALIERE:
            return c.getDateDebut().plusDays(p);
        case HEBDOMADAIRE:
            return c.getDateDebut().plusWeeks(p);
        default:
            return c.getDateDebut().plusMonths(p);
        }
    }

    /** Date de fin prevue d'un cycle de fonds commun : debut + duree x periode. */
    public static LocalDate finDeCycle(Cycle c, Tontine t) {
        return debutPeriode(c, t.getFrequence(), t.getDureeEnCycle());
    }

    /** Nombre de periodes entierement ecoulees a la date d (plafonne a 1000). */
    private static int periodesEcoulees(Cycle c, Frequence f, LocalDate d) {
        int p = 0;
        while (p < 1000 && !debutPeriode(c, f, p + 1).isAfter(d)) {
            p++;
        }
        return p;
    }

    /** Un membre arrive en cours de cycle ne doit que les periodes restantes : decalage en periodes. */
    private static int decalage(Tontine t, Cycle c, Adhesion a) {
        if (a.getDateAdhesion() == null || !a.getDateAdhesion().toLocalDate().isAfter(c.getDateDebut())) {
            return 0;
        }
        return Math.min(t.getDureeEnCycle(), periodesEcoulees(c, t.getFrequence(), a.getDateAdhesion().toLocalDate()));
    }

    /** Cotisations deja faites par le membre dans le cycle (confirmees ou en attente). */
    private int cotisationsFaites(Cycle c, int userId) {
        int n = 0;
        for (Transaction tx : transactionDao.findByCycle(c.getId())) {
            if (tx.getUserId() == userId && tx.getType() == TypeTransaction.COTISATION
                    && tx.getStatut() != StatutTransaction.ECHOUEE) {
                n++;
            }
        }
        return n;
    }

    /** Date limite de la cotisation 'tx' : fin de la periode correspondant a son rang. */
    private LocalDateTime echeanceMembre(Tontine t, Cycle c, Adhesion a, Transaction tx) {
        int rang = 0;
        for (Transaction autre : transactionDao.findByCycle(c.getId())) {
            if (autre.getUserId() == tx.getUserId() && autre.getType() == TypeTransaction.COTISATION
                    && autre.getStatut() != StatutTransaction.ECHOUEE && autre.getId() <= tx.getId()) {
                rang++;
            }
        }
        int p = decalage(t, c, a) + Math.max(1, rang);
        return limite(t, debutPeriode(c, t.getFrequence(), p - 1), debutPeriode(c, t.getFrequence(), p));
    }

    /**
     * Date et heure limites d'une cotisation pour la periode [debut, fin] : le jour choisi par le gerant (jour de la
     * semaine, du mois, ou chaque jour) a l'heure choisie. Sans jour defini, c'est la fin de periode.
     */
    public static LocalDateTime limite(Tontine t, LocalDate debut, LocalDate fin) {
        LocalDate d = fin;
        Integer j = t.getJourCotisation();
        if (j != null && t.getFrequence() != Frequence.JOURNALIERE) {
            if (t.getFrequence() == Frequence.HEBDOMADAIRE) {
                d = fin.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(j)));
            } else {
                d = jourPresent(fin.getYear(), fin.getMonthValue(), j);
                if (d.isAfter(fin)) {
                    LocalDate precedent = fin.minusMonths(1);
                    d = jourPresent(precedent.getYear(), precedent.getMonthValue(), j);
                }
            }
            if (d.isBefore(debut)) {
                d = fin;
            }
        }
        return LocalDateTime.of(d, t.getHeureCotisation());
    }

    /** Le jour demande du mois, ramene au dernier jour du mois s'il n'existe pas (ex. 31 en fevrier). */
    private static LocalDate jourPresent(int annee, int mois, int jour) {
        return LocalDate.of(annee, mois, Math.min(jour, YearMonth.of(annee, mois).lengthOfMonth()));
    }

    /**
     * Situation du membre dans le fonds commun : cotisations faites / attendues, peut-il cotiser maintenant,
     * date limite et penalite eventuelle. Une seule cotisation par periode ; en retard, la penalite est retiree du solde.
     */
    public EtatCotisation etatCotisation(int userId, int tontineId) {
        Tontine t = Controles.tontine(tontineId);
        Adhesion a = Controles.adhesionActive(tontineId, userId);
        Cycle c = Controles.cycleEnCours(t);
        Frequence f = t.getFrequence();
        LocalDate aujourdhui = LocalDate.now();
        int decalage = decalage(t, c, a);

        EtatCotisation e = new EtatCotisation();
        e.finCycle = finDeCycle(c, t);
        e.attendues = Math.max(0, t.getDureeEnCycle() - decalage);
        e.faites = cotisationsFaites(c, userId);
        e.rang = e.faites + 1;
        if (e.termine()) {
            return e;
        }
        LocalDate debut = debutPeriode(c, f, decalage + e.rang - 1);
        e.limite = limite(t, debut, debutPeriode(c, f, decalage + e.rang));
        e.echeance = e.limite.toLocalDate();
        e.peutCotiser = !aujourdhui.isBefore(debut);
        e.disponibleLe = debut;
        e.enRetard = LocalDateTime.now().isAfter(e.limite);
        e.absent = LocalDateTime.now().isAfter(finDeJourLimite(e.limite));
        e.penalite = retardPenalise(e.limite, LocalDateTime.now()) ? t.getPenaliteRetard() : 0;
        return e;
    }

    /** Nombre de cotisations manquantes d'un membre a la fin du cycle (utilise a la cloture). */
    int cotisationsManquantes(Tontine t, Cycle c, Adhesion a) {
        int attendues = Math.max(0, t.getDureeEnCycle() - decalage(t, c, a));
        return Math.max(0, attendues - cotisationsFaites(c, a.getUserId()));
    }

    /** Date limite de cotisation d'un tour : journalier = le jour du tour a l'heure fixee ; sinon jour fixe dans la periode. */
    public static LocalDateTime limiteTour(Tontine t, Cycle c) {
        if (t.getFrequence() == Frequence.JOURNALIERE) {
            return LocalDateTime.of(c.getDateDebut(), t.getHeureCotisation());
        }
        return limite(t, c.getDateDebut(), echeance(c, t.getFrequence()));
    }

    /** Date limite de cotisation d'un cycle (tour de role / tirage au sort) : debut + une periode. */
    static LocalDate echeance(Cycle c, Frequence f) {
        switch (f) {
        case JOURNALIERE:
            return c.getDateDebut().plusDays(1);
        case HEBDOMADAIRE:
            return c.getDateDebut().plusWeeks(1);
        default:
            return c.getDateDebut().plusMonths(1);
        }
    }
}
