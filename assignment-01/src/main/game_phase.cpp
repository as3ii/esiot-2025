#include "game_phase.h"
#include "WString.h"
#include "model.h"
#include <Arduino.h>
#include <LiquidCrystal_I2C.h>
#include <stdint.h>
#include <stdlib.h>

constexpr unsigned long defaultT1 = 5000;

static GAME_PHASE gamePhase = SHOW_SEQUENCE;
static STATUS gamePhaseStatus = OK;
static uint32_t score = 0;
static uint8_t difficulty = 1; // Range: 1..4
static bool gameOver = false;
static uint8_t index = 0;

// Sequence and input handling
static uint8_t sequence[4];
static uint8_t response[4];
static unsigned long debounce[4];

// Time-related stuff
static unsigned long T1 = defaultT1; // Round length in ms
// NOLINTNEXTLINE(cppcoreguidelines-avoid-magic-numbers,readability-magic-numbers)
static unsigned long F = (T1 / 100) * difficulty; // Reduction factor for T1
static unsigned long startTime = 0;
static unsigned long currentTime = 0;

static LiquidCrystal_I2C* lcd;

inline static void shuffle_sequence() {
  srand(millis()); // seed
  for (uint8_t i = 0; i < 4; i++) {
    const uint8_t rnd = rand() % 4;
    const uint8_t temp = sequence[rnd];
    sequence[rnd] = sequence[i];
    sequence[i] = temp;
  }
}

static STATUS show_sequence() {
  shuffle_sequence();

  lcd->clear();
  lcd->setCursor(0, 0);
  lcd->print("G0!");
  lcd->setCursor(0, 1);
  // Print sequence
  for (const uint8_t num : sequence) {
    lcd->print(num);
  }
  lcd->print("            "); // 12 spaces, clear the line

  // Reset round
  startTime = millis();
  index = 0;
  for (uint8_t i = 0; i < 4; i++) {
    response[i] = UINT8_MAX; // No response
    debounce[i] = 0;
  }
  for (const uint8_t led : leds) {
    digitalWrite(led, LOW);
  }

  return GO_NEXT;
}

static STATUS replicate_sequence() {
  currentTime = millis();
  // Check for timeout
  if ((currentTime - startTime) > T1 || index >= 4) {
    return GO_NEXT;
  }

  // Read buttons, with debounce
  for (uint8_t i = 0; i < 4; i++) {
    if (((currentTime - debounce[i]) > debounceDelay) &&
        digitalRead(buttons[i]) == HIGH) {
      debounce[i] = currentTime;
      if (index < 4) { // Limit inputs
        digitalWrite(leds[i], HIGH);
        response[index] = i + 1;
        index++;
      }
    }
  }

  return OK;
}

static STATUS end_round() {
  currentTime = millis();
  // Check response time
  gameOver = (currentTime - startTime) > T1;

  // Check sequence
  for (uint8_t i = 0; i < 4; i++) {
    SERIAL_DBG("seq: " + (String)sequence[i] +
               " - res: " + (String)response[i]);
    if (sequence[i] != response[i]) {
      gameOver = true;
    }
  }

  // Increase by 1 only if not gameOver
  score += (uint32_t)(!gameOver);
  T1 -= F;

  if (!gameOver) {
    lcd->clear();
    lcd->setCursor(0, 0);
    lcd->print("GOOD! Score: ");
    lcd->print(score);
    SERIAL_DBG("Ok, score: " + (String)score);
    delay(defaultT1);
  }

  return GO_NEXT;
}

static void game_init() {
  // Switch off leds 1..4
  for (const uint8_t led : leds) {
    digitalWrite(led, LOW);
  }
  digitalWrite(LED_S, LOW); // Switch off status LED

  // Reset variables
  gamePhase = SHOW_SEQUENCE;
  score = 0;
  gameOver = false;
  for (uint8_t i = 0; i < 4; i++) {
    sequence[i] = i + 1;
    debounce[i] = 0;
  }

  // Get difficulty
  difficulty =
    // NOLINTNEXTLINE(cppcoreguidelines-avoid-magic-numbers,readability-magic-numbers)
    (analogRead(POT) / 256) + 1; // analogRead returns a number 0..1023
  SERIAL_DBG("Difficulty: " + (String)difficulty)

  SERIAL_DBG("Game phase initialized");
}

STATUS game_phase(const bool isPhaseChanged, LiquidCrystal_I2C* const lcd_) {
  lcd = lcd_;
  if (isPhaseChanged) {
    game_init();
  }

  // Call the right game phase function
  switch (gamePhase) {
    case SHOW_SEQUENCE:
      gamePhaseStatus = show_sequence();
      break;
    case REPLICATE_SEQUENCE:
      gamePhaseStatus = replicate_sequence();
      break;
    case END_ROUND:
      gamePhaseStatus = end_round();
      break;
    default:
      return ERR;
  }

  // Check game phase status
  switch (gamePhaseStatus) {
    case OK:
      break;
    case GO_NEXT:
      switch (gamePhase) {
        case SHOW_SEQUENCE:
          gamePhase = REPLICATE_SEQUENCE;
          break;
        case REPLICATE_SEQUENCE:
          gamePhase = END_ROUND;
          break;
        case END_ROUND:
          gamePhase = SHOW_SEQUENCE;
          break;
        default:
          return ERR;
      }
      break;
    case ERR:
      return ERR;
  }

  // Handle game over
  if (gameOver) {
    digitalWrite(LED_S, HIGH);
    lcd->clear();
    lcd->setCursor(0, 0);
    lcd->print("Game Over");
    lcd->setCursor(0, 1);
    lcd->print("Final Score: ");
    lcd->print(score);
    SERIAL_DBG("Game over, score: " + (String)score);
    delay(defaultT1);
    digitalWrite(LED_S, LOW);
    return GO_NEXT;
  }
  return OK;
}

GAME_PHASE getGamePhase() { return gamePhase; }
