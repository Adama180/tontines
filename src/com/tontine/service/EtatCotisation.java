package com.tontine.service;

import java.time.LocalDate;

/**
 * Situation d'un membre d'un fonds commun face a ses cotisations : une cotisation par periode
 * (semaine, mois ou annee selon la frequence), pendant toute la duree du cycle.
 */
public class EtatCotisation {

    /** Nombre de cotisations attendues de ce membre pendant le cycle. */
    public int attendues;
    /** Cotisations deja faites (confirmees ou en attente de confirmation). */
    public int faites;
    /** Rang de la prochaine cotisation (faites + 1). */
    public int rang;
    /** Vrai si le membre peut cotiser aujourd'hui. */
    public boolean peutCotiser;
    /** Date a partir de laquelle il pourra cotiser (si pas encore possible). */
    public LocalDate disponibleLe;
    /** Date limite de la prochaine cotisation (apres elle : penalite). */
    public LocalDate echeance;
    /** Date ET heure limites de la prochaine cotisation (jour et heure definis par le gerant). */
    public java.time.LocalDateTime limite;
    /** Vrai si la prochaine cotisation est deja en retard. */
    public boolean enRetard;
    /** Penalite qui s'ajoutera au paiement (0 si a l'heure). */
    public double penalite;
    /** Tour de role : motif pour lequel le membre n'a pas a cotiser ce cycle (reciprocite), ou null. */
    public String exemption;
    /** Tour de role : penalites impayees d'anciens cycles, ajoutees au prochain paiement. */
    public double arrieres;
    /** Tour de role / tirage au sort : nom du beneficiaire du cycle (null si le tirage n'a pas eu lieu). */
    public String beneficiaire;
    /** Tirage au sort : le tirage de ce cycle doit d'abord avoir lieu. */
    public boolean tirageAttendu;
    /** Date de fin du cycle. */
    public LocalDate finCycle;

    public boolean termine() {
        return faites >= attendues;
    }
}
