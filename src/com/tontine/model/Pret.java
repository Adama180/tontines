package com.tontine.model;

import java.time.LocalDateTime;

/** Table pret : pr\u00eat sur le fonds commun (Type 1 uniquement). */
public class Pret {
    private int id;
    private int tontineId;
    private int userId;
    private double montant;
    private int nbJours;
    private String motif;
    private LocalDateTime dateDemande;
    private StatutPret statut;
    private Integer valideurId;
    private double montantRembourse;

    public Pret() {
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

    public double getMontant() {
        return montant;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }

    public int getNbJours() {
        return nbJours;
    }

    public void setNbJours(int nbJours) {
        this.nbJours = nbJours;
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

    public StatutPret getStatut() {
        return statut;
    }

    public void setStatut(StatutPret statut) {
        this.statut = statut;
    }

    public Integer getValideurId() {
        return valideurId;
    }

    public void setValideurId(Integer valideurId) {
        this.valideurId = valideurId;
    }

    public double getMontantRembourse() {
        return montantRembourse;
    }

    public void setMontantRembourse(double montantRembourse) {
        this.montantRembourse = montantRembourse;
    }
}
