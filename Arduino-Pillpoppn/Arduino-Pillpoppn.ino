#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include "addons/TokenHelper.h"
#include "addons/RTDBHelper.h"
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <ESP32Servo.h>
// Wi-Fi credentials and Firebase setup
#define WIFI_SSID "**********"
#define WIFI_PASSWORD "**********"
#define API_KEY "**********"
#define DATABASE_URL "**********"
#define BUTTON_DEBOUNCE_DELAY 50
// Phototransistor pins for each pill sensor
const int photoPins[] = {34, 35, 33, 32};
const int buzzerPin = 26; // Buzzer pin
const int buttonPin = 25; // Button pin for refilling mode

// Variables to track pill counts and previous states
volatile unsigned long pillCounts[4] = {0};
volatile int previousStates[4] = {LOW, LOW, LOW, LOW};

// Firebase variables
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

bool signupOK = false;
bool pillUpdated[4] = {false, false, false, false};
// Define servo motors for each pill container
Servo pillServos[4];
const int servoPins[4] = {12, 13, 16, 15}; // Specify the pins for each servo motor
// Variables for buzzer timing
unsigned long lastBuzzTime = 0;
const unsigned long buzzDuration = 200; // Shorter duration (200 milliseconds)

// Variables for refilling mode
bool refillingMode = false;
unsigned long buttonPressTime = 0;

// Alarm storage arrays
String pill1Alarms[10];
String pill2Alarms[10];
String pill3Alarms[10];
String pill4Alarms[10];
int alarmCount[4] = {0}; // Track alarm count for each pill container

// Header names for each pill container
String pillHeaders[4]; // To store header names from Firebase

LiquidCrystal_I2C lcd(0x27, 16, 2); // Initialize LCD with I2C address 0x27

void operateServo(int pillIndex) {
    // Sound the buzzer
    digitalWrite(buzzerPin, HIGH); // Play a tone of 1000 Hz
    pillServos[pillIndex].write(60);    // Open position
    delay(1000);                        // Keep open for 1 second
    pillServos[pillIndex].write(0);     // Close position
    delay(500);                         // Brief delay for stability
    digitalWrite(buzzerPin, LOW);               // Stop the buzzer sound
}

void IRAM_ATTR handleButtonPress() {
    // Toggle refilling mode on each press
    refillingMode = !refillingMode;
    if (refillingMode) {
        lcd.clear();
        lcd.print("Refilling Pills");
    } else {
        lcd.clear();
        lcd.print("Pill Poppin");
    }
}
// Function to fetch and store alarms in a specific array for each pill container
void fetchAndStoreAlarms(const String &pillPath, int pillIndex) {
    if (Firebase.RTDB.get(&fbdo, pillPath)) {
        if (fbdo.dataType() == "json") {
            FirebaseJson &json = fbdo.jsonObject();
            size_t count = json.iteratorBegin();
            Serial.printf("Alarms for %s:\n", pillPath.c_str());

            // Reset the specific alarm count
            alarmCount[pillIndex] = 0;

            // Store alarms in corresponding array based on pillIndex
            for (size_t i = 0; i < count; i++) {
                String key, value;
                int type;
                json.iteratorGet(i, type, key, value);

                if (pillIndex == 0 && alarmCount[0] < 10) {
                    pill1Alarms[alarmCount[0]++] = value;
                } else if (pillIndex == 1 && alarmCount[1] < 10) {
                    pill2Alarms[alarmCount[1]++] = value;
                } else if (pillIndex == 2 && alarmCount[2] < 10) {
                    pill3Alarms[alarmCount[2]++] = value;
                } else if (pillIndex == 3 && alarmCount[3] < 10) {
                    pill4Alarms[alarmCount[3]++] = value;
                }

                Serial.printf(" - Alarm ID: %s, Time: %s\n", key.c_str(), value.c_str());
            }
            json.iteratorEnd();
        } else {
            Serial.printf("No alarms found for %s or wrong data type.\n", pillPath.c_str());
        }
    } else {
        Serial.printf("Failed to get alarms for %s: %s\n", pillPath.c_str(), fbdo.errorReason().c_str());
    }
}


// Function to check and operate pill dispenser based on dispense status
void checkPillDispense(const String &pillPath, int pillIndex) {
    if (Firebase.RTDB.get(&fbdo, pillPath)) {
        if (fbdo.dataType() == "int") {
            int dispenseStatus = fbdo.intData();
            if (dispenseStatus == 1) {
                Serial.printf("Pill dispensed for %s\n", pillHeaders[pillIndex].c_str());

                // Operate the servo for this pill container
                operateServo(pillIndex);

                // Reset dispense status to 0
                if (Firebase.RTDB.setInt(&fbdo, pillPath, 0)) {
                    Serial.printf("Dispense status for %s updated to 0\n", pillPath.c_str());
                } else {
                    Serial.printf("Failed to update dispense status for %s: %s\n", pillPath.c_str(), fbdo.errorReason().c_str());
                }
            }
        } else {
            Serial.printf("No dispense status found for %s or wrong data type.\n", pillPath.c_str());
        }
    } else {
        Serial.printf("Failed to get dispense status for %s: %s\n", pillPath.c_str(), fbdo.errorReason().c_str());
    }
}

