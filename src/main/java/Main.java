import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== MENU PRINCIPAL ===");
        System.out.println("1. Lancer l'interface Lucien (version simple)");
        System.out.println("2. Lancer l'interface Lucien (version complète)");
        System.out.println("3. Tester le backend en console");
        System.out.print("Choix : ");

        int choix = scanner.nextInt();

        switch (choix) {
            case 1:
                InterfaceLucienSimple.launch(InterfaceLucienSimple.class, args);
                break;
            case 2:
                InterfaceLucien.launch(InterfaceLucien.class, args);
                break;
            case 3:
                testBackendConsole();
                break;
            default:
                System.out.println("Choix invalide");
        }

        scanner.close();
    }

    private static void testBackendConsole() {
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