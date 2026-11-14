package com.tontine.ui;

import java.util.List;

import com.tontine.dao.UtilisateurDao;
import com.tontine.model.Renouvellement;
import com.tontine.model.StatutRenouvellement;
import com.tontine.model.Tontine;
import com.tontine.model.Utilisateur;
import com.tontine.service.AdhesionService;
import com.tontine.service.ConsultationService;
import com.tontine.service.Libelles;
import com.tontine.service.RenouvellementService;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Relance de la tontine apres sa cloture : le gerant envoie une alerte a chaque membre, suit les reponses
 * (valide / refuse) et peut relancer le cycle quand tout le monde a repondu. Un refus retire le membre automatiquement.
 */
public class RenouvellementGerantPane extends PaneGestion {

    private final Utilisateur gerant;
    private final Tontine tontine;
    private final RenouvellementService service = new RenouvellementService();

    public RenouvellementGerantPane(Utilisateur gerant, Tontine tontine) {
        this.gerant = gerant;
        this.tontine = tontine;
        rafraichir();
    }

    @Override
    protected void construire() {
        Tontine t = new ConsultationService().detail(tontine.getId());
        boolean terminee = new AdhesionService().estTerminee(t);
        if (!terminee) {
            getChildren().add(vide("L'alerte de renouvellement se lance quand la tontine est termin\u00e9e (" + Libelles.cycle(t)
                    + " cl\u00f4tur\u00e9). Chaque membre pourra alors valider ou refuser de continuer."));
            return;
        }
        List<Renouvellement> alertes = service.alertes(gerant.getId(), t.getId());
        if (alertes.isEmpty()) {
            getChildren().add(vide("Pour relancer la tontine, envoyez d'abord une alerte \u00e0 chaque membre : celui qui valide continue, "
                    + "celui qui refuse ou qui ne r\u00e9pond pas est retir\u00e9 automatiquement de la tontine."));
            TextField delai = new TextField(String.valueOf(RenouvellementService.DELAI_DEFAUT_HEURES));
            getChildren().add(Composants.champ("D\u00e9lai de r\u00e9ponse (en heures) : pass\u00e9 ce d\u00e9lai, les membres sans r\u00e9ponse sont retir\u00e9s automatiquement", delai));
            Button envoyer = Composants.primaire("Envoyer l'alerte de renouvellement");
            envoyer.setOnAction(e -> action(() -> {
                int heures = Saisie.entier(delai.getText(), "D\u00e9lai de r\u00e9ponse");
                int n = service.lancerAlerte(gerant.getId(), t.getId(), heures);
                succes("L'alerte a \u00e9t\u00e9 envoy\u00e9e \u00e0 " + n + " membre(s). R\u00e9ponse attendue sous " + heures + " h.");
            }));
            getChildren().add(envoyer);
            return;
        }
        long attente = alertes.stream().filter(a -> a.getStatut() == StatutRenouvellement.EN_ATTENTE).count();
        long valides = alertes.stream().filter(a -> a.getStatut() == StatutRenouvellement.VALIDE).count();
        long refus = alertes.stream().filter(a -> a.getStatut() == StatutRenouvellement.REFUSE).count();
        getChildren().add(titreSection("R\u00e9ponses des membres : " + valides + " valid\u00e9(s), " + refus + " refus, " + attente + " en attente"));
        UtilisateurDao users = new UtilisateurDao();
        for (Renouvellement a : alertes) {
            String nom = users.findById(a.getUserId()).getNomComplet();
            Label etat = new Label(a.getStatut() == StatutRenouvellement.VALIDE ? "Continue"
                    : a.getStatut() == StatutRenouvellement.REFUSE ? "Retir\u00e9(e)" : "En attente");
            etat.getStyleClass().add(a.getStatut() == StatutRenouvellement.EN_ATTENTE ? "badge-type" : "badge");
            getChildren().add(ligne(texte(nom, "Alerte envoy\u00e9e le " + Aff.date(a.getDateEnvoi())
                    + (a.getDateLimite() != null && a.getStatut() == StatutRenouvellement.EN_ATTENTE ? " - r\u00e9ponse avant le " + Aff.date(a.getDateLimite()) : "")), etat));
        }
        if (attente > 0) {
            getChildren().add(vide("Sans r\u00e9ponse avant la date limite, un membre est consid\u00e9r\u00e9 comme ne souhaitant plus participer : il est retir\u00e9 automatiquement (sans action de votre part), au plus tard au d\u00e9marrage du " + Libelles.cycle(t) + " suivant."));
            Button retirer = Composants.contour("Retirer maintenant les membres sans r\u00e9ponse");
            retirer.setOnAction(e -> action(() -> {
                if (Nav.confirmer("Retirer de la tontine les " + attente + " membre(s) qui n'ont pas r\u00e9pondu ?")) {
                    int n = service.retirerSansReponse(gerant.getId(), t.getId());
                    succes(n + " membre(s) retir\u00e9(s).");
                }
            }));
            getChildren().add(retirer);
        } else {
            getChildren().add(vide("Tous les membres ont r\u00e9pondu : vous pouvez relancer la tontine."));
            Button relancer = Composants.primaire("Relancer la tontine");
            relancer.setOnAction(e -> action(() -> {
                com.tontine.model.Cycle c = new com.tontine.service.CycleService().demarrerCycle(gerant.getId(), t.getId());
                succes("La tontine est relanc\u00e9e : " + Libelles.cycle(t) + " " + c.getNumero() + " d\u00e9marr\u00e9. Les membres sont pr\u00e9venus.");
            }));
            getChildren().add(relancer);
        }
    }
}
