#ifndef TEMPERATURE_MEASUREMENT_H
#define TEMPERATURE_MEASUREMENT_H

#include "temperature_listener.h"
#include <stdint.h>
#include <task.h>
#include <thermometer.h>

class TemperatureMeasurement : public Task {
private:
  Thermometer* thermometer;
  TemperatureListener* listener;

public:
  TemperatureMeasurement(uint32_t period,
                         Thermometer* thermometer,
                         TemperatureListener* listener);
  void tick() final;
};

#endif
