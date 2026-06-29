#include "potentiometer.h"
#include <Arduino.h>
#include <stdint.h>

Potentiometer::Potentiometer(const uint8_t pin)
  : pin(pin) {
  pinMode(pin, INPUT);
}

uint8_t Potentiometer::read() const {
  // NOLINTNEXTLINE(cppcoreguidelines-avoid-magic-numbers,readability-magic-numbers,-warnings-as-errors)
  return ((uint32_t)analogRead(pin)) * 100 / 1023;
}
