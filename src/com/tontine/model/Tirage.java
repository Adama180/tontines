package com.tontine.model;

import java.time.LocalDateTime;

/** Table tirage : r\u00e9sultat d'un tirage au sort (non modifiable sans audit). */
public class Tirage {
    private int id;
    private int tontineId;
    private int semaine;
    private LocalDateTime date;
    private int beneficiaireId;

    public Tirage() {
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

    public int getSemaine() {
        return semaine;
    }

    public void setSemaine(int semaine) {
        this.semaine = semaine;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public int getBeneficiaireId() {
        return beneficiaireId;
    }

    public void setBeneficiaireId(int beneficiaireId) {
        this.beneficiaireId = beneficiaireId;
    }
}
