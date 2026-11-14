package com.tontine.model;

import java.time.LocalDateTime;

/** Table demande_reactivation : demande d'un utilisateur suspendu pour retrouver l'acces \u00e0 son compte. */
public class DemandeReactivation {
    private int id;
    private int userId;
    private String motif;
    private LocalDateTime dateDemande;
    private StatutDemande statut;
    private Integer adminId;
    private LocalDateTime dateTraitement;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public LocalDateTime getDateDemande() { return dateDemande; }
    public void setDateDemande(LocalDateTime dateDemande) { this.dateDemande = dateDemande; }
    public StatutDemande getStatut() { return statut; }
    public void setStatut(StatutDemande statut) { this.statut = statut; }
    public Integer getAdminId() { return adminId; }
    public void setAdminId(Integer adminId) { this.adminId = adminId; }
    public LocalDateTime getDateTraitement() { return dateTraitement; }
    public void setDateTraitement(LocalDateTime dateTraitement) { this.dateTraitement = dateTraitement; }
}
