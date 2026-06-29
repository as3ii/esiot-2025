#include "button.h"
#include <Arduino.h>
#include <stdint.h>

// NOLINTNEXTLINE(bugprone-easily-swappable-parameters)
Button::Button(const uint8_t pin, const uint16_t debounce_ms)
  : pin(pin)
  , debounce_ms(debounce_ms) {
  pinMode(pin, INPUT);
}

uint8_t Button::getPin() const { return pin; };

bool Button::isPressed() const { return digitalRead(pin) == HIGH; }

bool Button::isPressedDebounced() {
  const unsigned long current_time = millis();
  if ((current_time - last_pressed_time) > debounce_ms &&
      digitalRead(pin) == HIGH) {
    last_pressed_time = current_time;
    return true;
  }
  return false;
}
