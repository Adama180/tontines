package com.tontine.ui;

import java.util.List;

import com.tontine.model.Utilisateur;
import com.tontine.service.AdminService;
import com.tontine.service.Format;
import com.tontine.service.LigneVersement;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/** Tous les versements (cagnottes) effectues sur la plateforme. */
public class VersementsAdminPane extends PaneGestion {

    private final Utilisateur admin;
    private TableView<LigneVersement> table;

    public VersementsAdminPane(Utilisateur admin) {
        this.admin = admin;
        rafraichir();
    }

    @Override
    protected void construire() {
        List<LigneVersement> lignes = new AdminService().versementsDetailles(admin.getId());
        double total = 0;
        for (LigneVersement l : lignes) {
            total += l.getMontant();
        }
        Label resume = new Label(lignes.size() + " versement(s) - total vers\u00e9 : " + Format.montant(total));
        resume.getStyleClass().add("carte-titre");
        getChildren().add(resume);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Aucun versement pour le moment."));
        table.setPrefHeight(480);
        table.getColumns().add(colonne("Date", l -> Aff.date(l.getDate())));
        table.getColumns().add(colonne("Tontine", LigneVersement::getTontine));
        table.getColumns().add(colonne("Cycle / tour", l -> String.valueOf(l.getNumeroCycle())));
        table.getColumns().add(colonne("B\u00e9n\u00e9ficiaire", LigneVersement::getBeneficiaire));
        table.getColumns().add(colonne("Montant", l -> Format.montant(l.getMontant())));
        table.getItems().addAll(lignes);
        getChildren().add(table);
    }

    private TableColumn<LigneVersement, String> colonne(String titre, java.util.function.Function<LigneVersement, String> valeur) {
        TableColumn<LigneVersement, String> c = new TableColumn<>(titre);
        c.setCellValueFactory(cell -> new SimpleStringProperty(valeur.apply(cell.getValue())));
        c.setSortable(false);
        return c;
    }

    // acces pour les tests automatiques
    TableView<LigneVersement> getTable() { return table; }
}
