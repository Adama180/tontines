package com.tontine.ui;

import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/** Formulaire d'identite d'un visiteur. Deux colonnes si la place le permet, une seule sinon. */
public class FormIdentite extends FlowPane {

    private final TextField nom = new TextField();
    private final TextField prenom = new TextField();
    private final TextField email = new TextField();
    private final TextField telephone = new TextField();
    private final PasswordField motDePasse = new PasswordField();
    private final PasswordField confirmation = new PasswordField();

    public FormIdentite() {
        super(16, 14);
        email.setPromptText("exemple@mail.com");
        telephone.setPromptText("77 123 45 67 (S\u00e9n\u00e9gal, +221)");
        motDePasse.setPromptText("6 caract\u00e8res minimum");
        getChildren().addAll(
                colonne("Nom", nom), colonne("Pr\u00e9nom", prenom),
                colonne("Email", email), colonne("T\u00e9l\u00e9phone", telephone),
                colonne("Mot de passe", motDePasse), colonne("Confirmer le mot de passe", confirmation));
    }

    private VBox colonne(String libelle, javafx.scene.Node saisie) {
        VBox v = Composants.champ(libelle, saisie);
        v.setPrefWidth(290);
        v.setMinWidth(200);
        return v;
    }

    public String getNom() { return nom.getText(); }
    public String getPrenom() { return prenom.getText(); }
    public String getEmail() { return email.getText(); }
    public String getTelephone() { return telephone.getText(); }
    public String getMotDePasse() { return motDePasse.getText(); }

    /** Retourne un message d'erreur si les deux mots de passe different, sinon null. */
    public String erreurConfirmation() {
        return motDePasse.getText().equals(confirmation.getText()) ? null
                : "Les deux mots de passe ne sont pas identiques.";
    }

    /** Pre-remplit le formulaire (utile pour les tests automatiques). */
    public void remplir(String n, String p, String e, String t, String mdp) {
        nom.setText(n);
        prenom.setText(p);
        email.setText(e);
        telephone.setText(t);
        motDePasse.setText(mdp);
        confirmation.setText(mdp);
    }
}
