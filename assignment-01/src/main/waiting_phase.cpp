#include "waiting_phase.h"
#include "Arduino.h"
#include "model.h"
#include <EnableInterrupt.h>
#include <LiquidCrystal_I2C.h>
#include <avr/io.h>
#include <avr/sleep.h>
#include <stdint.h>

constexpr unsigned long waitBeforeSleep = 10000; // Time in ms
constexpr uint8_t pwmAbsIncrement = 5;
constexpr uint8_t pwmHighThreshold = 250; // Must be <= (255-pwmAbsIncrement)
constexpr uint8_t pwmLowThreshold = 5;    // Must be >= pwmAbsIncrement
constexpr uint8_t wait = 20;

static const uint8_t btn = buttons[0];

static unsigned long startTime = 0;
static unsigned long currentTime = 0;
static volatile bool go_next_phase = false;
static uint8_t pwmValue = UINT8_MAX / 2;
static uint8_t pwmIncr = pwmAbsIncrement; // Increment for PWM

static LiquidCrystal_I2C* lcd;

static void wakeUp() { disableInterrupt(btn); }

static void next() {
  go_next_phase = true;

  disableInterrupt(btn); // Disable interrupt
}

static void sleep() {
  digitalWrite(LED_S, LOW); // Power off led to save power
  lcd->noBacklight();
  disableInterrupt(btn);                 // Disable old interrupt
  enableInterrupt(btn, wakeUp, FALLING); // Enable interrupt to handle wake up
  set_sleep_mode(SLEEP_MODE_PWR_DOWN);   // Set deep sleep
  sleep_mode();                          // Enter sleep mode
}

static void waiting_init() {
  // Switch off leds 1..4
  for (const uint8_t led : leds) {
    digitalWrite(led, LOW);
  }

  go_next_phase = false;

  lcd->clear();
  lcd->setCursor(0, 0);
  lcd->print("Welcome to TOS!");
  lcd->setCursor(0, 1);
  lcd->print("Press B1 to Start");

  delay(debounceDelay); // Debounce button

  startTime = millis();

  enableInterrupt(btn, next, FALLING); // Enable interrupt to switch phase

  SERIAL_DBG("Waiting phase initialized");
}

STATUS waiting_phase(const bool isPhaseChanged, LiquidCrystal_I2C* lcd_) {
  lcd = lcd_;
  if (isPhaseChanged) {
    waiting_init();
  }

  currentTime = millis();
  if ((currentTime - startTime) > waitBeforeSleep) {
    SERIAL_DBG("Entering sleep");
    sleep();
    SERIAL_DBG("Exiting sleep");
    lcd->backlight();
    waiting_init();
  }

  // LED_S PWM
  analogWrite(LED_S, pwmValue);
  pwmValue += pwmIncr;
  if (pwmValue >= pwmHighThreshold || pwmValue <= pwmLowThreshold) {
    pwmIncr = -pwmIncr;
  }

  delay(wait);

  return go_next_phase ? GO_NEXT : OK;
}
