package com.tontine.model;

import java.time.LocalDateTime;

/** Table tontine : les r\u00e8gles d\u00e9finies par le g\u00e9rant (rien n'est cod\u00e9 en dur). */
public class Tontine {
    private int id;
    private String nom;
    private String description;
    private TypeTontine type;
    private double montantCotisation;
    private Frequence frequence;
    private int dureeEnCycle;
    private Double montantMaxCotisation; // fonds commun : montantCotisation = minimum, ceci = maximum
    private double penaliteRetard;
    private int nbMembreMax;
    private Double tauxInteret;
    private Integer dureeMaxPret;
    private Integer jourCotisation;
    private Integer moisCotisation;
    private java.time.LocalTime heureCotisation = java.time.LocalTime.of(23, 59);
    private int tourActuelIndex;
    private int gerantId;
    private StatutTontine statut;
    private LocalDateTime dateCreation;

    public Tontine() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TypeTontine getType() {
        return type;
    }

    public void setType(TypeTontine type) {
        this.type = type;
    }

    public double getMontantCotisation() {
        return montantCotisation;
    }

    public void setMontantCotisation(double montantCotisation) {
        this.montantCotisation = montantCotisation;
    }

    public Frequence getFrequence() {
        return frequence;
    }

    public void setFrequence(Frequence frequence) {
        this.frequence = frequence;
    }

    public int getDureeEnCycle() {
        return dureeEnCycle;
    }

    public void setDureeEnCycle(int dureeEnCycle) {
        this.dureeEnCycle = dureeEnCycle;
    }

    public Double getMontantMaxCotisation() {
        return montantMaxCotisation;
    }

    public void setMontantMaxCotisation(Double montantMaxCotisation) {
        this.montantMaxCotisation = montantMaxCotisation;
    }

    public double getPenaliteRetard() {
        return penaliteRetard;
    }

    public void setPenaliteRetard(double penaliteRetard) {
        this.penaliteRetard = penaliteRetard;
    }

    public int getNbMembreMax() {
        return nbMembreMax;
    }

    public void setNbMembreMax(int nbMembreMax) {
        this.nbMembreMax = nbMembreMax;
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

    public int getTourActuelIndex() {
        return tourActuelIndex;
    }

    public void setTourActuelIndex(int tourActuelIndex) {
        this.tourActuelIndex = tourActuelIndex;
    }

    public int getGerantId() {
        return gerantId;
    }

    public void setGerantId(int gerantId) {
        this.gerantId = gerantId;
    }

    public StatutTontine getStatut() {
        return statut;
    }

    public void setStatut(StatutTontine statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public boolean isFondsCommun() {
        return type == TypeTontine.FONDS_COMMUN;
    }

    public boolean isTourDeRole() {
        return type == TypeTontine.TOUR_DE_ROLE;
    }

    public boolean isTirageAuSort() {
        return type == TypeTontine.TIRAGE_AU_SORT;
    }

    @Override
    public String toString() {
        return nom;
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
        return heureCotisation == null ? java.time.LocalTime.of(23, 59) : heureCotisation;
    }

    public void setHeureCotisation(java.time.LocalTime heureCotisation) {
        this.heureCotisation = heureCotisation;
    }
}
