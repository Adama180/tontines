package com.tontine.model;

import java.time.LocalDateTime;

/** Table demande_adhesion : demande pour rejoindre une tontine (valid\u00e9e par le g\u00e9rant). */
public class DemandeAdhesion {
    private int id;
    private int tontineId;
    private int userId;
    private String nom;
    private String telephone;
    private String email;
    private String pieceHash;
    private String pieceChemin;
    private LocalDateTime dateEnvoi;
    private StatutDemande statut;

    public DemandeAdhesion() {
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

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPieceHash() {
        return pieceHash;
    }

    public void setPieceHash(String pieceHash) {
        this.pieceHash = pieceHash;
    }

    public String getPieceChemin() {
        return pieceChemin;
    }

    public void setPieceChemin(String pieceChemin) {
        this.pieceChemin = pieceChemin;
    }

    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public StatutDemande getStatut() {
        return statut;
    }

    public void setStatut(StatutDemande statut) {
        this.statut = statut;
    }
}
