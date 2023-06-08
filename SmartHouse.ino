#include <WiFi.h>
#include <IOXhop_FirebaseESP32.h>
#include <Servo.h>

#define FIREBASE_Host "https://smarthouse-database-bc26d-default-rtdb.firebaseio.com/"  // replace with your Firebase Host
#define FIREBASE_authorization_key "JWt0XnE2t0fTPFdZVfHSr15jNcjtp5VamC1ZEOZV"           // replace with your secret key
const char* ssid = "Redmi Note 10S";
const char* password =  "qwerasdf";

Servo myservo; // переменная под мотор
String fireStatus = "";  // LED State
int led_street = 18; // свет улица
int led_home = 17;     // свет дом
int MQ2 = A7;      // газ
int SoilM = A4;   // Почва
int AirM = A5; // Влажность вохдуха
int Svet = A6; // Датчик света
int Dvizh = 4;
int val = 0;             // значения датчиков

void setup() {
  pinMode(MQ2, INPUT);
  myservo.attach(15);
  Serial.begin(115200);
  pinMode (SoilM, INPUT);
  pinMode (AirM, INPUT);
  pinMode (Svet, INPUT);
  pinMode (Dvizh, INPUT); // Define PIR as input in D2
  delay(500);
  pinMode(led_street, OUTPUT);
  pinMode(led_home, OUTPUT);
  WiFi.begin(ssid, password);
  Serial.print("Connecting to WIFI");
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(500);
  }

  Serial.println();
  Serial.print("Connected to WIFI!");
  Serial.println();
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());  //send initial string of led status

  Firebase.begin(FIREBASE_Host, FIREBASE_authorization_key);
  myservo.write(0);
}

uint32_t sec = millis() / 1000ul;

void loop() {
  if (millis() / 1000ul - sec >= 7) {
    datchiki();
    sec = millis() / 1000ul;
  }

  sob();
}

void sob(){
  fireStatus = Firebase.getString("PowerHomeLight");
  if (fireStatus == "true") {  // compare the input of led status received from firebase
    digitalWrite(led_home, HIGH);  // make output led ON
  } else if (fireStatus == "false") {  // compare the input of led status received from firebase
    digitalWrite(led_home, LOW);  // make output led OFF
  }
  // Switch 1
  // LevelLighting 2
  // MotionSensor 3

  window();
  
  String fireStat = "";
  fireStat = Firebase.getString("ActionMode");
  int x = 0;
  if (fireStat == "Switch"){
    x = 1;
  } else if (fireStat == "LevelLighting"){
    x = 2;
  } else if (fireStat == "MotionSensor"){
    x = 3;
  }
  switch (x){
    case 1:
      fireStatus = Firebase.getString("PowerStreetLight");
      if (fireStatus == "true") {  // compare the input of led status received from firebase
        digitalWrite(led_street, HIGH);  // make output led ON
      } else if (fireStatus == "false") {  // compare the input of led status received from firebase
        digitalWrite(led_street, LOW);  // make output led OFF
      } else {
        Serial.println("Wrong Credential! Please send ON/OFF");
      }
    break;
    case 2:
      val = analogRead (Svet);
      if (val < 400){
        Serial.println(val);
        digitalWrite(led_street, HIGH);
      } else {
        digitalWrite(led_street, LOW);
      }
    break;
    case 3:
      if (digitalRead (Dvizh) == 1) {
        digitalWrite (led_street, HIGH); // LED light is on
      } else {
       digitalWrite (led_street, LOW); // LED light is not on
      }
    break;
  }
}

int pos = 0;
void window() {
  fireStatus = Firebase.getString("WindowPower");
  if (fireStatus == "false" && pos != 0){
    // for (int i = pos; i >= pos; i--){
    //   myservo.write(i);
    //   delay(10);
    // }
    myservo.write(0);
    pos = 0;
  } else if (fireStatus == "true"){
    fireStatus = Firebase.getString("WindowOpenMode");
    if (fireStatus == "100" && pos != 140){
      myservo.write(140);
      pos = 140;
    }
    if (fireStatus == "50" && pos == 0){
      myservo.write(70);
      pos = 70;
    }
    if (fireStatus == "50" && pos == 140){
      myservo.write(70);
      pos = 70;
    }
  }
}

void datchiki() {
  val = analogRead(MQ2);  // Read the voltage value of A0 port and assign it to val
  Serial.println(val);
  Firebase.setString("Co2", String(val));

  val = analogRead(SoilM);
  Serial.println(val);
  Firebase.setString("SoilMoisture", String(val));

  val = analogRead(AirM);
  Serial.println(val);
  Firebase.setString("AirHumidity", String(val));
}