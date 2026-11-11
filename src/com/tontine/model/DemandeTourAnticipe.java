package com.tontine.model;

import java.time.LocalDateTime;

/** Table demande_tour_anticipe : demande d'un membre d'une tontine \u00e0 tour de r\u00f4le. */
public class DemandeTourAnticipe {
    private int id;
    private int tontineId;
    private int userId;
    private String motif;
    private LocalDateTime dateDemande;
    private StatutDemande statut;

    public DemandeTourAnticipe() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTontineId() {
        return tontineId;
    }

    public void setTontineId(int tontineId) {
        this.tontineId = tontineId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public LocalDateTime getDateDemande() {
        return dateDemande;
    }

    public void setDateDemande(LocalDateTime dateDemande) {
        this.dateDemande = dateDemande;
    }

    public StatutDemande getStatut() {
        return statut;
    }

    public void setStatut(StatutDemande statut) {
        this.statut = statut;
    }
}
