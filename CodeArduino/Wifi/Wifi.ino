#include <WiFi.h>
#include <HTTPClient.h>

// --- CONFIGURATION RÉSEAU ---
const char* ssid     = "SFR_4FCF";      // Ton WiFi ou ton Partage de connexion
const char* password = "g4ikkb3mq9l7ip4ut9ev";   // Ton mot de passe
const char* serverUrl = "http://192.168.1.168:8080/bouton"; // REMPLACE PAR TON IP

// --- CONFIGURATION DES PINS ---
const int ledRouge = 1;  
const int ledVerte = 42; 
const int bouton = 2;  

void setup() {
  // 1. D'abord on initialise la vitesse (OBLIGATOIRE EN PREMIER)
  Serial.begin(115200);

  // 2. On attend VRAIMENT que le port USB soit prêt (crucial pour S3)
  while (!Serial) { 
    delay(100); 
  } 

  // 3. On attend encore un peu pour être sûr que le tampon est vide
  delay(1000);

  // 4. MAINTENANT on peut écrire
  Serial.println("\n--- SYSTEME PRET ---");

  // Configuration des composants
  pinMode(bouton, INPUT_PULLUP);
  pinMode(ledVerte, OUTPUT);
  pinMode(ledRouge, OUTPUT);

  // Initialisation : on éteint tout au début
  digitalWrite(ledVerte, LOW);
  digitalWrite(ledRouge, LOW);

  // Connexion au Wi-Fi
  Serial.print("Connexion au Wi-Fi : ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("\n[OK] Wi-Fi connecte !");
  Serial.print("Adresse IP de l'ESP32 : ");
  Serial.println(WiFi.localIP());
}

void loop() {
  // Détection de l'appui sur le bouton
  if (digitalRead(bouton) == LOW) { 
    Serial.println("\n[ACTION] Bouton presse ! Appel au serveur Java...");

    if (WiFi.status() == WL_CONNECTED) {
      HTTPClient http;
      
      http.begin(serverUrl);
      int httpCode = http.GET();

      if (httpCode == 200) {
        String reponse = http.getString(); // Récupère "V" ou "R"
        Serial.print("[JAVA] Ordre recu : ");
        Serial.println(reponse);

        if (reponse == "V") {
          digitalWrite(ledVerte, HIGH);
          digitalWrite(ledRouge, LOW);
          Serial.println("-> LED VERTE ALLUMEE");
        } 
        else if (reponse == "R") {
          digitalWrite(ledVerte, LOW);
          digitalWrite(ledRouge, HIGH);
          Serial.println("-> LED ROUGE ALLUMEE");
        }
      } 
      else {
        Serial.print("[ERREUR HTTP] Code : ");
        Serial.println(httpCode);
      }
      http.end();
    } 
    else {
      Serial.println("[ERREUR] Wi-Fi perdu !");
    }

    delay(1000); // Anti-rebond
  }
}