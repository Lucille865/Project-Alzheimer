

public class TacheQuotidienne {
    String nom;          // ex: "Prendre les médicaments"
    boolean estFaite;    // false par défaut (rouge) [cite: 4907]

    // Méthode pour valider la tâche
    public void valider() {
        this.estFaite = true; // passe au vert [cite: 4907]
        System.out.println("Tâche validée !");
    }
}

void main() {
    //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
    // to see how IntelliJ IDEA suggests fixing it.
    IO.println(String.format("Hello and welcome!"));

    for (int i = 1; i <= 5; i++) {
        //TIP Press <shortcut actionId="Debug"/> to start debugging your code. We have set one <icon src="AllIcons.Debugger.Db_set_breakpoint"/> breakpoint
        // for you, but you can always add more by pressing <shortcut actionId="ToggleLineBreakpoint"/>.
        IO.println("i = " + i);
    }
}
