package com.tontine.model;

import java.time.LocalDateTime;

/** Table journal_audit : trace des actions sensibles (consult\u00e9e par l'administrateur). */
public class JournalAudit {
    private int id;
    private Integer userId;
    private String action;
    private String details;
    private LocalDateTime date;

    public JournalAudit() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
