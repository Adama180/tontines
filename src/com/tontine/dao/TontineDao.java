package com.tontine.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.tontine.model.*;

/** DAO de la table `tontine` : insertion, lecture, modification, suppression et requetes utiles. */
public class TontineDao extends BaseDao {

    private Tontine map(java.sql.ResultSet rs) throws java.sql.SQLException {
        Tontine o = new Tontine();
        o.setId(rs.getInt("id"));
        o.setNom(rs.getString("nom"));
        o.setDescription(rs.getString("description"));
        o.setType(enumOf(TypeTontine.class, rs.getString("type")));
        o.setMontantCotisation(rs.getDouble("montant_cotisation"));
        o.setMontantMaxCotisation(getDoubleObj(rs, "montant_max_cotisation"));
        o.setFrequence(enumOf(Frequence.class, rs.getString("frequence")));
        o.setDureeEnCycle(rs.getInt("duree_en_cycle"));
        o.setPenaliteRetard(rs.getDouble("penalite_retard"));
        o.setNbMembreMax(rs.getInt("nb_membre_max"));
        o.setTauxInteret(getDoubleObj(rs, "taux_interet"));
        o.setDureeMaxPret(getInteger(rs, "duree_max_pret"));
        o.setJourCotisation(getInteger(rs, "jour_cotisation"));
        o.setMoisCotisation(getInteger(rs, "mois_cotisation"));
        java.sql.Time h = rs.getTime("heure_cotisation");
        o.setHeureCotisation(h == null ? java.time.LocalTime.of(23, 59) : h.toLocalTime());
        o.setTourActuelIndex(rs.getInt("tour_actuel_index"));
        o.setGerantId(rs.getInt("gerant_id"));
        o.setStatut(enumOf(StatutTontine.class, rs.getString("statut")));
        o.setDateCreation(rs.getObject("date_creation", LocalDateTime.class));
        return o;
    }

    /** Insere l'objet, renseigne son id et retourne cet id. */
    public int insert(Tontine o) {
        if (o.getStatut() == null) {
            o.setStatut(StatutTontine.ACTIVE);
        }
        if (o.getDateCreation() == null) {
            o.setDateCreation(LocalDateTime.now());
        }

        int id = insertReturnId(
                "INSERT INTO `tontine` (`nom`,`description`,`type`,`montant_cotisation`,`montant_max_cotisation`,`frequence`,`duree_en_cycle`,`penalite_retard`,`nb_membre_max`,`taux_interet`,`duree_max_pret`,`jour_cotisation`,`mois_cotisation`,`heure_cotisation`,`tour_actuel_index`,`gerant_id`,`statut`,`date_creation`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                o.getNom(),
                o.getDescription(),
                o.getType(),
                o.getMontantCotisation(),
                o.getMontantMaxCotisation(),
                o.getFrequence(),
                o.getDureeEnCycle(),
                o.getPenaliteRetard(),
                o.getNbMembreMax(),
                o.getTauxInteret(),
                o.getDureeMaxPret(),
                o.getJourCotisation(),
                o.getMoisCotisation(),
                java.sql.Time.valueOf(o.getHeureCotisation()),
                o.getTourActuelIndex(),
                o.getGerantId(),
                o.getStatut(),
                o.getDateCreation());
        o.setId(id);
        return id;
    }

    public boolean update(Tontine o) {
        return executeUpdate("UPDATE `tontine` SET `nom`=?,`description`=?,`type`=?,`montant_cotisation`=?,`montant_max_cotisation`=?,`frequence`=?,`duree_en_cycle`=?,`penalite_retard`=?,`nb_membre_max`=?,`taux_interet`=?,`duree_max_pret`=?,`jour_cotisation`=?,`mois_cotisation`=?,`heure_cotisation`=?,`tour_actuel_index`=?,`gerant_id`=?,`statut`=?,`date_creation`=? WHERE `id`=?",
                o.getNom(),
                o.getDescription(),
                o.getType(),
                o.getMontantCotisation(),
                o.getMontantMaxCotisation(),
                o.getFrequence(),
                o.getDureeEnCycle(),
                o.getPenaliteRetard(),
                o.getNbMembreMax(),
                o.getTauxInteret(),
                o.getDureeMaxPret(),
                o.getJourCotisation(),
                o.getMoisCotisation(),
                java.sql.Time.valueOf(o.getHeureCotisation()),
                o.getTourActuelIndex(),
                o.getGerantId(),
                o.getStatut(),
                o.getDateCreation(), o.getId()) > 0;
    }

    public boolean delete(int id) {
        return executeUpdate("DELETE FROM `tontine` WHERE `id`=?", id) > 0;
    }

    public Tontine findById(int id) {
        return queryOne("SELECT * FROM `tontine` WHERE `id`=?", this::map, id);
    }

    public List<Tontine> findAll() {
        return queryList("SELECT * FROM `tontine` ORDER BY `id`", this::map);
    }

    public Tontine findByNom(String nom) {
        return queryOne("SELECT * FROM `tontine` WHERE `nom`=?", this::map, nom);
    }

    public boolean nomExiste(String nom) {
        return scalarInt("SELECT COUNT(*) FROM `tontine` WHERE `nom`=?", nom) > 0;
    }

    public List<Tontine> findByGerant(int gerantId) {
        return queryList("SELECT * FROM `tontine` WHERE `gerant_id`=? ORDER BY `nom`", this::map, gerantId);
    }

    public List<Tontine> findByType(TypeTontine type) {
        return queryList("SELECT * FROM `tontine` WHERE `type`=? AND `statut`='ACTIVE' ORDER BY `nom`", this::map, type);
    }

    public List<Tontine> findActives() {
        return queryList("SELECT * FROM `tontine` WHERE `statut`='ACTIVE' ORDER BY `nom`", this::map);
    }

    /** Toutes les tontines dont l'utilisateur est membre actif (membre ou gerant). */
    public List<Tontine> findByMembre(int userId) {
        return queryList("SELECT t.* FROM `tontine` t JOIN `adhesion` a ON a.`tontine_id`=t.`id` "
                + "WHERE a.`user_id`=? AND a.`statut`='ACTIF' ORDER BY t.`nom`", this::map, userId);
    }

    public boolean changerStatut(int id, StatutTontine statut) {
        return executeUpdate("UPDATE `tontine` SET `statut`=? WHERE `id`=?", statut, id) > 0;
    }

    public boolean changerTourActuel(int id, int index) {
        return executeUpdate("UPDATE `tontine` SET `tour_actuel_index`=? WHERE `id`=?", index, id) > 0;
    }
}
