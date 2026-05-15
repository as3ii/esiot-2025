#include "pwm_led.h"
#include "Arduino.h"
#include "led.h"
#include <stdint.h>

PwmLed::PwmLed(const uint8_t pin, const uint8_t intensity)
  : Led(pin) {
  analogWrite(pin, intensity);
}

void PwmLed::setIntensity(const uint8_t intensity) const {
  analogWrite(getPin(), intensity);
}
