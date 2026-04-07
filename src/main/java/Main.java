public class Main {
    public static void main(String[] args) {
        // Test console du backend sans lancer l'interface
        TacheManager manager = new TacheManager();

        System.out.println("=== PROGRAMME DE LUCIEN ===");
        for (Tache t : manager.getTaches()) {
            System.out.println(t.getPlageHoraire() + "  |  " + t.getNom());
        }

        System.out.println("\nSimulation : validation du déjeuner");
        manager.getTaches().get(3).valider();
        System.out.println("Progression : " + manager.getNbValidees()
                + " / " + manager.getNbTotal());
    }
}
