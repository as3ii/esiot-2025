#include "distance_measurement.h"
#include "components.h"
#include "distance_listener.h"
#include <distance_meter.h>
#include <stdint.h>
#include <task.h>

DistanceMeasurement::DistanceMeasurement(const uint32_t period,
                                         DistanceMeter* distance_meter,
                                         DistanceListener* listener)
  : Task(period)
  , distance_meter(distance_meter)
  , listener(listener) {
  distance_meter->setTemperature(
    Components::getInstance().getThermometer().getTemperature());
}

void DistanceMeasurement::tick() {
  const float distance = distance_meter->getDistance();
  listener->updateDistance(distance);
}
