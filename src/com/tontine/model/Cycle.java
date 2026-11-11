package com.tontine.model;

import java.time.LocalDate;

/** Table cycle : une p\u00e9riode de cotisation d'une tontine. */
public class Cycle {
    private int id;
    private int tontineId;
    private int numero;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private StatutCycle statut;

    public Cycle() {
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

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public StatutCycle getStatut() {
        return statut;
    }

    public void setStatut(StatutCycle statut) {
        this.statut = statut;
    }
}
