#ifndef TASK_MOVEMENT_DETECTION_H
#define TASK_MOVEMENT_DETECTION_H

#include "movement_listener.h"
#include <movement_detector.h>
#include <stdint.h>
#include <task.h>

class MovementDetection : public Task {
private:
  MovementDetector* movement_detector;
  MovementListener* listener;

public:
  MovementDetection(uint32_t period,
                    MovementDetector* movement_detector,
                    MovementListener* listener);
  void tick() final;
};

#endif
