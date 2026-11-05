package com.tontine.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.tontine.dao.AdhesionDao;
import com.tontine.dao.DemandeAdhesionDao;
import com.tontine.dao.DemandeCreationDao;
import com.tontine.dao.TontineDao;
import com.tontine.dao.UtilisateurDao;
import com.tontine.model.DemandeAdhesion;
import com.tontine.model.DemandeCreation;
import com.tontine.model.StatutDemande;
import com.tontine.model.StatutTontine;
import com.tontine.model.Tontine;
import com.tontine.model.TypeTontine;
import com.tontine.model.Utilisateur;
import com.tontine.util.HashUtil;

/** Demandes d'un utilisateur : rejoindre une tontine, ou en creer une (devenir gerant). */
public class DemandeService {

    private static final long TAILLE_MAX_PIECE = 5L * 1024 * 1024;
    /** Dossier (a la racine du projet) ou sont copiees les pieces d'identite. */
    public static final String DOSSIER_PIECES = "pieces";

    private final AuthService auth = new AuthService();
    private final UtilisateurDao utilisateurDao = new UtilisateurDao();
    private final TontineDao tontineDao = new TontineDao();
    private final AdhesionDao adhesionDao = new AdhesionDao();
    private final DemandeAdhesionDao demandeAdhesionDao = new DemandeAdhesionDao();
    private final DemandeCreationDao demandeCreationDao = new DemandeCreationDao();

    // ---------------------------------------------------------------- adhesion

    /**
     * Visiteur qui clique sur "Rejoindre cette tontine" : cree son compte ET sa demande en une seule action.
     */
    public Utilisateur inscrireEtDemanderAdhesion(String nom, String prenom, String email, String telephone,
            String motDePasse, int tontineId, File pieceIdentite) {
        Tontine t = Controles.tontine(tontineId);
        Controles.exiger(t.getStatut() == StatutTontine.ACTIVE, "Cette tontine n'accepte plus de demandes.");
        controlerPiece(pieceIdentite, -1); // verifie avant de creer le compte
        Utilisateur u = auth.inscrire(nom, prenom, email, telephone, motDePasse);
        try {
            demanderAdhesion(u.getId(), tontineId, pieceIdentite);
        } catch (RuntimeException e) {
            utilisateurDao.delete(u.getId()); // on ne garde pas un compte a moitie cree
            throw e;
        }
        return u;
    }

    /** Utilisateur deja inscrit qui demande a rejoindre une (autre) tontine. */
    public DemandeAdhesion demanderAdhesion(int userId, int tontineId, File pieceIdentite) {
        Utilisateur u = Controles.utilisateur(userId);
        Tontine t = Controles.tontine(tontineId);
        Controles.exiger(t.getStatut() == StatutTontine.ACTIVE, "Cette tontine n'accepte plus de demandes.");
        Controles.exiger(!adhesionDao.estMembre(tontineId, userId), "Vous \u00eates d\u00e9j\u00e0 membre de cette tontine.");
        Controles.exiger(!demandeAdhesionDao.demandeEnAttenteExiste(tontineId, userId),
                "Vous avez d\u00e9j\u00e0 une demande en attente pour cette tontine.");

        String[] piece = preparerPiece(pieceIdentite, userId);

        DemandeAdhesion d = new DemandeAdhesion();
        d.setTontineId(tontineId);
        d.setUserId(userId);
        d.setNom(u.getNomComplet());
        d.setTelephone(u.getTelephone());
        d.setEmail(u.getEmail());
        d.setPieceHash(piece[0]);
        d.setPieceChemin(piece[1]);
        demandeAdhesionDao.insert(d);

        NotificationService.notifier(t.getGerantId(),
                "Nouvelle demande d'adh\u00e9sion de " + u.getNomComplet() + " pour la tontine " + t.getNom() + ".",
                "ADHESION");
        return d;
    }

    // ---------------------------------------------------------------- creation

