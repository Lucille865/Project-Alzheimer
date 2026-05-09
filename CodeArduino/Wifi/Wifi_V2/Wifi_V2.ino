#include <WiFi.h>
#include <HTTPClient.h>

// --- CONFIGURATION RÉSEAU ---
const char* ssid     = "S23 FE de Maxence";
const char* password = "7jfptc984ypn7i8";
const char* serverUrl = "http://10.37.102.230:8081/bouton"; 

// --- PINS ---
const int ledRouge = 1;  
const int ledVerte = 42; 
const int bouton   = 2;   
bool etatPrecedent = HIGH;

void setup() {
  Serial.begin(115200);
  while (!Serial) { delay(10); } // Specifique ESP32-S3
  
  pinMode(ledRouge, OUTPUT);
  pinMode(ledVerte, OUTPUT);
  pinMode(bouton, INPUT_PULLUP);

  // Connexion Wi-Fi
  WiFi.begin(ssid, password);
  Serial.print("Connexion au Wi-Fi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\n[OK] Connecté !");
}

unsigned long dernierCheck = 0;
const long intervalleCheck = 500; // Vérifie l'état toutes les 0.5 seconde

void loop() {
  int etatActuel = digitalRead(bouton);
  unsigned long maintenant = millis();

  // --- CAS 1 : Appui sur le bouton (Action immédiate) ---
  if (etatActuel == LOW && etatPrecedent == HIGH) {
    envoyerRequeteJava("action=valider");
    dernierCheck = maintenant; // On reset le timer pour ne pas ré-envoyer un "check" juste après
    delay(200); // Anti-rebond
  }
  etatPrecedent = etatActuel;

  // --- CAS 2 : Vérification automatique (Toutes les 2s) ---
  if (maintenant - dernierCheck >= intervalleCheck) {
    dernierCheck = maintenant;
    envoyerRequeteJava("action=check");
  }
}

// On déplace la logique dans une fonction réutilisable
void envoyerRequeteJava(String parametre) {
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    String urlComplete = String(serverUrl) + "?" + parametre;
    
    // LOG : On affiche l'envoi
    Serial.print("[WIFI] Envoi requête : ");
    Serial.println(parametre); 

    http.begin(urlComplete); 
    http.addHeader("Connection", "close");
    http.setTimeout(400); 
    
    int httpCode = http.GET();
    
    if (httpCode == 200) {
      String reponse = http.getString();
      Serial.println("[WIFI] Réponse reçue : " + reponse); // LOG : On voit V, R ou X

      if (reponse == "V") {
        digitalWrite(ledVerte, HIGH);
        digitalWrite(ledRouge, LOW);
      } 
      else if (reponse == "R") {
        digitalWrite(ledVerte, LOW);
        digitalWrite(ledRouge, HIGH);
      } 
      else if (reponse == "X") {
        digitalWrite(ledVerte, LOW);
        digitalWrite(ledRouge, LOW);
      }
    } 
    else {
      // LOG : On voit l'erreur (ex: -1)
      Serial.print("[ERREUR] Code HTTP : ");
      Serial.println(httpCode);
    }
    http.end();
  }
}