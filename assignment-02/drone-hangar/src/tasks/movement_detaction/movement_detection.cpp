#include "movement_detection.h"
#include "movement_listener.h"
#include <movement_detector.h>
#include <stdint.h>
#include <task.h>

MovementDetection::MovementDetection(const uint32_t period,
                                     MovementDetector* movement_detector,
                                     MovementListener* listener)
  : Task(period)
  , movement_detector(movement_detector)
  , listener(listener) {}

void MovementDetection::tick() {
  if (movement_detector->isMotionDetected()) {
    listener->droneDetected();
  }
}
