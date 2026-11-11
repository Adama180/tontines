package com.tontine.model;

import java.time.LocalDateTime;

/** Table utilisateur : l'identit\u00e9 d'une personne, enregistr\u00e9e une seule fois. */
public class Utilisateur {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String motDePasse;
    private RoleSysteme roleSysteme;
    private StatutCompte statut;
    private LocalDateTime dateCreation;

    public Utilisateur() {
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

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public RoleSysteme getRoleSysteme() {
        return roleSysteme;
    }

    public void setRoleSysteme(RoleSysteme roleSysteme) {
        this.roleSysteme = roleSysteme;
    }

    public StatutCompte getStatut() {
        return statut;
    }

    public void setStatut(StatutCompte statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getNomComplet() {
        return prenom + " " + nom;
    }

    public boolean isAdmin() {
        return roleSysteme == RoleSysteme.ADMIN;
    }

    @Override
    public String toString() {
        return getNomComplet();
    }
}
