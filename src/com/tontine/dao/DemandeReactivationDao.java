package com.tontine.dao;

import java.time.LocalDateTime;
import java.util.List;

import com.tontine.model.DemandeReactivation;
import com.tontine.model.StatutDemande;

/** DAO de la table `demande_reactivation` : demandes de reactivation de compte adressees a l'administrateur. */
public class DemandeReactivationDao extends BaseDao {

    private DemandeReactivation map(java.sql.ResultSet rs) throws java.sql.SQLException {
        DemandeReactivation o = new DemandeReactivation();
        o.setId(rs.getInt("id"));
        o.setUserId(rs.getInt("user_id"));
        o.setMotif(rs.getString("motif"));
        o.setDateDemande(rs.getObject("date_demande", LocalDateTime.class));
        o.setStatut(enumOf(StatutDemande.class, rs.getString("statut")));
        o.setAdminId(getInteger(rs, "admin_id"));
        o.setDateTraitement(rs.getObject("date_traitement", LocalDateTime.class));
        return o;
    }

    public int insert(DemandeReactivation o) {
        if (o.getDateDemande() == null) {
            o.setDateDemande(LocalDateTime.now());
        }
        if (o.getStatut() == null) {
            o.setStatut(StatutDemande.EN_ATTENTE);
        }
        int id = insertReturnId(
                "INSERT INTO `demande_reactivation` (`user_id`,`motif`,`date_demande`,`statut`) VALUES (?,?,?,?)",
                o.getUserId(), o.getMotif(), o.getDateDemande(), o.getStatut());
        o.setId(id);
        return id;
    }

    public DemandeReactivation findById(int id) {
        return queryOne("SELECT * FROM `demande_reactivation` WHERE `id`=?", this::map, id);
    }

    public List<DemandeReactivation> findByStatut(StatutDemande statut) {
        return queryList("SELECT * FROM `demande_reactivation` WHERE `statut`=? ORDER BY `date_demande`", this::map, statut);
    }

    public List<DemandeReactivation> findByUser(int userId) {
        return queryList("SELECT * FROM `demande_reactivation` WHERE `user_id`=? ORDER BY `date_demande` DESC", this::map, userId);
    }

    public boolean traiter(int id, StatutDemande statut, int adminId) {
        return executeUpdate("UPDATE `demande_reactivation` SET `statut`=?, `admin_id`=?, `date_traitement`=? WHERE `id`=?",
                statut, adminId, LocalDateTime.now(), id) > 0;
    }
}
