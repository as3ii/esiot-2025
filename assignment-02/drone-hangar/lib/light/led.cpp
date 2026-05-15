#include "led.h"
#include <Arduino.h>
#include <stdint.h>

Led::Led(const uint8_t pin)
  : pin(pin) {
  pinMode(pin, OUTPUT);
}

uint8_t Led::getPin() const { return pin; }

void Led::setState(const bool state) const {
  digitalWrite(pin, state ? HIGH : LOW);
}

void Led::switchOn() const { setState(true); }

void Led::switchOff() const { setState(false); }
