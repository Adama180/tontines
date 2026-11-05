package com.tontine;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tontine.dao.CycleDao;
import com.tontine.dao.TontineDao;
import com.tontine.dao.TransactionDao;
import com.tontine.dao.UtilisateurDao;
import com.tontine.model.Cycle;
import com.tontine.model.DemandeAdhesion;
import com.tontine.model.DemandeCreation;
import com.tontine.model.DemandeTourAnticipe;
import com.tontine.model.Distribution;
import com.tontine.model.Frequence;
import com.tontine.model.ModePaiement;
import com.tontine.model.Pret;
import com.tontine.model.StatutDemande;
import com.tontine.model.Tirage;
import com.tontine.model.Tontine;
import com.tontine.model.Transaction;
import com.tontine.model.TypeTransaction;
import com.tontine.model.TypeTontine;
import com.tontine.model.Utilisateur;
import com.tontine.service.AdhesionService;
import com.tontine.service.AdminService;
import com.tontine.service.AuthService;
import com.tontine.service.CotisationService;
import com.tontine.service.CycleService;
import com.tontine.service.DemandeService;
import com.tontine.service.Libelles;
import com.tontine.service.ParametresTontine;
import com.tontine.service.PretService;
import com.tontine.service.ServiceException;
import com.tontine.service.Session;
import com.tontine.service.StatistiquesService;
import com.tontine.util.ConnexionBD;
import com.tontine.util.HashUtil;
import com.tontine.util.InitBase;

/**
 * Test de l'etape 4 : deroule un scenario complet avec les 3 types de tontines.
 * Toutes les donnees creees sont supprimees a la fin (la base retrouve son etat initial).
 * Lancer : clic droit > Run As > Java Application (MySQL demarre dans XAMPP).
 */
public class TestServices {

    private static int ok = 0;
    private static int ko = 0;
    private static int compteur = 0;
    private static final long BASE_TEL = 770000100L;

    private static final AuthService auth = new AuthService();
    private static final DemandeService demandes = new DemandeService();
    private static final AdhesionService adhesions = new AdhesionService();
    private static final CotisationService cotisations = new CotisationService();
    private static final PretService prets = new PretService();
    private static final CycleService cycles = new CycleService();
    private static final AdminService admin = new AdminService();
    private static final StatistiquesService stats = new StatistiquesService();

    private static int adminId;
    private static Utilisateur membreFondsCommun;
    private static final List<File> pieces = new ArrayList<>();
    private static final java.util.Map<Integer, File> piecesParUser = new java.util.HashMap<>();

    public static void main(String[] args) {
        System.out.println("=== TEST ETAPE 4 : SERVICES METIER ===");
        LocalDateTime debut = LocalDateTime.now().minusSeconds(2);
        try {
            InitBase.creerAdminParDefaut();
            adminId = new UtilisateurDao().findByEmail(InitBase.ADMIN_EMAIL).getId();
            testComptes();
            testDateHeureCotisation();
            testFondsCommun();
            testTourDeRole();
            testReciprocite();
            testReciprociteTirage();
            testTirageAuSort();
            testAdmin();
        } catch (Exception e) {
            ko++;
            System.out.println("[ECHEC] Exception inattendue : " + e);
            e.printStackTrace(System.out);
        } finally {
            nettoyer(debut);
        }
        System.out.println("=== RESULTAT : " + ok + " verifications reussies, " + ko + " echec(s) ===");
        if (ko == 0) {
            System.out.println("Tout est OK : vous pouvez passer a l'etape 5 (interface).");
        }
    }

    // ------------------------------------------------------------------ scenarios

    private static void testComptes() throws Exception {
        System.out.println("-- Comptes et demandes --");
        Utilisateur u = inscrire("compte");
        verifier("Inscription d'un utilisateur", u.getId() > 0);
        erreur("Email deja utilise", () -> auth.inscrire("A", "B", u.getEmail(), "779999999", "secret1"));
        erreur("Telephone deja utilise", () -> auth.inscrire("A", "B", "autre@test.tontine", u.getTelephone(), "secret1"));
        erreur("Telephone non senegalais (France)", () -> auth.inscrire("A", "B", "fr@test.tontine", "+33612345678", "secret1"));
        erreur("Telephone non senegalais (debut 61)", () -> auth.inscrire("A", "B", "fr2@test.tontine", "612345678", "secret1"));
        erreur("Telephone trop court", () -> auth.inscrire("A", "B", "court2@test.tontine", "77123456", "secret1"));
        Utilisateur sn = auth.inscrire("Sn", "Test", "sn@test.tontine", "+221 77 555 44 33", "secret1");
        verifier("Numero +221 enregistre sous forme nationale", "775554433".equals(sn.getTelephone()));
        verifier("Connexion avec l'indicatif +221", auth.seConnecter("00221 77 555 44 33", "secret1").getId() == sn.getId());
        erreur("Meme numero ecrit autrement refuse", () -> auth.inscrire("A", "B", "sn2@test.tontine", "77 555 44 33", "secret1"));
        erreur("Mot de passe trop court", () -> auth.inscrire("A", "B", "court@test.tontine", "778888888", "123"));
        erreur("Connexion avec un mauvais mot de passe", () -> auth.seConnecter(u.getEmail(), "faux"));
        verifier("Connexion par email", auth.seConnecter(u.getEmail(), "secret1").getId() == u.getId());
        verifier("Session memorisee", Session.getUtilisateur() != null && Session.getUtilisateur().getId() == u.getId());
        auth.deconnecter();
        verifier("Connexion par telephone", auth.seConnecter(u.getTelephone(), "secret1").getId() == u.getId());
        auth.deconnecter();
        admin.suspendreCompte(adminId, u.getId());
        erreur("Compte suspendu : connexion refusee", () -> auth.seConnecter(u.getEmail(), "secret1"));
        admin.reactiverCompte(adminId, u.getId());
        verifier("Compte reactive : connexion possible", auth.seConnecter(u.getEmail(), "secret1") != null);
        auth.deconnecter();
        erreur("Un non-admin ne peut pas suspendre", () -> admin.suspendreCompte(u.getId(), adminId));

        // piece d'identite pour la creation d'une tontine
        Utilisateur a = inscrire("piece-a");
        Utilisateur b = inscrire("piece-b");
        erreur("Creation sans piece d'identite (aucune enregistree)", () -> demandes.soumettreDemandeCreation(
                a.getId(), nomUnique("SansPiece"), TypeTontine.TOUR_DE_ROLE, params(TypeTontine.TOUR_DE_ROLE, 1000, Frequence.MENSUELLE, 3), null));
        File pieceA = pieceSure("piece-a");
        DemandeCreation dcA = demandes.soumettreDemandeCreation(a.getId(), nomUnique("AvecPiece"),
                TypeTontine.TOUR_DE_ROLE, params(TypeTontine.TOUR_DE_ROLE, 1000, Frequence.MENSUELLE, 3), pieceA);
        verifier("Creation avec piece : empreinte et chemin enregistres", dcA.getPieceHash() != null && dcA.getPieceChemin() != null);
        erreur("Meme piece utilisee par une autre personne (creation)", () -> demandes.soumettreDemandeCreation(
                b.getId(), nomUnique("Doublon"), TypeTontine.TOUR_DE_ROLE, params(TypeTontine.TOUR_DE_ROLE, 1000, Frequence.MENSUELLE, 3), pieceA));
        verifier("Piece deja enregistree : inutile de la renvoyer", demandes.aPieceEnregistree(a.getId())
                && !demandes.aPieceEnregistree(b.getId())
                && demandes.soumettreDemandeCreation(a.getId(), nomUnique("Reutilise"), TypeTontine.TOUR_DE_ROLE,
                        params(TypeTontine.TOUR_DE_ROLE, 1000, Frequence.MENSUELLE, 3), null).getPieceHash().equals(dcA.getPieceHash()));
        erreur("Visiteur : creation sans piece d'identite", () -> demandes.inscrireEtDemanderCreation("X", "Y",
                "visiteur-sans-piece@test.tontine", "6177700001", "secret1", nomUnique("Visiteur"),
                TypeTontine.TOUR_DE_ROLE, params(TypeTontine.TOUR_DE_ROLE, 1000, Frequence.MENSUELLE, 3), null));
        verifier("Visiteur : aucun compte cree quand la piece manque", new UtilisateurDao().findByEmail("visiteur-sans-piece@test.tontine") == null);
    }

