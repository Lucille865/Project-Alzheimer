import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        // Création de la liste des tâches quotidiennes (FP1)
        ArrayList<Tache> journeelucien = new ArrayList<>();

        journeelucien.add(new Tache("Prendre le petit-déjeuner", "08:00"));
        journeelucien.add(new Tache("Boire un verre d'eau", "10:30"));
        journeelucien.add(new Tache("Manger le déjeuner", "12:30"));

        // Simulation de l'affichage pour Lucien
        System.out.println("--- TABLEAU DE BORD DE LUCIEN ---");
        for (Tache t : journeelucien) {
            String statut = t.estValidee ? "[VERT - FAIT]" : "[ROUGE - A FAIRE]";
            System.out.println(t.heurePrevue + " : " + t.nom + " " + statut);
        }

        // Simulation d'un appui sur le bouton physique "OUI" (FS2)
        System.out.println("\n[Action] Lucien appuie sur OUI pour le déjeuner...");
        journeelucien.get(2).valider();

        // Mise à jour de l'affichage
        System.out.println("\n--- MISE À JOUR DU STATUT ---");
        System.out.println(journeelucien.get(2).nom + " est maintenant : " +
                (journeelucien.get(2).estValidee ? "VERT" : "ROUGE"));
    }
}