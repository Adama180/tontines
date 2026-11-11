package com.tontine.model;

import java.time.LocalDateTime;

/** Table transaction : cotisation, pr\u00eat, remboursement, distribution ou p\u00e9nalit\u00e9. */
public class Transaction {
    private int id;
    private int tontineId;
    private int userId;
    private Integer cycleId;
    private TypeTransaction type;
    private double montant;
    private LocalDateTime date;
    private StatutTransaction statut;
    private ModePaiement modePaiement;
    private String reference;

    public Transaction() {
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

    public Integer getCycleId() {
        return cycleId;
    }

    public void setCycleId(Integer cycleId) {
        this.cycleId = cycleId;
    }

    public TypeTransaction getType() {
        return type;
    }

    public void setType(TypeTransaction type) {
        this.type = type;
    }

    public double getMontant() {
        return montant;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public StatutTransaction getStatut() {
        return statut;
    }

    public void setStatut(StatutTransaction statut) {
        this.statut = statut;
    }

    public ModePaiement getModePaiement() {
        return modePaiement;
    }

    public void setModePaiement(ModePaiement modePaiement) {
        this.modePaiement = modePaiement;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
