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
    private Integer jourCotisation;  // hebdo : 1 (lundi) a 7 ; mensuelle / annuelle : jour du mois (1 a 31)
    private Integer moisCotisation;  // annuelle : mois (1 a 12)
    private java.time.LocalTime heureCotisation = java.time.LocalTime.of(23, 59); // heure limite de cotisation

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
    public Integer getJourCotisation() { return jourCotisation; }
    public void setJourCotisation(Integer v) { this.jourCotisation = v; }
    public Integer getMoisCotisation() { return moisCotisation; }
    public void setMoisCotisation(Integer v) { this.moisCotisation = v; }
    public java.time.LocalTime getHeureCotisation() { return heureCotisation; }
    public void setHeureCotisation(java.time.LocalTime v) { this.heureCotisation = v; }

    /**
     * Controle commun a la demande de creation (tout est defini avant l'envoi a l'administrateur) et a la
     * modification des regles. membresActuels = nombre de membres deja actifs (1 pour une nouvelle tontine).
     */
    public void controler(com.tontine.model.TypeTontine type, int membresActuels) {
        Controles.exiger(type != null, "Choisissez un type de tontine.");
        Controles.exiger(frequence != null, "Choisissez une fr\u00e9quence de cotisation.");
        if (type == com.tontine.model.TypeTontine.FONDS_COMMUN) {
            Controles.exiger(montantCotisation >= CotisationService.MIN_FONDS_COMMUN,
                    "Le montant minimum de cotisation doit \u00eatre d'au moins " + Format.montant(CotisationService.MIN_FONDS_COMMUN) + ".");
            Controles.exiger(montantMaxCotisation != null && montantMaxCotisation >= montantCotisation,
                    "Le montant maximum doit \u00eatre sup\u00e9rieur ou \u00e9gal au montant minimum.");
        } else {
            Controles.exiger(montantCotisation > 0, "Le montant de la cotisation doit \u00eatre positif.");
        }
        Controles.exiger(penaliteRetard >= 0, "La p\u00e9nalit\u00e9 de retard ne peut pas \u00eatre n\u00e9gative.");
        Controles.exiger(dureeEnCycle >= 1, "La dur\u00e9e d'un cycle doit \u00eatre d'au moins 1 p\u00e9riode.");
        Controles.exiger(nbMembreMax >= 2 && nbMembreMax >= membresActuels,
                "Le nombre maximum de membres doit \u00eatre au moins 2 et au moins \u00e9gal au nombre de membres actuels.");
        if (type == com.tontine.model.TypeTontine.FONDS_COMMUN) {
            Controles.exiger(tauxInteret != null && tauxInteret >= 0 && tauxInteret <= 100,
                    "Le taux d'int\u00e9r\u00eat doit \u00eatre compris entre 0 et 100 %.");
            Controles.exiger(dureeMaxPret != null && dureeMaxPret >= 1,
                    "La dur\u00e9e maximale d'un pr\u00eat doit \u00eatre d'au moins 1 jour.");
        }
        controlerHoraire();
    }

    /**
     * Date et heure de cotisation : jour de la semaine, du mois ou de l'annee selon la frequence, et heure limite.
     * Sans jour (valeur vide), la date limite est la fin de la periode.
     */
    private void controlerHoraire() {
        Controles.exiger(heureCotisation != null, "Indiquez l'heure limite de cotisation.");
        if (frequence == Frequence.JOURNALIERE) {
            jourCotisation = null;
            moisCotisation = null;
            return;
        }
        if (frequence == Frequence.ANNUELLE) {
            Controles.exiger((jourCotisation == null) == (moisCotisation == null),
                    "Pour une cotisation annuelle, indiquez le mois ET le jour.");
        } else {
            moisCotisation = null;
        }
        if (jourCotisation == null) {
            return;
        }
        switch (frequence) {
        case HEBDOMADAIRE:
            Controles.exiger(jourCotisation >= 1 && jourCotisation <= 7, "Choisissez le jour de la semaine de la cotisation.");
            break;
        case MENSUELLE:
            Controles.exiger(jourCotisation >= 1 && jourCotisation <= 31,
                    "Le jour du mois de la cotisation doit \u00eatre compris entre 1 et 31.");
            break;
        default:
            Controles.exiger(moisCotisation >= 1 && moisCotisation <= 12, "Choisissez le mois de la cotisation.");
            Controles.exiger(jourCotisation >= 1 && jourCotisation <= java.time.YearMonth.of(2024, moisCotisation).lengthOfMonth(),
                    "Ce jour n'existe pas dans le mois choisi.");
        }
    }
}
