const int ledRouge = 1;  
const int ledVerte = 42; 
const int bouton = 2;    
bool etatPrecedent = HIGH;

void setup() {
  Serial.begin(115200);
  pinMode(ledRouge, OUTPUT);
  pinMode(ledVerte, OUTPUT);
  pinMode(bouton, INPUT_PULLUP);
}

void loop() {
  // 1. Écouter les ordres de JavaFX (pour l'état des LEDs)
  if (Serial.available() > 0) {
    char cmd = Serial.read();
    if (cmd == 'V') { digitalWrite(ledVerte, HIGH); digitalWrite(ledRouge, LOW); }
    if (cmd == 'R') { digitalWrite(ledVerte, LOW); digitalWrite(ledRouge, HIGH); }
    if (cmd == 'X') { digitalWrite(ledVerte, LOW); digitalWrite(ledRouge, LOW); }
  }

  // 2. Envoyer l'appui à JavaFX
  int etatActuel = digitalRead(bouton);
  if (etatActuel == LOW && etatPrecedent == HIGH) {
    Serial.println("BTN_ROUGE");
    delay(50); // Anti-rebond
  }
  etatPrecedent = etatActuel;
}