    /**
     * Utilisateur qui souhaite devenir gerant : TOUS les parametres de la tontine (montants, frequence, date et
     * heure de cotisation, penalites, pret...) sont definis ici, avant l'envoi. La demande est ensuite validee
     * par l'administrateur, sans que le gerant ait d'autres parametres a definir.
     */
    public DemandeCreation soumettreDemandeCreation(int userId, String nomSouhaite, TypeTontine type,
            ParametresTontine p, File pieceIdentite) {
        Utilisateur u = Controles.utilisateur(userId);
        nomSouhaite = Controles.texte(nomSouhaite, "nom de la tontine");
        Controles.exiger(p != null, "Les param\u00e8tres de la tontine sont obligatoires.");
        p.controler(type, 1);
        Controles.exiger(!tontineDao.nomExiste(nomSouhaite), "Une tontine porte d\u00e9j\u00e0 ce nom.");
        for (DemandeCreation autre : demandeCreationDao.findByStatut(StatutDemande.EN_ATTENTE)) {
            Controles.exiger(!autre.getNomSouhaite().equalsIgnoreCase(nomSouhaite),
                    "Une demande en attente utilise d\u00e9j\u00e0 ce nom.");
        }

        String[] piece = preparerPiece(pieceIdentite, userId);

        DemandeCreation d = new DemandeCreation();
        d.setDemandeurId(userId);
        d.setPieceHash(piece[0]);
        d.setPieceChemin(piece[1]);
        d.setNomSouhaite(nomSouhaite);
        d.setTypeSouhaite(type);
        d.setDescription(p.getDescription() == null ? "" : p.getDescription().trim());
        d.setMontant(p.getMontantCotisation());
        d.setMontantMax(type == TypeTontine.FONDS_COMMUN ? p.getMontantMaxCotisation() : null);
        d.setFrequence(p.getFrequence());
        d.setNbMembre(p.getNbMembreMax());
        d.setDureeEnCycle(p.getDureeEnCycle());
        d.setPenaliteRetard(p.getPenaliteRetard());
        d.setTauxInteret(type == TypeTontine.FONDS_COMMUN ? p.getTauxInteret() : null);
        d.setDureeMaxPret(type == TypeTontine.FONDS_COMMUN ? p.getDureeMaxPret() : null);
        d.setJourCotisation(p.getJourCotisation());
        d.setMoisCotisation(p.getMoisCotisation());
        d.setHeureCotisation(p.getHeureCotisation());
        demandeCreationDao.insert(d);

        NotificationService.notifierAdmins(
                "Nouvelle demande de cr\u00e9ation de tontine '" + nomSouhaite + "' par " + u.getNomComplet() + ".",
                "CREATION");
        return d;
    }

    /** Visiteur qui clique sur "Creer ma propre tontine" : compte + demande en une action. */
    public Utilisateur inscrireEtDemanderCreation(String nom, String prenom, String email, String telephone,
            String motDePasse, String nomTontine, TypeTontine type, ParametresTontine p, File pieceIdentite) {
        Controles.exiger(p != null, "Les param\u00e8tres de la tontine sont obligatoires.");
        p.controler(type, 1); // verifie avant de creer le compte
        controlerPiece(pieceIdentite, -1);
        Utilisateur u = auth.inscrire(nom, prenom, email, telephone, motDePasse);
        try {
            soumettreDemandeCreation(u.getId(), nomTontine, type, p, pieceIdentite);
        } catch (RuntimeException e) {
            utilisateurDao.delete(u.getId());
            throw e;
        }
        return u;
    }

    // ---------------------------------------------------------------- suivi

    public List<DemandeAdhesion> suivreDemandesAdhesion(int userId) {
        return demandeAdhesionDao.findByUser(userId);
    }

    public List<DemandeCreation> suivreDemandesCreation(int userId) {
        return demandeCreationDao.findByDemandeur(userId);
    }

