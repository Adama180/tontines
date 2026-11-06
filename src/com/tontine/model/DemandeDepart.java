package com.tontine.model;

public class DemandeDepart {

    private int id;
    private int tontineId;
    private int userId;
    private String motif;
    private StatutDemande statut = StatutDemande.EN_ATTENTE;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTontineId() { return tontineId; }
    public void setTontineId(int tontineId) { this.tontineId = tontineId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public StatutDemande getStatut() { return statut; }
    public void setStatut(StatutDemande statut) { this.statut = statut; }
}