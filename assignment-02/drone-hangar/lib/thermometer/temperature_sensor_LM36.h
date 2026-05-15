#ifndef TEMPERATURE_SENSOR_LM36_H
#define TEMPERATURE_SENSOR_LM36_H

#include "thermometer.h"
#include <stdint.h>

class TemperatureSensorLM36 : public Thermometer {
private:
  uint8_t pin;

public:
  explicit TemperatureSensorLM36(uint8_t pin);
  // Returns temperature in celsius
  float getTemperature() const final;
};

#endif
