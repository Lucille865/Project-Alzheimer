public class Tache {
    String nom;
    String heurePrevue;
    boolean estValidee;

    // Constructeur : crée une tâche avec un nom et une heure
    public Tache(String nom, String heurePrevue) {
        this.nom = nom;
        this.heurePrevue = heurePrevue;
        this.estValidee = false; // Rouge par défaut
    }

    // Méthode pour valider la tâche (Bouton OUI du boîtier)
    public void valider() {
        this.estValidee = true; // Passe au Vert
        System.out.println("Succès : La tâche '" + nom + "' a été validée.");
    }
}
