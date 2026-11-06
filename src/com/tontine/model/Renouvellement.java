package com.tontine.model;

import java.time.LocalDateTime;

/** Table renouvellement : alerte envoyee par le gerant a un membre apres la fin d'un cycle (rester ou partir). */
public class Renouvellement {
    private int id;
    private int tontineId;
    private int userId;
    /** Numero du dernier cycle clos au moment de l'alerte. */
    private int cycleNumero;
    private StatutRenouvellement statut;
    private LocalDateTime dateEnvoi;
    private LocalDateTime dateReponse;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTontineId() { return tontineId; }
    public void setTontineId(int tontineId) { this.tontineId = tontineId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getCycleNumero() { return cycleNumero; }
    public void setCycleNumero(int cycleNumero) { this.cycleNumero = cycleNumero; }
    public StatutRenouvellement getStatut() { return statut; }
    public void setStatut(StatutRenouvellement statut) { this.statut = statut; }
    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }
    public LocalDateTime getDateReponse() { return dateReponse; }
    public void setDateReponse(LocalDateTime dateReponse) { this.dateReponse = dateReponse; }
}
