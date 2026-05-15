#ifndef TASK_DISTANCE_MEASUREMENT_H
#define TASK_DISTANCE_MEASUREMENT_H

#include "distance_listener.h"
#include <distance_meter.h>
#include <stdint.h>
#include <task.h>

class DistanceMeasurement : public Task {
private:
  DistanceMeter* distance_meter;
  DistanceListener* listener;

public:
  DistanceMeasurement(uint32_t period,
                      DistanceMeter* distance_meter,
                      DistanceListener* listener);
  void tick() final;
};

#endif
