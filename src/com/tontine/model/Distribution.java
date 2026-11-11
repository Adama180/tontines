package com.tontine.model;

import java.time.LocalDateTime;

/** Table distribution : montant revers\u00e9 \u00e0 la cl\u00f4ture d'un cycle ou d'un tour. */
public class Distribution {
    private int id;
    private int cycleId;
    private Integer beneficiaireId;
    private LocalDateTime date;
    private double montantTotal;

    public Distribution() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCycleId() {
        return cycleId;
    }

    public void setCycleId(int cycleId) {
        this.cycleId = cycleId;
    }

    public Integer getBeneficiaireId() {
        return beneficiaireId;
    }

    public void setBeneficiaireId(Integer beneficiaireId) {
        this.beneficiaireId = beneficiaireId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public double getMontantTotal() {
        return montantTotal;
    }

    public void setMontantTotal(double montantTotal) {
        this.montantTotal = montantTotal;
    }
}
