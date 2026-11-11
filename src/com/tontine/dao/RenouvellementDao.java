package com.tontine.dao;

import java.time.LocalDateTime;
import java.util.List;

import com.tontine.model.Renouvellement;
import com.tontine.model.StatutRenouvellement;

/** DAO de la table `renouvellement` : alertes de relance d'une tontine et reponses des membres. */
public class RenouvellementDao extends BaseDao {

    private Renouvellement map(java.sql.ResultSet rs) throws java.sql.SQLException {
        Renouvellement o = new Renouvellement();
        o.setId(rs.getInt("id"));
        o.setTontineId(rs.getInt("tontine_id"));
        o.setUserId(rs.getInt("user_id"));
        o.setCycleNumero(rs.getInt("cycle_numero"));
        o.setStatut(enumOf(StatutRenouvellement.class, rs.getString("statut")));
        o.setDateEnvoi(rs.getObject("date_envoi", LocalDateTime.class));
        o.setDateReponse(rs.getObject("date_reponse", LocalDateTime.class));
        o.setDateLimite(rs.getObject("date_limite", LocalDateTime.class));
        return o;
    }

    public int insert(Renouvellement o) {
        if (o.getDateEnvoi() == null) {
            o.setDateEnvoi(LocalDateTime.now());
        }
        if (o.getStatut() == null) {
            o.setStatut(StatutRenouvellement.EN_ATTENTE);
        }
        int id = insertReturnId(
                "INSERT INTO `renouvellement` (`tontine_id`,`user_id`,`cycle_numero`,`statut`,`date_envoi`,`date_limite`) VALUES (?,?,?,?,?,?)",
                o.getTontineId(), o.getUserId(), o.getCycleNumero(), o.getStatut(), o.getDateEnvoi(), o.getDateLimite());
        o.setId(id);
        return id;
    }

    public Renouvellement findById(int id) {
        return queryOne("SELECT * FROM `renouvellement` WHERE `id`=?", this::map, id);
    }

    /** Alertes d'une tontine pour un cycle clos donne. */
    public List<Renouvellement> findByTontineEtCycle(int tontineId, int cycleNumero) {
        return queryList("SELECT * FROM `renouvellement` WHERE `tontine_id`=? AND `cycle_numero`=? ORDER BY `id`",
                this::map, tontineId, cycleNumero);
    }

    public Renouvellement findByTontineCycleEtUser(int tontineId, int cycleNumero, int userId) {
        return queryOne("SELECT * FROM `renouvellement` WHERE `tontine_id`=? AND `cycle_numero`=? AND `user_id`=?",
                this::map, tontineId, cycleNumero, userId);
    }

    public boolean repondre(int id, StatutRenouvellement statut) {
        return executeUpdate("UPDATE `renouvellement` SET `statut`=?, `date_reponse`=? WHERE `id`=?",
                statut, LocalDateTime.now(), id) > 0;
    }

    /** Alertes sans reponse dont la date limite est depassee. */
    public List<Renouvellement> findExpirees(LocalDateTime maintenant) {
        return queryList("SELECT * FROM `renouvellement` WHERE `statut`='EN_ATTENTE' AND `date_limite` IS NOT NULL "
                + "AND `date_limite` < ? ORDER BY `id`", this::map, maintenant);
    }
}
