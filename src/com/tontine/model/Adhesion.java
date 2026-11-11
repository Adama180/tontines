package com.tontine.model;

import java.time.LocalDateTime;

/** Table adhesion : la participation d'un utilisateur \u00e0 UNE tontine, avec son r\u00f4le. */
public class Adhesion {
    private int id;
    private int tontineId;
    private int userId;
    private RoleAdhesion role;
    private LocalDateTime dateAdhesion;
    private double solde;
    private double totalCotise;
    private int cotisationsFaites;
    private Integer ordreTour;
    private StatutAdhesion statut;

    public Adhesion() {
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

    public RoleAdhesion getRole() {
        return role;
    }

    public void setRole(RoleAdhesion role) {
        this.role = role;
    }

    public LocalDateTime getDateAdhesion() {
        return dateAdhesion;
    }

    public void setDateAdhesion(LocalDateTime dateAdhesion) {
        this.dateAdhesion = dateAdhesion;
    }

    public double getSolde() {
        return solde;
    }

    public void setSolde(double solde) {
        this.solde = solde;
    }

    public double getTotalCotise() {
        return totalCotise;
    }

    public void setTotalCotise(double totalCotise) {
        this.totalCotise = totalCotise;
    }

    public int getCotisationsFaites() {
        return cotisationsFaites;
    }

    public void setCotisationsFaites(int cotisationsFaites) {
        this.cotisationsFaites = cotisationsFaites;
    }

    public Integer getOrdreTour() {
        return ordreTour;
    }

    public void setOrdreTour(Integer ordreTour) {
        this.ordreTour = ordreTour;
    }

    public StatutAdhesion getStatut() {
        return statut;
    }

    public void setStatut(StatutAdhesion statut) {
        this.statut = statut;
    }

    public boolean isGerant() {
        return role == RoleAdhesion.GERANT;
    }
}