    // ---------------------------------------------------------------- piece d'identite

    /** Vrai si l'utilisateur a deja fourni une piece d'identite (il n'a pas besoin de la renvoyer). */
    public boolean aPieceEnregistree(int userId) {
        return pieceEnregistree(userId) != null;
    }

    /** Derniere piece d'identite fournie par l'utilisateur : {empreinte, chemin}, ou null. */
    private String[] pieceEnregistree(int userId) {
        for (DemandeAdhesion d : demandeAdhesionDao.findByUser(userId)) {
            if (d.getPieceHash() != null && !d.getPieceHash().isEmpty()) {
                return new String[] { d.getPieceHash(), d.getPieceChemin() };
            }
        }
        for (DemandeCreation d : demandeCreationDao.findByDemandeur(userId)) {
            if (d.getPieceHash() != null && !d.getPieceHash().isEmpty()) {
                return new String[] { d.getPieceHash(), d.getPieceChemin() };
            }
        }
        return null;
    }

    /**
     * Nouvelle piece fournie : elle est controlee et copiee. Sinon, on reutilise celle deja enregistree
     * (pas besoin de tout ressaisir) ; s'il n'y en a pas, la piece est obligatoire.
     */
    private String[] preparerPiece(File piece, int userId) {
        if (piece != null) {
            String hash = controlerPiece(piece, userId);
            return new String[] { hash, copierPiece(piece, hash) };
        }
        String[] existante = pieceEnregistree(userId);
        Controles.exiger(existante != null, "La pi\u00e8ce d'identit\u00e9 est obligatoire.");
        return existante;
    }

    /** Verifie la piece et retourne son empreinte. userId = -1 pour une personne sans compte. */
    private String controlerPiece(File piece, int userId) {
        Controles.exiger(piece != null && piece.isFile(), "La pi\u00e8ce d'identit\u00e9 est obligatoire.");
        String nom = piece.getName().toLowerCase();
        Controles.exiger(nom.endsWith(".pdf") || nom.endsWith(".jpg") || nom.endsWith(".jpeg") || nom.endsWith(".png"),
                "Format de pi\u00e8ce accept\u00e9 : PDF, JPG ou PNG.");
        Controles.exiger(piece.length() <= TAILLE_MAX_PIECE, "La pi\u00e8ce d'identit\u00e9 ne doit pas d\u00e9passer 5 Mo.");
        String hash;
        try {
            hash = HashUtil.sha256Fichier(piece);
        } catch (IOException e) {
            throw new ServiceException("Impossible de lire la pi\u00e8ce d'identit\u00e9.");
        }
        // protection anti-doublon : la meme piece ne peut pas servir a deux personnes differentes
        DemandeAdhesion deja = demandeAdhesionDao.findByPieceHash(hash);
        DemandeCreation dejaCreation = demandeCreationDao.findByPieceHash(hash);
        Controles.exiger((deja == null || deja.getUserId() == userId)
                && (dejaCreation == null || dejaCreation.getDemandeurId() == userId),
                "Cette pi\u00e8ce d'identit\u00e9 a d\u00e9j\u00e0 \u00e9t\u00e9 utilis\u00e9e par un autre compte.");
        return hash;
    }

    private String copierPiece(File piece, String hash) {
        try {
            String nom = piece.getName();
            String ext = nom.substring(nom.lastIndexOf('.')).toLowerCase();
            Path dossier = Paths.get(DOSSIER_PIECES);
            Files.createDirectories(dossier);
            Path cible = dossier.resolve(hash + ext);
            if (!Files.exists(cible)) {
                Files.copy(piece.toPath(), cible, StandardCopyOption.REPLACE_EXISTING);
            }
            return cible.toString();
        } catch (IOException e) {
            throw new ServiceException("Impossible d'enregistrer la pi\u00e8ce d'identit\u00e9 : " + e.getMessage());
        }
    }
}
