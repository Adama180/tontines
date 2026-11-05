package com.tontine.model;

import java.time.LocalDateTime;

/** Table demande_creation : demande pour devenir g\u00e9rant (valid\u00e9e par l'administrateur). */
public class DemandeCreation {
    private int id;
    private int demandeurId;
    private String nomSouhaite;
    private TypeTontine typeSouhaite;
    private double montant;
    private Frequence frequence;
    private int nbMembre;
    private LocalDateTime dateEnvoi;
    private StatutDemande statut;
    private Integer adminId;
    private String pieceHash;
    private String pieceChemin;
    private String description;
    private Double montantMax;
    private int dureeEnCycle = 12;
    private double penaliteRetard;
    private Double tauxInteret;
    private Integer dureeMaxPret;
    private Integer jourCotisation;
    private Integer moisCotisation;
    private java.time.LocalTime heureCotisation = java.time.LocalTime.of(23, 59);

    public DemandeCreation() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDemandeurId() {
        return demandeurId;
    }

    public void setDemandeurId(int demandeurId) {
        this.demandeurId = demandeurId;
    }

    public String getNomSouhaite() {
        return nomSouhaite;
    }

    public void setNomSouhaite(String nomSouhaite) {
        this.nomSouhaite = nomSouhaite;
    }

    public TypeTontine getTypeSouhaite() {
        return typeSouhaite;
    }

    public void setTypeSouhaite(TypeTontine typeSouhaite) {
        this.typeSouhaite = typeSouhaite;
    }

    public double getMontant() {
        return montant;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }

    public Frequence getFrequence() {
        return frequence;
    }

    public void setFrequence(Frequence frequence) {
        this.frequence = frequence;
    }

    public int getNbMembre() {
        return nbMembre;
    }

    public void setNbMembre(int nbMembre) {
        this.nbMembre = nbMembre;
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

    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
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
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getMontantMax() {
        return montantMax;
    }

    public void setMontantMax(Double montantMax) {
        this.montantMax = montantMax;
    }

    public int getDureeEnCycle() {
        return dureeEnCycle;
    }

    public void setDureeEnCycle(int dureeEnCycle) {
        this.dureeEnCycle = dureeEnCycle;
    }

    public double getPenaliteRetard() {
        return penaliteRetard;
    }

    public void setPenaliteRetard(double penaliteRetard) {
        this.penaliteRetard = penaliteRetard;
    }

    public Double getTauxInteret() {
        return tauxInteret;
    }

    public void setTauxInteret(Double tauxInteret) {
        this.tauxInteret = tauxInteret;
    }

    public Integer getDureeMaxPret() {
        return dureeMaxPret;
    }

    public void setDureeMaxPret(Integer dureeMaxPret) {
        this.dureeMaxPret = dureeMaxPret;
    }

    public Integer getJourCotisation() {
        return jourCotisation;
    }

    public void setJourCotisation(Integer jourCotisation) {
        this.jourCotisation = jourCotisation;
    }

    public Integer getMoisCotisation() {
        return moisCotisation;
    }

    public void setMoisCotisation(Integer moisCotisation) {
        this.moisCotisation = moisCotisation;
    }

    public java.time.LocalTime getHeureCotisation() {
        return heureCotisation;
    }

    public void setHeureCotisation(java.time.LocalTime heureCotisation) {
        this.heureCotisation = heureCotisation;
    }

}
