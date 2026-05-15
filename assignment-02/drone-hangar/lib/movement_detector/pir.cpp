#include "pir.h"
#include "Arduino.h"
#include <stdint.h>

constexpr unsigned long calibration_time = 10000; // 10 seconds

Pir::Pir(const uint8_t pin)
  : pin(pin)
  , creation_time(millis()) {
  pinMode(pin, INPUT);
}

bool Pir::isCalibrationFinished() const {
  return (millis() - creation_time) > calibration_time;
}

bool Pir::isMotionDetected() const { return digitalRead(pin) == HIGH; }