void fetchHeaderNames() {
    String headerPaths[4] = {"/headers/pill1", "/headers/pill2", "/headers/pill3", "/headers/pill4"};
    for (int i = 0; i < 4; i++) {
        if (Firebase.RTDB.get(&fbdo, headerPaths[i])) {
            if (fbdo.dataType() == "string") {
                pillHeaders[i] = fbdo.stringData();
                Serial.printf("Header for Pill %d: %s\n", i + 1, pillHeaders[i].c_str());
            } else {
                Serial.printf("Failed to get header for Pill %d or wrong data type.\n", i + 1);
            }
        } else {
            Serial.printf("Failed to get header for Pill %d: %s\n", i + 1, fbdo.errorReason().c_str());
        }
    }
}

void printStoredAlarms() {
    Serial.println("\nStored Alarms:");
    for (int i = 0; i < alarmCount[0]; i++) {
        Serial.printf("%s Alarm %d: %s\n", pillHeaders[0].c_str(), i + 1, pill1Alarms[i].c_str());
    }
    for (int i = 0; i < alarmCount[1]; i++) {
        Serial.printf("%s Alarm %d: %s\n", pillHeaders[1].c_str(), i + 1, pill2Alarms[i].c_str());
    }
    for (int i = 0; i < alarmCount[2]; i++) {
        Serial.printf("%s Alarm %d: %s\n", pillHeaders[2].c_str(), i + 1, pill3Alarms[i].c_str());
    }
    for (int i = 0; i < alarmCount[3]; i++) {
        Serial.printf("%s Alarm %d: %s\n", pillHeaders[3].c_str(), i + 1, pill4Alarms[i].c_str());
    }
    Serial.println();
}

// Interrupt service routines for each phototransistor
void IRAM_ATTR photoInterrupt(int index) {
    int currentState = digitalRead(photoPins[index]);
    if (currentState != previousStates[index]) {
        if (currentState == HIGH) {
            pillUpdated[index] = true;  // Mark that the pill was detected
            digitalWrite(buzzerPin, HIGH); // Turn on buzzer immediately
            lastBuzzTime = millis();       // Start the timer for turning it off
        }
        previousStates[index] = currentState;
    }
}

void IRAM_ATTR photoInterrupt1() { photoInterrupt(0); }
void IRAM_ATTR photoInterrupt2() { photoInterrupt(1); }
void IRAM_ATTR photoInterrupt3() { photoInterrupt(2); }
void IRAM_ATTR photoInterrupt4() { photoInterrupt(3); }

