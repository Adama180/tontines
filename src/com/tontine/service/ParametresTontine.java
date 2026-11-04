package com.tontine.service;

import com.tontine.model.Frequence;

/** Regles d'une tontine definies par son gerant (aucune valeur n'est codee en dur). */
public class ParametresTontine {

    private String description;
    private double montantCotisation;
    private Frequence frequence;
    private int dureeEnCycle;
    private Double montantMaxCotisation; // fonds commun : montantCotisation = minimum
    private double penaliteRetard;
    private int nbMembreMax;
    private Double tauxInteret;      // fonds commun uniquement (en %)
    private Integer dureeMaxPret;    // fonds commun uniquement (en jours)

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getMontantCotisation() { return montantCotisation; }
    public void setMontantCotisation(double v) { this.montantCotisation = v; }
    public Double getMontantMaxCotisation() { return montantMaxCotisation; }
    public void setMontantMaxCotisation(Double v) { this.montantMaxCotisation = v; }
    public Frequence getFrequence() { return frequence; }
    public void setFrequence(Frequence frequence) { this.frequence = frequence; }
    public int getDureeEnCycle() { return dureeEnCycle; }
    public void setDureeEnCycle(int dureeEnCycle) { this.dureeEnCycle = dureeEnCycle; }
    public double getPenaliteRetard() { return penaliteRetard; }
    public void setPenaliteRetard(double penaliteRetard) { this.penaliteRetard = penaliteRetard; }
    public int getNbMembreMax() { return nbMembreMax; }
    public void setNbMembreMax(int nbMembreMax) { this.nbMembreMax = nbMembreMax; }
    public Double getTauxInteret() { return tauxInteret; }
    public void setTauxInteret(Double tauxInteret) { this.tauxInteret = tauxInteret; }
    public Integer getDureeMaxPret() { return dureeMaxPret; }
    public void setDureeMaxPret(Integer dureeMaxPret) { this.dureeMaxPret = dureeMaxPret; }
}
