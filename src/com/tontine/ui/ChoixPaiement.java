package com.tontine.ui;

import com.tontine.model.ModePaiement;
import com.tontine.service.Libelles;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Choix du mode de paiement (Wave ou Orange Money, jamais especes) + numero pour le paiement mobile. */
public class ChoixPaiement extends VBox {

    private final ToggleGroup groupe = new ToggleGroup();
    private final ToggleButton wave = bouton(ModePaiement.WAVE);
    private final ToggleButton orange = bouton(ModePaiement.ORANGE_MONEY);
    private final TextField numero = new TextField();
    private final Label etiquetteNumero = Composants.etiquette("Num\u00e9ro Wave");
    private final VBox zoneNumero = new VBox(5, etiquetteNumero, numero);

    public ChoixPaiement() {
        super(10);
        numero.setPromptText("77 000 00 00");
        HBox puces = new HBox(8, wave, orange);
        wave.setSelected(true);
        groupe.selectedToggleProperty().addListener((o, ancien, nouveau) -> {
            if (nouveau == null) {
                ancien.setSelected(true); // un mode doit toujours etre choisi
            } else {
                miseAJour();
            }
        });
        getChildren().addAll(Composants.etiquette("Mode de paiement"), puces, zoneNumero);
        miseAJour();
    }

    private ToggleButton bouton(ModePaiement m) {
        ToggleButton b = new ToggleButton(Libelles.mode(m));
        b.setToggleGroup(groupe);
        b.getStyleClass().add("puce");
        b.setUserData(m);
        return b;
    }

    private void miseAJour() {
        ModePaiement m = getMode();
        etiquetteNumero.setText("Num\u00e9ro " + Libelles.mode(m));
    }

    public ModePaiement getMode() {
        return (ModePaiement) groupe.getSelectedToggle().getUserData();
    }

    public String getNumero() {
        return numero.getText();
    }

    public void setNumero(String n) {
        numero.setText(n);
    }

    public void selectionner(ModePaiement m) {
        (m == ModePaiement.ORANGE_MONEY ? orange : wave).setSelected(true);
    }
}
