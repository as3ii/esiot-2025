#include "game_phase.h"
#include "model.h"
#include "waiting_phase.h"
#include <Arduino.h>
#include <HardwareSerial.h>
#include <LiquidCrystal_I2C.h>
#include <stdio.h>

constexpr size_t MAX_LENGTH = 40;

static LiquidCrystal_I2C lcd(LCD_ADDR, LCD_COLS, LCD_ROWS);
static SYS_PHASE currentPhase = WAITING;
static bool isPhaseChanged = true;
static STATUS phaseStatus = OK;

static void (*resetFunc)() = nullptr; // Reset function

void setup() {
  // Led initialization
  pinMode(LED_1, OUTPUT);
  pinMode(LED_2, OUTPUT);
  pinMode(LED_3, OUTPUT);
  pinMode(LED_4, OUTPUT);
  pinMode(LED_S, OUTPUT);

  // Buttons initialization
  pinMode(BTN_1, INPUT);
  pinMode(BTN_2, INPUT);
  pinMode(BTN_3, INPUT);
  pinMode(BTN_4, INPUT);

  // Potentiometer
  pinMode(POT, INPUT);

  // UART - communication with computer
  Serial.begin(UART_SPEED);
  // LCD
  lcd.init();
  lcd.backlight();

  SERIAL_DBG("Hardware initialized");
}

inline static void runPhase() {
  switch (currentPhase) {
    case WAITING:
      phaseStatus = waiting_phase(isPhaseChanged, &lcd);
      break;
    case GAME:
      phaseStatus = game_phase(isPhaseChanged, &lcd);
      break;
  }
}

inline static void checkPhaseSwitch() {
  switch (phaseStatus) {
    case GO_NEXT:
      switch (currentPhase) {
        case WAITING:
          currentPhase = GAME;
          break;
        case GAME:
          currentPhase = WAITING;
          break;
      }
      isPhaseChanged = true;
      break;
    case OK:
      isPhaseChanged = false;
      break;
    case ERR:
      char str[MAX_LENGTH];
      genErrorMsg(str, MAX_LENGTH, currentPhase);
      Serial.println(str);
      Serial.flush();
      resetFunc(); // Soft reset
      break;
  }
}

void loop() {
  runPhase();
  checkPhaseSwitch();
}
