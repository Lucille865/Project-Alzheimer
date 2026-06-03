# 📱 MémoGuide - Assistant mémoire pour personnes âgées

[![Java](https://img.shields.io/badge/Java-21%2B-blue.svg)](https://www.java.com/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-green.svg)](https://openjfx.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**MémoGuide** est une application Java conçue pour aider les personnes âgées souffrant de troubles de la mémoire (Alzheimer), à suivre leur routine quotidienne.

---

## Aperçu de l'application

*(à venir - ajoute ici une capture d'écran de l'interface)*

---

## Fonctionnalités

### Interface tablette (pour la personne âgée)
* **Affichage plein écran** adapté aux tablettes
* **Code couleur** selon l'urgence : Orange = à faire, Rouge = urgent, Vert = accompli
* **Rappels sonores** : (activation, rappel 30min)
* **Raccourcis clavier** :
  - `ENTRÉE` = Valider la tâche
  - `←` / `→` = Avancer/reculer de 15 min (simulation)
  - `↑` = Réinitialiser l'heure
  - `F1` ou `A` = Afficher l'aide
* **Bouton d'aide** avec explications vocales

### Tableau de bord web (pour l'entourage)
* **Statistiques en temps réel**
* **Historique des 7 derniers jours**
* **Liste des tâches validées/non validées** avec leur heure de validation
* **Ajouter / Modifier / Supprimer** des tâches
* **Rafraîchissement automatique toutes les 10 secondes**

---

## Architecture du projet

* `InterfaceLucienSimple.java` : Interface principale
* `DashboardServeur.java` : Serveur HTTP embarqué
* `TacheManager.java` : Gestion des tâches + JSON

---

## Installation et lancement

### Prérequis

- **Java 21** ou supérieur
- **JavaFX 21** (inclus dans les dépendances Maven)
- **Maven** (optionnel, mais recommandé)

### Avec Maven (recommandé)

### 1. Cloner le projet
```bash
git clone https://github.com/votre-compte/MemoGuide.git
cd MemoGuide
```

### 2. Compiler le projet
```bash
mvn clean compile
```

### 3. Exécuter l'application
```bash
mvn exec:java -Dexec.mainClass="InterfaceLucienSimple"
```

### Sans Maven

### 1. Compiler
```bash
javac --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.media,javafx.fxml src/main/java/*.java
```

### 2. Exécuter
```bash
java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.media,javafx.fxml InterfaceLucienSimple
```

---

## Configuration

Le fichier `taches.json` est automatiquement créé avec les tâches suivantes :

| Tâche | Horaire |
|:-------------------------------|:------------:|
| Prendre le petit-déjeuner      | 07:00 - 10:30 |
| Se laver les dents             | 09:00 - 10:00 |
| Faire de l'exercice physique   | 10:30 - 12:00 |
| Déjeuner                       | 12:00 - 14:30 |
| Faire une sieste / Repos       | 14:00 - 15:30 |
| Boire un verre d'eau           | 16:00 - 17:00 |
| Prendre sa douche              | 18:00 - 20:00 |
| Dîner                          | 19:30 - 21:30 |

---

## Tableau de bord web

Une fois l'application lancée, le tableau de bord est accessible à l'adresse :
```bash
http://localhost:8082
```

---

## Technologies utilisées

| Technologie | Version | Utilisation |
|:---------------------|:--------:|:--------------------:|
|Java                  |   21+	  | Langage principal  |
|JavaFX	               |   21	    | Interface graphique  |
|com.sun.net.httpserver|	  -	    | Serveur HTTP embarqué  |
|Gson	                 |  2.10.1	| Parsing JSON |
|Maven	               |   3.8+	  | Gestion des dépendances|

---

## Licence

Ce projet est sous licence **MIT**

---

<div align="center"> <br> <b>💙 Développé pour Lucien et tous les seniors</b> <br><br> <sub>© 2026 MémoGuide</sub> </div> ```
