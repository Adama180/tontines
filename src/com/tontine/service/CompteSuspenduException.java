package com.tontine.service;

/**
 * Connexion refusee parce que le compte est suspendu (identifiants corrects). L'ecran de connexion propose alors
 * le bouton \u00ab Demander la reactivation de mon compte \u00bb.
 */
public class CompteSuspenduException extends ServiceException {

    private static final long serialVersionUID = 1L;

    public CompteSuspenduException() {
        super("Votre compte est suspendu. Vous pouvez envoyer une demande de r\u00e9activation \u00e0 l'administrateur.");
    }
}
