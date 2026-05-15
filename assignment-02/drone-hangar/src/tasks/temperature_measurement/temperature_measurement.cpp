#include "temperature_measurement.h"
#include "temperature_listener.h"
#include <stdint.h>
#include <task.h>
#include <thermometer.h>

TemperatureMeasurement::TemperatureMeasurement(const uint32_t period,
                                               Thermometer* thermometer,
                                               TemperatureListener* listener)
  : Task(period)
  , thermometer(thermometer)
  , listener(listener) {}

void TemperatureMeasurement::tick() {
  const float temperature = thermometer->getTemperature();
  listener->setTemperature(temperature);
}
