#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

BLEServer *pServer = NULL;
BLECharacteristic *pTxCharacteristic;
bool deviceConnected = false;

// UUIDs standards pour le service UART (Nordic)
#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

const int PIN_BOUTON = 5; // À vérifier selon votre branchement Grove (souvent D5)

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) { deviceConnected = true; };
    void onDisconnect(BLEServer* pServer) { deviceConnected = false; }
};

void setup() {
  Serial.begin(115200);
  pinMode(PIN_BOUTON, INPUT_PULLUP);

  // Initialisation BLE
  BLEDevice::init("Lucien_Bouton");
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService *pService = pServer->createService(SERVICE_UUID);
  pTxCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_TX, BLECharacteristic::PROPERTY_NOTIFY);
  pTxCharacteristic->addDescriptor(new BLE2902());
  
  pService->start();
  pServer->getAdvertising()->start();
  Serial.println("En attente de connexion Bluetooth...");
}

void loop() {
  // Détection du bouton
  if (digitalRead(PIN_BOUTON) == LOW) { // Si bouton pressé
    if (deviceConnected) {
      pTxCharacteristic->setValue("BTN_ROUGE");
      pTxCharacteristic->notify();
      Serial.println("Signal envoyé : BTN_ROUGE");
      delay(500); // Anti-rebond
    }
  }
}