void setup() {
    Serial.begin(115200);

    // Connect to Wi-Fi
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    Serial.print("Connecting to Wi-Fi");
    while (WiFi.status() != WL_CONNECTED) {
        Serial.print(".");
        delay(300);
    }
    Serial.println();
    Serial.print("Connected with IP: ");
    Serial.println(WiFi.localIP());
    Serial.println();

    // Firebase configuration
    config.api_key = API_KEY;
    config.database_url = DATABASE_URL;
    if (Firebase.signUp(&config, &auth, "", "")) {
        Serial.println("Sign-up OK");
        signupOK = true;
    } else {
        Serial.printf("Sign-up error: %s\n", config.signer.signupError.message.c_str());
    }

    config.token_status_callback = tokenStatusCallback; // Monitor token generation
    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);

     // Initialize and attach servo motors
    for (int i = 0; i < 4; i++) {
        pillServos[i].setPeriodHertz(50);
        pillServos[i].attach(servoPins[i], 500, 2400);
        pillServos[i].write(0);
    }

    // Pin setup
    pinMode(buzzerPin, OUTPUT); // Set buzzer pin as OUTPUT
    pinMode(buttonPin, INPUT_PULLUP); // Set button pin as INPUT with pull-up

    for (int i = 0; i < 4; i++) {
        pinMode(photoPins[i], INPUT);
    }

    // Attach interrupts for each sensor
    attachInterrupt(digitalPinToInterrupt(photoPins[0]), photoInterrupt1, CHANGE);
    attachInterrupt(digitalPinToInterrupt(photoPins[1]), photoInterrupt2, CHANGE);
    attachInterrupt(digitalPinToInterrupt(photoPins[2]), photoInterrupt3, CHANGE);
    attachInterrupt(digitalPinToInterrupt(photoPins[3]), photoInterrupt4, CHANGE);

    // Initialize LCD
    lcd.init();
    lcd.backlight();
    lcd.print("Pill Poppin");

        fetchHeaderNames();
}
// Declare pillQuantity array to store pill quantities
int pillQuantity[4] = {0, 0, 0, 0};
void loop() {
    // Button debounce logic
    static bool lastButtonState = HIGH;
    bool currentButtonState = digitalRead(buttonPin);

    if (currentButtonState == LOW && lastButtonState == HIGH) {
        // Button pressed
        buttonPressTime = millis();
    } else if (currentButtonState == HIGH && lastButtonState == LOW) {
        // Button released
        if (millis() - buttonPressTime > 200) { // Simple debounce check
            refillingMode = !refillingMode; // Toggle refilling mode
            if (refillingMode) {
                lcd.clear();
                lcd.print("Refilling Pills");
            } else {
                lcd.clear();
                lcd.print("Pill Poppin");
            }
        }
    }
    lastButtonState = currentButtonState;

    // Check if refilling mode is off and display "Pill Poppin" on Serial Monitor
    if (!refillingMode) {
        if (Firebase.ready() && signupOK) {
            // Periodically fetch the updated header names from Firebase
            fetchHeaderNames();

            // Display each pill header and quantity on the LCD sequentially
            for (int i = 0; i < 4; i++) {
                String path = "/pills/pill" + String(i + 1); // Path for pill1, pill2, etc.

                // Fetch the current count from Firebase
                if (Firebase.RTDB.getInt(&fbdo, path)) {
                    pillQuantity[i] = fbdo.intData(); // Update pillQuantity with the latest count
                } else {
                    Serial.printf("Failed to read pill %d count: %s\n", i + 1, fbdo.errorReason().c_str());
                    pillQuantity[i] = 0; // Default to 0 if read fails
                }
                // Check dispense status and operate servo if needed
                checkPillDispense("/dispense/pill1", 0);
                checkPillDispense("/dispense/pill2", 1);
                checkPillDispense("/dispense/pill3", 2);
                checkPillDispense("/dispense/pill4", 3);

                lcd.clear();                 // Clear the LCD screen
                lcd.setCursor(0, 0);         // Set cursor to the first line
                lcd.print("Container:");     // Display static text
                lcd.setCursor(0, 1);         // Set cursor to the second line
                lcd.print(pillHeaders[i]);   // Display the header for the pill container
                lcd.print(" Qty: ");           // Display "Qty" label
                lcd.print(pillQuantity[i]);  // Display the pill quantity
                delay(1000);                 // Delay to show each header and quantity for 2 seconds
            }
        }

        delay(500); // Delay between database checks
    } else {
        // Handle the buzzer logic when in refilling mode
        // Ensure to turn on the buzzer based on pill detection
        for (int i = 0; i < 4; i++) {
            if (pillUpdated[i]) { // If the pill was detected
                digitalWrite(buzzerPin, HIGH); // Turn on buzzer
                lastBuzzTime = millis(); // Start the timer for buzzer control
            }
        }

        // Turn off the buzzer after the set duration
        if (digitalRead(buzzerPin) == HIGH && (millis() - lastBuzzTime >= buzzDuration)) {
            digitalWrite(buzzerPin, LOW); // Turn off buzzer
        }

        // Handle pill count updates and Firebase interactions
        for (int i = 0; i < 4; i++) {
            if (pillUpdated[i]) { // Only update if pill was detected
                String path = "/pills/pill" + String(i + 1); // Path for pill1, pill2, etc.

                // Read the current count from Firebase
                if (Firebase.RTDB.getInt(&fbdo, path)) {
                    int currentCount = fbdo.intData(); // Get the current pill count from Firebase
                    pillCounts[i] = currentCount + 1;  // Increment the count
                    pillQuantity[i] = pillCounts[i];   // Update pillQuantity with the current count

                    // Write the updated count back to Firebase
                    if (Firebase.RTDB.setInt(&fbdo, path, pillCounts[i])) {
                        Serial.printf("Pill %d count updated: %d\n", i + 1, pillCounts[i]);
                    } else {
                        Serial.printf("Failed to update pill %d count: %s\n", i + 1, fbdo.errorReason().c_str());
                    }
                } else {
                    Serial.printf("Failed to read pill %d count: %s\n", i + 1, fbdo.errorReason().c_str());
                }

                pillUpdated[i] = false; // Reset the update flag
            }
        }

        delay(100); // Shortened delay for faster loop execution
    }
}
