#include <WiFi.h>                  // биолиотека для включения wifi
#include <IOXhop_FirebaseESP32.h>  // библиотека БД
#include <Servo.h>                 // библиотека моторчика

#define FIREBASE_Host "https://smarthouse-database-bc26d-default-rtdb.firebaseio.com/"  // ссылка на БД
#define FIREBASE_authorization_key "JWt0XnE2t0fTPFdZVfHSr15jNcjtp5VamC1ZEOZV"           // ключ к БД
const char* ssid = "Redmi Note 10S";                                                    // Название точки доступа
const char* password = "qwerasdf";                                                      // Пароль

Servo myservo;           // переменная под мотор
String fireStatus = "";  // LED State
int led_street = 18;     // свет улица
int led_home = 17;       // свет дом
int MQ2 = A7;            // газ
int SoilM = A4;          // Почва
int AirM = A5;           // Влажность вохдуха
int Svet = A6;           // Датчик света
int Dvizh = 4;           // Моторчик
int val = 0;             // значения датчиков

void setup() {

  // Включение всех датчиков
  pinMode(MQ2, INPUT);
  myservo.attach(15);
  Serial.begin(115200);
  pinMode(SoilM, INPUT);
  pinMode(AirM, INPUT);
  pinMode(Svet, INPUT);
  pinMode(Dvizh, INPUT);
  delay(500);
  pinMode(led_street, OUTPUT);
  pinMode(led_home, OUTPUT);
  WiFi.begin(ssid, password);

  // Подключение к wifi
  Serial.print("Connecting to WIFI");
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(500);
  }
  Serial.println();
  Serial.print("Connected to WIFI!");
  Serial.println();
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());

  // Подключение к БД
  Firebase.begin(FIREBASE_Host, FIREBASE_authorization_key);
  myservo.write(0);
}

uint32_t sec = millis() / 1000ul; // переменная под время

void loop() {
  if (millis() / 1000ul - sec >= 7) { // раз в 7 секунд отправка показаний
    datchiki();
    sec = millis() / 1000ul;
  }

  sob(); // обработчик событий
}

void sob() {
  fireStatus = Firebase.getString("PowerHomeLight"); // Домашнее освещение
  if (fireStatus == "true") {
    digitalWrite(led_home, HIGH); // Вкл
  } else if (fireStatus == "false") {
    digitalWrite(led_home, LOW); // Выкл
  }

  window(); // Обработка окна

  String fireStat = "";
  fireStat = Firebase.getString("ActionMode"); // Режим освещения
  int x = 0;
  if (fireStat == "Switch") {
    x = 1;
  } else if (fireStat == "LevelLighting") {
    x = 2;
  } else if (fireStat == "MotionSensor") {
    x = 3;
  }
  switch (x) {

    case 1:  // Переключатель
      fireStatus = Firebase.getString("PowerStreetLight");
      if (fireStatus == "true") {
        digitalWrite(led_street, HIGH);
      } else if (fireStatus == "false") {
        digitalWrite(led_street, LOW);
      } else {
        Serial.println("Wrong Credential! Please send ON/OFF");
      }
      break;

    case 2: // По датчику освещения
      val = analogRead(Svet);
      if (val < 400) {
        Serial.println(val);
        digitalWrite(led_street, HIGH);
      } else {
        digitalWrite(led_street, LOW);
      }
      break;

    case 3: // По датчику движения
      if (digitalRead(Dvizh) == 1) {
        digitalWrite(led_street, HIGH);  // LED light is on
      } else {
        digitalWrite(led_street, LOW);  // LED light is not on
      }
      break;
  }
}

int pos = 0;
void window() { // Обработка окна
  fireStatus = Firebase.getString("WindowPower");
  if (fireStatus == "false" && pos != 0) { // Закрытие окна
    myservo.write(0);
    pos = 0;
  } else if (fireStatus == "true") {
    fireStatus = Firebase.getString("WindowOpenMode");
    if (fireStatus == "100" && pos != 140) { // Открытие
      myservo.write(140);
      pos = 140;
    }
    if (fireStatus == "50" && pos == 0) { // Среднее положение из закрытого
      myservo.write(70);
      pos = 70;
    }
    if (fireStatus == "50" && pos == 140) { // Среднее положение из открытого
      myservo.write(70);
      pos = 70;
    }
  }
}

void datchiki() { // Отправка показаний датчиков
  val = analogRead(MQ2);  
  Serial.println(val);
  Firebase.setString("Co2", String(val)); // СО2

  val = analogRead(SoilM);
  val = val * 100 / 4096;
  Serial.println(val);
  Firebase.setString("SoilMoisture", String(val)); // Влажность почвы

  val = analogRead(AirM);
  val = val * 100 / 4096;
  Serial.println(val);
  Firebase.setString("AirHumidity", String(val)); // Влажность воздуха
}