    /** Date et heure limites de cotisation, definies par le gerant pour chaque frequence. */
    private static void testDateHeureCotisation() {
        System.out.println("-- Date et heure de cotisation --");
        java.time.LocalTime h18 = java.time.LocalTime.of(18, 0);
        Tontine t = new Tontine();
        t.setHeureCotisation(h18);
        t.setFrequence(Frequence.JOURNALIERE);
        verifier("Journalier : heure limite le jour de l'echeance",
                CotisationService.limite(t, java.time.LocalDate.of(2026, 10, 5), java.time.LocalDate.of(2026, 10, 6))
                        .equals(LocalDateTime.of(2026, 10, 6, 18, 0)));
        t.setFrequence(Frequence.HEBDOMADAIRE);
        t.setJourCotisation(5); // vendredi
        verifier("Hebdomadaire : le vendredi a 18 h",
                CotisationService.limite(t, java.time.LocalDate.of(2026, 10, 5), java.time.LocalDate.of(2026, 10, 12))
                        .equals(LocalDateTime.of(2026, 10, 9, 18, 0)));
        t.setFrequence(Frequence.MENSUELLE);
        t.setJourCotisation(5);
        verifier("Mensuel : le 5 du mois",
                CotisationService.limite(t, java.time.LocalDate.of(2026, 1, 10), java.time.LocalDate.of(2026, 2, 10))
                        .equals(LocalDateTime.of(2026, 2, 5, 18, 0)));
        verifier("Mensuel : le 5 avant le debut du cycle -> mois precedent",
                CotisationService.limite(t, java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 2, 3))
                        .equals(LocalDateTime.of(2026, 1, 5, 18, 0)));
        t.setJourCotisation(31);
        verifier("Mensuel : le 31 ramene au dernier jour de fevrier",
                CotisationService.limite(t, java.time.LocalDate.of(2026, 1, 28), java.time.LocalDate.of(2026, 2, 28))
                        .equals(LocalDateTime.of(2026, 2, 28, 18, 0)));
        t.setFrequence(Frequence.ANNUELLE);
        t.setJourCotisation(15);
        t.setMoisCotisation(3);
        verifier("Annuel : le 15 mars",
                CotisationService.limite(t, java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2027, 1, 1))
                        .equals(LocalDateTime.of(2026, 3, 15, 18, 0)));
        t.setJourCotisation(null);
        verifier("Sans jour defini : fin de periode a l'heure choisie",
                CotisationService.limite(t, java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2027, 1, 1))
                        .equals(LocalDateTime.of(2027, 1, 1, 18, 0)));
        ParametresTontine p = new ParametresTontine();
        p.setMontantCotisation(1000);
        p.setNbMembreMax(3);
        p.setDureeEnCycle(1);
        p.setFrequence(Frequence.ANNUELLE);
        p.setJourCotisation(31);
        p.setMoisCotisation(4);
        erreur("Annuel : 31 avril n'existe pas", () -> p.controler(TypeTontine.TOUR_DE_ROLE, 1));
        p.setJourCotisation(30);
        p.controler(TypeTontine.TOUR_DE_ROLE, 1);
        verifier("Annuel : 30 avril accepte", true);
        p.setFrequence(Frequence.HEBDOMADAIRE);
        p.setJourCotisation(8);
        erreur("Hebdomadaire : jour 8 refuse", () -> p.controler(TypeTontine.TOUR_DE_ROLE, 1));
        p.setFrequence(Frequence.JOURNALIERE);
        p.controler(TypeTontine.TOUR_DE_ROLE, 1);
        verifier("Journalier : jour et mois ignores", p.getJourCotisation() == null && p.getMoisCotisation() == null);
    }

    private static void testFondsCommun() throws Exception {
        System.out.println("-- Type 1 : fonds commun --");
        Utilisateur g = inscrire("fc-gerant");
        Utilisateur m1 = inscrire("fc-m1");
        membreFondsCommun = m1;
        Utilisateur m2 = inscrire("fc-m2");
        Utilisateur intrus = inscrire("fc-intrus");

        ParametresTontine p = new ParametresTontine();
        p.setDescription("Tontine de test");
        p.setMontantCotisation(1000);
        p.setMontantMaxCotisation(2000.0);
        p.setFrequence(Frequence.MENSUELLE);
        p.setDureeEnCycle(12);
        p.setPenaliteRetard(500);
        p.setNbMembreMax(3);
        p.setTauxInteret(10.0);
        p.setDureeMaxPret(30);
        p.setJourCotisation(5);
        p.setHeureCotisation(java.time.LocalTime.of(18, 0));

        // tous les parametres sont definis AVANT l'envoi de la demande a l'administrateur
        ParametresTontine mauvais = new ParametresTontine();
        mauvais.setMontantCotisation(500);
        mauvais.setMontantMaxCotisation(2000.0);
        mauvais.setFrequence(Frequence.MENSUELLE); mauvais.setDureeEnCycle(12); mauvais.setNbMembreMax(3);
        mauvais.setTauxInteret(10.0); mauvais.setDureeMaxPret(30);
        erreur("Minimum de cotisation inferieur a 1 000 refuse", () -> demandes.soumettreDemandeCreation(
                g.getId(), nomUnique("Mauvais"), TypeTontine.FONDS_COMMUN, mauvais, pieceSure("creation")));
        mauvais.setMontantCotisation(1500);
        mauvais.setMontantMaxCotisation(1000.0);
        erreur("Maximum inferieur au minimum refuse", () -> demandes.soumettreDemandeCreation(
                g.getId(), nomUnique("Mauvais"), TypeTontine.FONDS_COMMUN, mauvais, pieceSure("creation")));
        mauvais.setMontantMaxCotisation(null);
        erreur("Maximum obligatoire pour un fonds commun", () -> demandes.soumettreDemandeCreation(
                g.getId(), nomUnique("Mauvais"), TypeTontine.FONDS_COMMUN, mauvais, pieceSure("creation")));
        mauvais.setMontantMaxCotisation(2000.0);
        mauvais.setTauxInteret(null);
        erreur("Taux d'interet obligatoire pour un fonds commun", () -> demandes.soumettreDemandeCreation(
                g.getId(), nomUnique("Mauvais"), TypeTontine.FONDS_COMMUN, mauvais, pieceSure("creation")));
        mauvais.setTauxInteret(10.0);
        mauvais.setJourCotisation(40);
        erreur("Jour du mois invalide refuse", () -> demandes.soumettreDemandeCreation(
                g.getId(), nomUnique("Mauvais"), TypeTontine.FONDS_COMMUN, mauvais, pieceSure("creation")));
        mauvais.setJourCotisation(null);
        mauvais.setHeureCotisation(null);
        erreur("Heure de cotisation obligatoire", () -> demandes.soumettreDemandeCreation(
                g.getId(), nomUnique("Mauvais"), TypeTontine.FONDS_COMMUN, mauvais, pieceSure("creation")));

        DemandeCreation dc = demandes.soumettreDemandeCreation(g.getId(), nomUnique("Fonds"), TypeTontine.FONDS_COMMUN,
                p, pieceSure("creation"));
        verifier("Demande de creation en attente", dc.getStatut() == StatutDemande.EN_ATTENTE);
        verifier("Demande : tous les parametres enregistres", dc.getPenaliteRetard() == 500 && dc.getJourCotisation() == 5
                && dc.getHeureCotisation().equals(java.time.LocalTime.of(18, 0)) && dc.getMontantMax() == 2000.0);
        erreur("Nom de tontine deja demande", () -> creation(m1.getId(), dc.getNomSouhaite(),
                TypeTontine.FONDS_COMMUN, 5000, Frequence.MENSUELLE, 3));
        erreur("Seul l'admin valide une creation", () -> admin.validerDemandeCreation(g.getId(), dc.getId(), true));
        Tontine t = admin.validerDemandeCreation(adminId, dc.getId(), true);
        verifier("Tontine creee, demandeur = gerant", t != null && t.getGerantId() == g.getId());
        Tontine tCree = new TontineDao().findById(t.getId());
        verifier("Tontine creee avec les parametres de la demande (sans page Parametres)",
                tCree.getPenaliteRetard() == 500 && tCree.getTauxInteret() == 10.0 && tCree.getDureeMaxPret() == 30
                        && tCree.getDureeEnCycle() == 12 && tCree.getMontantMaxCotisation() == 2000.0
                        && "Tontine de test".equals(tCree.getDescription()));
        verifier("Date et heure de cotisation conservees", tCree.getJourCotisation() == 5
                && tCree.getHeureCotisation().equals(java.time.LocalTime.of(18, 0)));
        verifier("Horaire en clair", Libelles.horaire(tCree).equals("le 5 de chaque mois avant 18:00"));
        rejoindre(t, g, m1);
        rejoindre(t, g, m2);
        erreur("Parametres : reserve au gerant", () -> adhesions.definirParametres(m1.getId(), t.getId(), p));
        Tontine tParam = new TontineDao().findById(t.getId());

        File pieceIntrus = piecesParUser.get(m1.getId()); // piece deja utilisee par m1
        erreur("Piece d'identite deja utilisee par une autre personne",
                () -> demandes.demanderAdhesion(intrus.getId(), t.getId(), pieceIntrus));
        erreur("Tontine complete", () -> {
            Utilisateur extra = inscrire("fc-extra");
            DemandeAdhesion d = demandes.demanderAdhesion(extra.getId(), t.getId(), pieceSure("extra"));
            adhesions.validerDemandeAdhesion(g.getId(), d.getId(), true);
        });
        verifier("3 membres actifs", new com.tontine.service.ConsultationService().nombreMembres(t.getId()) == 3);

        erreur("Cotiser sans cycle en cours", () -> cotisations.payerCotisation(g.getId(), t.getId(), 1500, ModePaiement.WAVE, "770000001"));
        Cycle c = cycles.demarrerCycle(g.getId(), t.getId());
        verifier("Cycle 1 demarre", c.getNumero() == 1);
        erreur("Un seul cycle a la fois", () -> cycles.demarrerCycle(g.getId(), t.getId()));
        erreur("Non-membre ne peut pas cotiser", () -> cotisations.payerCotisation(intrus.getId(), t.getId(), 1500, ModePaiement.WAVE, "770000002"));
        erreur("Montant au-dessus de 2 000", () -> cotisations.payerCotisation(g.getId(), t.getId(), 3000, ModePaiement.WAVE, "770000001"));
        erreur("Montant au-dessous de 1 000", () -> cotisations.payerCotisation(g.getId(), t.getId(), 500, ModePaiement.WAVE, "770000001"));
        erreur("Numero Wave invalide", () -> cotisations.payerCotisation(g.getId(), t.getId(), 1500, ModePaiement.WAVE, "12"));

        cotisations.payerCotisation(g.getId(), t.getId(), 2000, ModePaiement.WAVE, "770000001");
        erreur("Une seule cotisation par periode (fonds commun)", () -> cotisations.payerCotisation(g.getId(), t.getId(), 2000, ModePaiement.WAVE, "770000001"));
        Transaction esp = cotisations.payerCotisation(m1.getId(), t.getId(), 1500, ModePaiement.ESPECE, null);
        verifier("Espece : paiement en attente", esp.getStatut().name().equals("EN_ATTENTE"));
        cotisations.payerCotisation(m2.getId(), t.getId(), 1000, ModePaiement.ORANGE_MONEY, "770000003");
        verifier("Fonds avant confirmation espece = 3 000", prets.fondsDisponible(t.getId()) == 3000);
        erreur("Un membre ne confirme pas les especes", () -> cotisations.confirmerPaiementEspece(m2.getId(), esp.getId(), true));
        erreur("Cloture impossible avec un paiement en attente", () -> cycles.cloturerCycle(g.getId(), t.getId()));
        cotisations.confirmerPaiementEspece(g.getId(), esp.getId(), true);
        verifier("Fonds apres confirmation = 4 500", prets.fondsDisponible(t.getId()) == 4500);

        erreur("Pret refuse : trop eleve", () -> prets.demanderPret(m1.getId(), t.getId(), 99999, 10, "x"));
        erreur("Pret refuse : duree trop longue", () -> prets.demanderPret(m1.getId(), t.getId(), 1000, 31, "x"));
        Pret p1 = prets.demanderPret(m1.getId(), t.getId(), 2000, 20, "Commerce");
        erreur("Un seul pret a la fois", () -> prets.demanderPret(m1.getId(), t.getId(), 1000, 10, "x"));
        erreur("Un membre ne valide pas un pret", () -> prets.validerPret(m2.getId(), p1.getId(), true));
        prets.validerPret(g.getId(), p1.getId(), true);
        verifier("Pret de 2 000 accorde, fonds = 2 500", prets.fondsDisponible(t.getId()) == 2500);

        Pret p2 = prets.demanderPret(g.getId(), t.getId(), 1000, 10, "Urgence");
        erreur("Le gerant ne valide jamais son propre pret", () -> prets.validerPret(g.getId(), p2.getId(), true));
        verifier("Le pret du gerant est dans la file de l'admin", admin.pretsGerantsEnAttente(adminId).size() == 1);
        admin.validerPretGerant(adminId, p2.getId(), true);
        verifier("Les prets sont visibles de tous les membres", prets.listerPrets(m2.getId(), t.getId()).size() == 2);
        erreur("Non-membre ne voit pas les prets", () -> prets.listerPrets(intrus.getId(), t.getId()));
        verifier("Interet simple 10 % de 2 000 = 200", prets.interets(p1, tParam) == 200.0 && prets.totalDu(p1, tParam) == 2200.0);

        erreur("Cloture impossible avec des prets non rembourses", () -> cycles.cloturerCycle(g.getId(), t.getId()));
        erreur("Remboursement superieur au reste du", () -> prets.rembourser(m1.getId(), p1.getId(), 7000, ModePaiement.WAVE, "770000004"));
        prets.rembourser(m1.getId(), p1.getId(), 1000, ModePaiement.WAVE, "770000004");
        prets.rembourser(m1.getId(), p1.getId(), 1200, ModePaiement.WAVE, "770000004");
        // le pret du gerant (10 jours) est decaisse "il y a 20 jours" : son remboursement sera en retard
        TransactionDao txDao = new TransactionDao();
        for (Transaction tx : txDao.findByTontine(t.getId())) {
            if (tx.getType() == TypeTransaction.PRET && ("PRET-" + p2.getId()).equals(tx.getReference())) {
                tx.setDate(LocalDateTime.now().minusDays(20));
                txDao.update(tx);
            }
        }
        prets.rembourser(g.getId(), p2.getId(), 1100, ModePaiement.ORANGE_MONEY, "770000001");
        verifier("Remboursement en retard : penalite de 500 appliquee", txDao.totalParType(t.getId(), TypeTransaction.PENALITE) == 500);
        verifier("Prets rembourses, fonds = 4 800 (la penalite est retiree du solde du gerant, pas payee en plus)", prets.fondsDisponible(t.getId()) == 4800);
        erreur("Pret deja rembourse", () -> prets.rembourser(m1.getId(), p1.getId(), 10, ModePaiement.WAVE, "770000004"));

        verifier("Historique complet visible par un membre", cotisations.historiqueTontine(m2.getId(), t.getId()).size() == 9);
        erreur("Historique interdit a un non-membre", () -> cotisations.historiqueTontine(intrus.getId(), t.getId()));
        verifier("Activite membre", stats.activiteMembre(m1.getId(), t.getId()).getValeurs().size() >= 5);
        verifier("Tableau de bord tontine", stats.tontine(g.getId(), t.getId()).getValeurs().size() >= 6);

        // le cycle (12 mois) n'est pas termine : cloture impossible avant sa date de fin
        erreur("Cloture impossible avant la fin du cycle", () -> cycles.cloturerCycle(g.getId(), t.getId()));
        // on recule le debut du cycle de 12 mois : le cycle est maintenant termine
        CycleDao cycleDao = new CycleDao();
        c.setDateDebut(LocalDate.now().minusMonths(12));
        cycleDao.update(c);
        List<Distribution> versements = cycles.cloturerCycle(g.getId(), t.getId());
        double somme = 0;
        for (Distribution d : versements) {
            somme += d.getMontantTotal();
        }
        verifier("3 versements en fin de cycle", versements.size() == 3);
        verifier("Total verse = 4 800 (cotisations 4 500 + interets 300, penalite redistribuee)", Math.abs(somme - 4800) < 0.005);
        verifier("Fonds vide apres cloture", Math.abs(prets.fondsDisponible(t.getId())) < 0.005);
        verifier("Soldes remis a zero", stats.activiteMembre(m1.getId(), t.getId()).getValeurs().get("Mon solde dans la tontine").startsWith("0"));
        // le gerant change l'intervalle entre deux cycles : 1 500 a 3 000
        p.setMontantCotisation(1500);
        p.setMontantMaxCotisation(3000.0);
        adhesions.definirParametres(g.getId(), t.getId(), p);
        Tontine tNouv = new TontineDao().findById(t.getId());
        verifier("Intervalle modifie par le gerant : 1 500 a 3 000", tNouv.getMontantCotisation() == 1500 && tNouv.getMontantMaxCotisation() == 3000);
        Cycle c2 = cycles.demarrerCycle(g.getId(), t.getId());
        verifier("Nouveau cycle possible (cycle 2)", c2.getNumero() == 2);

        // ---- frequence journaliere : la periode est le jour
        Tontine tJour = new Tontine();
        tJour.setFrequence(Frequence.JOURNALIERE);
        tJour.setDureeEnCycle(30);
        Cycle cJour = new Cycle();
        cJour.setDateDebut(LocalDate.of(2026, 1, 1));
        verifier("Journaliere : un cycle de 30 periodes dure 30 jours", CotisationService.finDeCycle(cJour, tJour).equals(LocalDate.of(2026, 1, 31)));
        verifier("Journaliere : libelles", com.tontine.service.Libelles.frequence(Frequence.JOURNALIERE).startsWith("Journali")
                && com.tontine.service.Libelles.periode(Frequence.JOURNALIERE).equals("jour"));

        // ---- cycle 2 : cotisation en retard, penalite, partage des penalites a la fin du cycle
        verifier("Cycle de 12 periodes : fin prevue dans 12 mois", CotisationService.finDeCycle(c2, tParam).equals(LocalDate.now().plusMonths(12)));
        verifier("A l'heure : pas de penalite prevue", !cotisations.etatCotisation(g.getId(), t.getId()).enRetard);
        try (Connection cn = ConnexionBD.getConnection(); Statement st = cn.createStatement()) { // membres presents depuis le debut du cycle
            st.executeUpdate("UPDATE adhesion SET date_adhesion = NOW() - INTERVAL 2 YEAR WHERE tontine_id = " + t.getId());
        }
        c2.setDateDebut(LocalDate.now().minusDays(40)); // la periode 1 s'est terminee il y a une dizaine de jours
        cycleDao.update(c2);
        com.tontine.service.EtatCotisation etG = cotisations.etatCotisation(g.getId(), t.getId());
        verifier("Etat : cotisation 1/12 en retard, penalite 500", etG.rang == 1 && etG.attendues == 12 && etG.enRetard && etG.penalite == 500);
        erreur("Nouvel intervalle : 1 000 est sous le nouveau minimum", () -> cotisations.payerCotisation(g.getId(), t.getId(), 1000, ModePaiement.WAVE, "770000001"));
        erreur("Nouvel intervalle : 3 500 depasse le nouveau maximum", () -> cotisations.payerCotisation(g.getId(), t.getId(), 3500, ModePaiement.WAVE, "770000001"));
        double penAvant = txDao.totalParType(t.getId(), TypeTransaction.PENALITE);
        cotisations.payerCotisation(g.getId(), t.getId(), 1500, ModePaiement.WAVE, "770000001");
        verifier("Cotisation payee en retard : penalite de 500 enregistree", txDao.totalParType(t.getId(), TypeTransaction.PENALITE) == penAvant + 500);
        verifier("Cotisation en retard : solde = 1 500 - 500 de penalite = 1 000 (aucun paiement en plus)", new com.tontine.dao.AdhesionDao().findByTontineEtUser(t.getId(), g.getId()).getSolde() == 1000);
        cotisations.payerCotisation(m1.getId(), t.getId(), 2500, ModePaiement.ORANGE_MONEY, "770000003");
        verifier("2e cotisation du meme mois autorisee (rattrapage, periode 2 commencee)", cotisations.etatCotisation(g.getId(), t.getId()).peutCotiser);
        verifier("m2 n'a pas cotise : signale au gerant", cotisations.membresSansCotisation(t.getId()).size() >= 1);
        c2.setDateDebut(LocalDate.now().minusMonths(12));
        cycleDao.update(c2);
        double potAvant = prets.fondsDisponible(t.getId());
        List<Distribution> v2c = cycles.cloturerCycle(g.getId(), t.getId());
        double s2 = 0;
        for (Distribution d : v2c) {
            s2 += d.getMontantTotal();
        }
        verifier("Fin de cycle 2 : tout le fonds (cotisations + penalites) est reparti entre les membres", Math.abs(s2 - potAvant) < 0.005 && potAvant == 4000);
        verifier("Penalites partagees : m2 (qui n'avait rien cotise) recoit une part", v2c.stream().anyMatch(d -> d.getBeneficiaireId() == m2.getId() && d.getMontantTotal() > 0));
        verifier("Fonds vide apres la cloture du cycle 2", Math.abs(prets.fondsDisponible(t.getId())) < 0.005);
    }

    private static void testTourDeRole() throws Exception {
        System.out.println("-- Type 2 : tour de role --");
        Utilisateur h = inscrire("tr-gerant");
        Utilisateur n1 = inscrire("tr-n1");
        Utilisateur n2 = inscrire("tr-n2");
        DemandeCreation dc = creation(h.getId(), nomUnique("Tour"), TypeTontine.TOUR_DE_ROLE,
                3000, Frequence.MENSUELLE, 3);
        Tontine t = admin.validerDemandeCreation(adminId, dc.getId(), true);
        ParametresTontine p = new ParametresTontine();
        p.setDescription("Tour de role");
        p.setMontantCotisation(3000);
        p.setFrequence(Frequence.MENSUELLE);
        p.setDureeEnCycle(3);
        p.setPenaliteRetard(200);
        p.setNbMembreMax(3);
        adhesions.definirParametres(h.getId(), t.getId(), p);
        p.setMontantCotisation(3500);
        adhesions.definirParametres(h.getId(), t.getId(), p);
        verifier("Tour de role : le gerant definit le montant a cotiser (3 500)", new TontineDao().findById(t.getId()).getMontantCotisation() == 3500
                && new TontineDao().findById(t.getId()).getMontantMaxCotisation() == null);
        p.setMontantCotisation(0);
        erreur("Tour de role : montant nul refuse", () -> adhesions.definirParametres(h.getId(), t.getId(), p));
        p.setMontantCotisation(3000);
        adhesions.definirParametres(h.getId(), t.getId(), p);
        rejoindre(t, h, n1);
        rejoindre(t, h, n2);
        try {
            DemandeAdhesion d2 = demandes.demanderAdhesion(membreFondsCommun.getId(), t.getId(), piecesParUser.get(membreFondsCommun.getId()));
            verifier("Meme personne, meme piece d'identite pour une 2e tontine", d2.getId() > 0);
        } catch (com.tontine.util.DaoException e) {
            ko++;
            System.out.println("  [ECHEC] Meme piece pour une 2e tontine refusee par la base : "
                    + "executez d'abord patch_etape4.sql dans phpMyAdmin. (" + e.getMessage() + ")");
        }
        try {
            Utilisateur n3 = inscrire("tr-n3");
            DemandeAdhesion premiere = demandes.demanderAdhesion(n3.getId(), t.getId(), pieceSure("n3"));
            adhesions.validerDemandeAdhesion(h.getId(), premiere.getId(), false); // refusee : il pourra redemander ailleurs
            Tontine autre = admin.validerDemandeCreation(adminId, creation(n3.getId(), nomUnique("Autre"),
                    TypeTontine.TOUR_DE_ROLE, 1000, Frequence.MENSUELLE, 3).getId(), true);
            DemandeAdhesion sansPiece = demandes.demanderAdhesion(n3.getId(), t.getId(), null);
            verifier("2e demande sans renvoyer la piece : la piece enregistree est reutilisee",
                    sansPiece.getPieceHash().equals(premiere.getPieceHash()) && autre != null);
        } catch (RuntimeException e) {
            ko++;
            System.out.println("  [ECHEC] Reutilisation de la piece enregistree : " + e.getMessage());
        }
        verifier("Prochain beneficiaire = gerant (ordre 1)", cycles.prochainBeneficiaire(t.getId()).getId() == h.getId());
        erreur("Pret interdit hors fonds commun", () -> prets.demanderPret(n1.getId(), t.getId(), 1000, 5, "x"));
        erreur("Tirage interdit hors tirage au sort", () -> cycles.lancerTirage(h.getId(), t.getId()));

        Cycle c1 = cycles.demarrerCycle(h.getId(), t.getId());
        // on recule le debut du cycle pour tester la penalite de retard (echeance depassee)
        c1.setDateDebut(LocalDate.now().minusDays(40));
        new CycleDao().update(c1);
        erreur("Montant fixe : refus d'un autre montant", () -> cotisations.payerCotisation(h.getId(), t.getId(), 4000, ModePaiement.WAVE, "770000010"));
        cotisations.payerCotisation(h.getId(), t.getId(), 3000, ModePaiement.WAVE, "770000010");
        erreur("Une seule cotisation par cycle", () -> cotisations.payerCotisation(h.getId(), t.getId(), 3000, ModePaiement.WAVE, "770000010"));
        cotisations.payerCotisation(n1.getId(), t.getId(), 3000, ModePaiement.WAVE, "770000011");
        verifier("Un membre n'a pas encore cotise", cotisations.membresSansCotisation(t.getId()).size() == 1);
        cotisations.enregistrerCotisationEspece(h.getId(), t.getId(), n2.getId(), 3000);
        erreur("Le gerant ne s'enregistre pas ses propres especes", () -> cotisations.enregistrerCotisationEspece(h.getId(), t.getId(), h.getId(), 3000));
        List<Distribution> v1 = cycles.cloturerCycle(h.getId(), t.getId());
        verifier("Cycle 1 : le gerant recoit 9 000 + 3 penalites de 200 - ses propres 200 deduits = 9 400",
                v1.size() == 1 && v1.get(0).getBeneficiaireId() == h.getId() && Math.abs(v1.get(0).getMontantTotal() - 9400) < 0.005);
        verifier("Penalites retirees du solde : n1 et n2 doivent chacun 200 (solde -200)",
                new com.tontine.dao.AdhesionDao().findByTontineEtUser(t.getId(), n1.getId()).getSolde() == -200
                && new com.tontine.dao.AdhesionDao().findByTontineEtUser(t.getId(), n2.getId()).getSolde() == -200
                && new com.tontine.dao.AdhesionDao().findByTontineEtUser(t.getId(), h.getId()).getSolde() == 0);

        DemandeTourAnticipe d = cycles.demanderTourAnticipe(n2.getId(), t.getId(), "Besoin urgent");
        erreur("Une seule demande de tour anticipe en attente", () -> cycles.demanderTourAnticipe(n2.getId(), t.getId(), "encore"));
        cycles.traiterDemandeTourAnticipe(h.getId(), d.getId(), true);
        verifier("Tour anticipe : n2 passe avant n1", cycles.prochainBeneficiaire(t.getId()).getId() == n2.getId());

        cycles.demarrerCycle(h.getId(), t.getId());
        cotisations.payerCotisation(h.getId(), t.getId(), 3000, ModePaiement.WAVE, "770000010");
        cotisations.payerCotisation(n1.getId(), t.getId(), 3000, ModePaiement.WAVE, "770000011");
        cotisations.payerCotisation(n2.getId(), t.getId(), 3000, ModePaiement.ORANGE_MONEY, "770000012");
        List<Distribution> v2 = cycles.cloturerCycle(h.getId(), t.getId());
        verifier("Cycle 2 : n2 recoit 9 000 - 200 de penalite due = 8 800", v2.get(0).getBeneficiaireId() == n2.getId() && Math.abs(v2.get(0).getMontantTotal() - 8800) < 0.005);

        cycles.demarrerCycle(h.getId(), t.getId());
        cotisations.payerCotisation(h.getId(), t.getId(), 3000, ModePaiement.WAVE, "770000010");
        cotisations.payerCotisation(n1.getId(), t.getId(), 3000, ModePaiement.WAVE, "770000011");
        cotisations.payerCotisation(n2.getId(), t.getId(), 3000, ModePaiement.ORANGE_MONEY, "770000012");
        List<Distribution> v3 = cycles.cloturerCycle(h.getId(), t.getId());
        verifier("Cycle 3 : n1 recoit 9 000 - 200 de penalite due = 8 800", v3.get(0).getBeneficiaireId() == n1.getId() && Math.abs(v3.get(0).getMontantTotal() - 8800) < 0.005);
        verifier("Fin du tour : l'ordre recommence", new TontineDao().findById(t.getId()).getTourActuelIndex() == 0);
        verifier("Le beneficiaire de depart redevient le gerant", cycles.prochainBeneficiaire(t.getId()).getId() == h.getId());
        verifier("Soldes remis a zero apres les versements",
                stats.activiteMembre(n1.getId(), t.getId()).getValeurs().get("Mon solde dans la tontine").startsWith("0"));
    }

    private static void testReciprocite() throws Exception {
        System.out.println("-- Tour de role : reciprocite des cotisations --");
        Utilisateur x = inscrire("rc-x");
        Utilisateur y = inscrire("rc-y");
        Utilisateur z = inscrire("rc-z");
        Tontine t = admin.validerDemandeCreation(adminId, creation(x.getId(), nomUnique("Reciproque"),
                TypeTontine.TOUR_DE_ROLE, 2000, Frequence.MENSUELLE, 3).getId(), true);
        ParametresTontine p = new ParametresTontine();
        p.setDescription("Reciprocite");
        p.setMontantCotisation(2000);
        p.setFrequence(Frequence.MENSUELLE);
        p.setDureeEnCycle(3);
        p.setPenaliteRetard(500);
        p.setNbMembreMax(3);
        adhesions.definirParametres(x.getId(), t.getId(), p);
        rejoindre(t, x, y);
        rejoindre(t, x, z);
        TransactionDao txDao = new TransactionDao();

        // cycle 1 : beneficiaire x ; x et y cotisent, z ne cotise PAS
        cycles.demarrerCycle(x.getId(), t.getId());
        verifier("Cycle 1 : aucune dispense (premier tour)", cotisations.etatTour(z.getId(), t.getId()).exemption == null);
        cotisations.payerCotisation(x.getId(), t.getId(), 2000, ModePaiement.WAVE, "770000010");
        cotisations.payerCotisation(y.getId(), t.getId(), 2000, ModePaiement.WAVE, "770000011");
        List<Distribution> v1 = cycles.cloturerCycle(x.getId(), t.getId());
        verifier("Cycle 1 : x recoit 4 000 + 500 de penalite de z = 4 500", v1.get(0).getBeneficiaireId() == x.getId() && Math.abs(v1.get(0).getMontantTotal() - 4500) < 0.005);
        int dettes = 0;
        for (Transaction tx : txDao.findByTontine(t.getId())) {
            if (tx.getType() == TypeTransaction.PENALITE && tx.getStatut().name().equals("CONFIRMEE")
                    && tx.getUserId() == z.getId() && tx.getMontant() == 500) {
                dettes++;
            }
        }
        verifier("z qui devait cotiser sans le faire : penalite de 500 retiree de son solde (-500)", dettes == 1
                && new com.tontine.dao.AdhesionDao().findByTontineEtUser(t.getId(), z.getId()).getSolde() == -500);
        verifier("La penalite impayee n'apparait pas comme un paiement en especes a confirmer", cotisations.paiementsEnAttente(x.getId(), t.getId()).isEmpty());

        // cycle 2 : beneficiaire y ; x (y a cotise pour lui) et z (premier tour) doivent cotiser
        cycles.demarrerCycle(x.getId(), t.getId());
        verifier("Cycle 2 : x doit cotiser pour y (y a cotise pour x)", cotisations.etatTour(x.getId(), t.getId()).exemption == null);
        com.tontine.service.EtatCotisation ez = cotisations.etatTour(z.getId(), t.getId());
        verifier("Cycle 2 : z garde un solde de -500 et cotise normalement (rien a payer en plus)", ez.arrieres == 0 && ez.penalite == 0);
        cotisations.payerCotisation(x.getId(), t.getId(), 2000, ModePaiement.WAVE, "770000010");
        cotisations.payerCotisation(y.getId(), t.getId(), 2000, ModePaiement.WAVE, "770000011");
        cotisations.payerCotisation(z.getId(), t.getId(), 2000, ModePaiement.ORANGE_MONEY, "770000012");
        List<Distribution> v2 = cycles.cloturerCycle(x.getId(), t.getId());
        verifier("z reste a -500 (recupere a son tour)", new com.tontine.dao.AdhesionDao().findByTontineEtUser(t.getId(), z.getId()).getSolde() == -500);
        verifier("Cycle 2 : y recoit 6 000", v2.get(0).getBeneficiaireId() == y.getId() && Math.abs(v2.get(0).getMontantTotal() - 6000) < 0.005);

        // cycle 3 : beneficiaire z ; z n'avait pas cotise pour x au cycle 1 => x est dispense ; y doit cotiser
        cycles.demarrerCycle(x.getId(), t.getId());
        String motif = cotisations.etatTour(x.getId(), t.getId()).exemption;
        verifier("Cycle 3 : x est dispense (z n'a pas cotise pour lui)", motif != null);
        verifier("Cycle 3 : y doit cotiser (z a cotise pour lui)", cotisations.etatTour(y.getId(), t.getId()).exemption == null);
        erreur("Cycle 3 : x ne peut pas cotiser pour z", () -> cotisations.payerCotisation(x.getId(), t.getId(), 2000, ModePaiement.WAVE, "770000010"));
        verifier("Cycle 3 : le membre dispense n'est pas signale comme manquant", cotisations.membresSansCotisation(t.getId()).size() == 2);
        cotisations.payerCotisation(y.getId(), t.getId(), 2000, ModePaiement.WAVE, "770000011");
        cotisations.payerCotisation(z.getId(), t.getId(), 2000, ModePaiement.ORANGE_MONEY, "770000012");
        List<Distribution> v3 = cycles.cloturerCycle(x.getId(), t.getId());
        verifier("Cycle 3 : z recoit 4 000 - 500 de penalite due = 3 500 (x dispense)", v3.get(0).getBeneficiaireId() == z.getId() && Math.abs(v3.get(0).getMontantTotal() - 3500) < 0.005);
        int dettesX = 0;
        for (Transaction tx : txDao.findByTontine(t.getId())) {
            if (tx.getUserId() == x.getId() && tx.getType() == TypeTransaction.PENALITE) {
                dettesX++;
            }
        }
        verifier("Le membre dispense ne subit aucune penalite", dettesX == 0);
    }

    private static void testReciprociteTirage() throws Exception {
        System.out.println("-- Tirage au sort : reciprocite des cotisations --");
        Utilisateur a = inscrire("rt-a");
        Utilisateur b = inscrire("rt-b");
        Utilisateur c3 = inscrire("rt-c");
        Tontine t = admin.validerDemandeCreation(adminId, creation(a.getId(), nomUnique("ReciTirage"),
                TypeTontine.TIRAGE_AU_SORT, 2000, Frequence.MENSUELLE, 3).getId(), true);
        ParametresTontine p = new ParametresTontine();
        p.setDescription("Reciprocite tirage");
        p.setMontantCotisation(2000);
        p.setFrequence(Frequence.MENSUELLE);
        p.setDureeEnCycle(3);
        p.setPenaliteRetard(500);
        p.setNbMembreMax(3);
        adhesions.definirParametres(a.getId(), t.getId(), p);
        rejoindre(t, a, b);
        rejoindre(t, a, c3);
        List<Utilisateur> tous = new ArrayList<>();
        tous.add(a); tous.add(b); tous.add(c3);

        // cycle 1 : tirage, puis tous cotisent SAUF un membre (qui n'est pas le beneficiaire)
        cycles.demarrerCycle(a.getId(), t.getId());
        Tirage t1 = cycles.lancerTirage(a.getId(), t.getId());
        Utilisateur b1 = null, defaillant = null, payeur = null;
        for (Utilisateur u : tous) {
            if (u.getId() == t1.getBeneficiaireId()) {
                b1 = u;
            } else if (defaillant == null) {
                defaillant = u;
            } else {
                payeur = u;
            }
        }
        verifier("Tirage : le beneficiaire est connu avant les cotisations", cotisations.etatTour(payeur.getId(), t.getId()).beneficiaire != null);
        cotisations.payerCotisation(b1.getId(), t.getId(), 2000, ModePaiement.WAVE, "770000030");
        cotisations.payerCotisation(payeur.getId(), t.getId(), 2000, ModePaiement.WAVE, "770000031");
        List<Distribution> v1 = cycles.cloturerCycle(a.getId(), t.getId());
        verifier("Cycle 1 : le gagnant recoit 4 000 + 500 de penalite du membre defaillant", v1.get(0).getBeneficiaireId() == b1.getId() && Math.abs(v1.get(0).getMontantTotal() - 4500) < 0.005);

        // cycle 2 : tirage parmi les deux autres
        cycles.demarrerCycle(a.getId(), t.getId());
        verifier("Cycle 2 : la penalite de non-cotisation (500) est deja retiree du solde du defaillant", new com.tontine.dao.AdhesionDao().findByTontineEtUser(t.getId(), defaillant.getId()).getSolde() == -500);
        Tirage t2 = cycles.lancerTirage(a.getId(), t.getId());
        boolean b2EstDefaillant = t2.getBeneficiaireId() == defaillant.getId();
        // b1 (deja gagnant) ne doit cotiser pour le nouveau gagnant que si celui-ci avait cotise pour lui au cycle 1
        boolean b1Dispense = cotisations.etatTour(b1.getId(), t.getId()).exemption != null;
        verifier("Cycle 2 : le gagnant du cycle 1 est dispense si et seulement si le nouveau gagnant n'avait pas cotise pour lui", b1Dispense == b2EstDefaillant);
        if (b1Dispense) {
            final Utilisateur fb1 = b1;
            erreur("Cycle 2 : le membre dispense ne peut pas cotiser", () -> cotisations.payerCotisation(fb1.getId(), t.getId(), 2000, ModePaiement.WAVE, "770000030"));
        } else {
            cotisations.payerCotisation(b1.getId(), t.getId(), 2000, ModePaiement.WAVE, "770000030");
        }
        cotisations.payerCotisation(defaillant.getId(), t.getId(), 2000 , ModePaiement.WAVE, "770000032");
        verifier("Cycle 2 : aucune penalite supplementaire a payer", cotisations.etatTour(defaillant.getId(), t.getId()).arrieres == 0);
    }

    private static void testTirageAuSort() throws Exception {
        System.out.println("-- Type 3 : tirage au sort --");
        Utilisateur k = inscrire("ts-gerant");
        Utilisateur p1 = inscrire("ts-p1");
        Utilisateur p2 = inscrire("ts-p2");
        DemandeCreation dc = creation(k.getId(), nomUnique("Tirage"), TypeTontine.TIRAGE_AU_SORT,
                2000, Frequence.HEBDOMADAIRE, 3);
        Tontine t = admin.validerDemandeCreation(adminId, dc.getId(), true);
        rejoindre(t, k, p1);
        rejoindre(t, k, p2);

        Set<Integer> gagnants = new HashSet<>();
        for (int cycle = 1; cycle <= 3; cycle++) {
            cycles.demarrerCycle(k.getId(), t.getId());
            erreur("Cloture impossible sans tirage (cycle " + cycle + ")", () -> cycles.cloturerCycle(k.getId(), t.getId()));
            erreur("Tirage au sort : cotisation impossible avant le tirage (cycle " + cycle + ")",
                    () -> cotisations.payerCotisation(k.getId(), t.getId(), 2000, ModePaiement.WAVE, "770000020"));
            Tirage ti = cycles.lancerTirage(k.getId(), t.getId());
            cotisations.payerCotisation(k.getId(), t.getId(), 2000, ModePaiement.WAVE, "770000020");
            cotisations.payerCotisation(p1.getId(), t.getId(), 2000, ModePaiement.WAVE, "770000021");
            cotisations.payerCotisation(p2.getId(), t.getId(), 2000, ModePaiement.ORANGE_MONEY, "770000022");
            erreur("Un seul tirage par cycle (cycle " + cycle + ")", () -> cycles.lancerTirage(k.getId(), t.getId()));
            gagnants.add(ti.getBeneficiaireId());
            List<Distribution> v = cycles.cloturerCycle(k.getId(), t.getId());
            verifier("Cycle " + cycle + " : le gagnant recoit 6 000",
                    v.get(0).getBeneficiaireId() == ti.getBeneficiaireId() && Math.abs(v.get(0).getMontantTotal() - 6000) < 0.005);
        }
        verifier("Les 3 membres ont ete tires chacun une fois", gagnants.size() == 3);
        verifier("Historique des tirages", cycles.listerTirages(k.getId(), t.getId()).size() == 3);

        cycles.demarrerCycle(k.getId(), t.getId());
        Tirage nouveau = cycles.lancerTirage(k.getId(), t.getId());
        verifier("Nouveau tour : tirage de nouveau possible", nouveau.getBeneficiaireId() > 0);
        int autre = nouveau.getBeneficiaireId() == p1.getId() ? p2.getId() : p1.getId();
        erreur("Correction d'un tirage : reservee a l'admin", () -> admin.corrigerTirage(k.getId(), nouveau.getId(), autre, "x"));
        erreur("Correction d'un tirage : motif obligatoire", () -> admin.corrigerTirage(adminId, nouveau.getId(), autre, " "));
        admin.corrigerTirage(adminId, nouveau.getId(), autre, "Erreur de saisie");
        verifier("Tirage corrige et trace dans le journal d'audit",
                admin.consulterJournalAudit(adminId, 50).stream().anyMatch(j -> "CORRECTION_TIRAGE".equals(j.getAction())));
    }

    private static void testAdmin() {
        System.out.println("-- Administrateur --");
        verifier("Statistiques globales", admin.consulterJournalAudit(adminId, 5).size() > 0
                && stats.globales(adminId).getValeurs().size() >= 6);
        verifier("Liste des versements", admin.listerVersements(adminId).size() >= 7);
        erreur("Statistiques globales : admin uniquement", () -> {
            Utilisateur u = new UtilisateurDao().findByEmail("compte-" + 1 + "@test.tontine");
            stats.globales(u == null ? -1 : u.getId());
        });
    }

    // ------------------------------------------------------------------ outils de test

    private static Utilisateur inscrire(String prefixe) {
        compteur++;
        return auth.inscrire("Test", prefixe, prefixe + "-" + compteur + "@test.tontine", String.valueOf(BASE_TEL + compteur), "secret1");
    }

    private static DemandeCreation creation(int userId, String nom, TypeTontine type, double montant, Frequence f, int nb) {
        return demandes.soumettreDemandeCreation(userId, nom, type, params(type, montant, f, nb), pieceSure("creation"));
    }

    /** Parametres complets d'une demande de creation (tout est defini avant l'envoi a l'administrateur). */
    private static ParametresTontine params(TypeTontine type, double montant, Frequence f, int nb) {
        ParametresTontine p = new ParametresTontine();
        boolean fc = type == TypeTontine.FONDS_COMMUN;
        p.setMontantCotisation(fc ? Math.max(montant, 1000) : montant);
        p.setMontantMaxCotisation(fc ? p.getMontantCotisation() * 5 : null);
        p.setFrequence(f);
        p.setDureeEnCycle(nb);
        p.setNbMembreMax(nb);
        if (fc) {
            p.setTauxInteret(10.0);
            p.setDureeMaxPret(30);
        }
        return p;
    }

    private static String nomUnique(String base) {
        return "TEST " + base + " " + System.nanoTime();
    }

    private static File pieceSure(String nom) {
        try {
            return nouvellePiece(nom);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static File nouvellePiece(String nom) throws Exception {
        File f = File.createTempFile("piece-" + nom, ".png");
        Files.write(f.toPath(), ("piece-" + nom + "-" + System.nanoTime()).getBytes(StandardCharsets.UTF_8));
        f.deleteOnExit();
        pieces.add(f);
        return f;
    }

    /** Un membre s'inscrit, demande a rejoindre la tontine, et le gerant accepte. */
    private static void rejoindre(Tontine t, Utilisateur gerant, Utilisateur membre) throws Exception {
        File pieceMembre = nouvellePiece(membre.getPrenom());
        piecesParUser.put(membre.getId(), pieceMembre);
        DemandeAdhesion d = demandes.demanderAdhesion(membre.getId(), t.getId(), pieceMembre);
        verifier(membre.getPrenom() + " : demande en attente chez le gerant",
                adhesions.listerDemandes(gerant.getId(), t.getId(), StatutDemande.EN_ATTENTE).size() >= 1);
        erreur("Seul le gerant valide une adhesion", () -> adhesions.validerDemandeAdhesion(membre.getId(), d.getId(), true));
        verifier(membre.getPrenom() + " devient membre", adhesions.validerDemandeAdhesion(gerant.getId(), d.getId(), true) != null);
    }

    private static void verifier(String libelle, boolean condition) {
        if (condition) {
            ok++;
            System.out.println("  [OK] " + libelle);
        } else {
            ko++;
            System.out.println("  [ECHEC] " + libelle);
        }
    }

    /** Verifie qu'une action est bien REFUSEE par une regle metier. */
    private static void erreur(String libelle, Runnable action) {
        try {
            action.run();
            ko++;
            System.out.println("  [ECHEC] " + libelle + " (aurait du etre refuse)");
        } catch (ServiceException e) {
            ok++;
            System.out.println("  [OK] " + libelle + " -> " + e.getMessage());
        } catch (RuntimeException e) {
            ko++;
            System.out.println("  [ECHEC] " + libelle + " -> erreur technique : " + e);
        }
    }

    // ------------------------------------------------------------------ nettoyage

    /** Supprime toutes les donnees creees par ce test (comptes @test.tontine et leurs tontines). */
    private static void nettoyer(LocalDateTime debut) {
        String users = "(SELECT id FROM (SELECT id FROM utilisateur WHERE email LIKE '%@test.tontine') x)";
        String tontines = "(SELECT id FROM (SELECT id FROM tontine WHERE gerant_id IN " + users + ") x)";
        String cycles = "(SELECT id FROM (SELECT id FROM cycle WHERE tontine_id IN " + tontines + ") x)";
        String[] sql = {
            "DELETE FROM notification WHERE user_id IN " + users + " OR date_envoi >= '" + debut + "'",
            "DELETE FROM journal_audit WHERE user_id IN " + users + " OR date >= '" + debut + "'",
            "DELETE FROM `transaction` WHERE tontine_id IN " + tontines + " OR user_id IN " + users,
            "DELETE FROM distribution WHERE cycle_id IN " + cycles,
            "DELETE FROM tirage WHERE tontine_id IN " + tontines,
            "DELETE FROM pret WHERE tontine_id IN " + tontines,
            "DELETE FROM demande_tour_anticipe WHERE tontine_id IN " + tontines + " OR user_id IN " + users,
            "DELETE FROM demande_adhesion WHERE tontine_id IN " + tontines + " OR user_id IN " + users,
            "DELETE FROM demande_creation WHERE demandeur_id IN " + users,
            "DELETE FROM cycle WHERE tontine_id IN " + tontines,
            "DELETE FROM adhesion WHERE tontine_id IN " + tontines + " OR user_id IN " + users,
            "DELETE FROM tontine WHERE gerant_id IN " + users,
            "DELETE FROM utilisateur WHERE email LIKE '%@test.tontine'"
        };
        try (Connection c = ConnexionBD.getConnection(); Statement st = c.createStatement()) {
            for (String s : sql) {
                st.executeUpdate(s);
            }
            System.out.println("-- Donnees de test supprimees. --");
        } catch (Exception e) {
            System.out.println("[ATTENTION] Nettoyage incomplet : " + e.getMessage());
        }
        // pieces d'identite de test copiees dans le dossier "pieces"
        for (File f : pieces) {
            try {
                String hash = HashUtil.sha256Fichier(f);
                Files.deleteIfExists(Paths.get("pieces", hash + ".png"));
            } catch (Exception e) {
                // ignore
            }
        }
        try {
            Path dossier = Paths.get("pieces");
            if (Files.isDirectory(dossier) && Files.list(dossier).count() == 0) {
                Files.delete(dossier);
            }
        } catch (Exception e) {
            // ignore
        }
    }
}
