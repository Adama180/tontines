package com.tontine.dao;

import java.util.List;

import com.tontine.model.DemandeDepart;
import com.tontine.model.StatutDemande;

/** DAO de la table `demande_depart`. */
public class DemandeDepartDao extends BaseDao {

    private DemandeDepart map(java.sql.ResultSet rs) throws java.sql.SQLException {
        DemandeDepart d = new DemandeDepart();
        d.setId(rs.getInt("id"));
        d.setTontineId(rs.getInt("tontine_id"));
        d.setUserId(rs.getInt("user_id"));
        d.setMotif(rs.getString("motif"));
        d.setStatut(enumOf(StatutDemande.class, rs.getString("statut")));
        return d;
    }

    public int insert(DemandeDepart d) {
        if (d.getStatut() == null) {
            d.setStatut(StatutDemande.EN_ATTENTE);
        }
        int id = insertReturnId(
                "INSERT INTO `demande_depart` (`tontine_id`,`user_id`,`motif`,`statut`) VALUES (?,?,?,?)",
                d.getTontineId(),
                d.getUserId(),
                d.getMotif(),
                d.getStatut());
        d.setId(id);
        return id;
    }

    public DemandeDepart findById(int id) {
        return queryOne("SELECT * FROM `demande_depart` WHERE `id`=?", this::map, id);
    }

    public List<DemandeDepart> findByUser(int userId) {
        return queryList("SELECT * FROM `demande_depart` WHERE `user_id`=? ORDER BY `id`",
                this::map, userId);
    }

    public List<DemandeDepart> findByTontine(int tontineId) {
        return queryList("SELECT * FROM `demande_depart` WHERE `tontine_id`=? ORDER BY `id`",
                this::map, tontineId);
    }

    public boolean changerStatut(int id, StatutDemande statut) {
        return executeUpdate("UPDATE `demande_depart` SET `statut`=? WHERE `id`=?", statut, id) > 0;
    }
    public List<DemandeDepart> findByTontineEtStatut(int tontineId, StatutDemande statut) {
        return queryList(
                "SELECT * FROM `demande_depart` WHERE `tontine_id`=? AND `statut`=? ORDER BY `id`",
                this::map, tontineId, statut);
    }
}
