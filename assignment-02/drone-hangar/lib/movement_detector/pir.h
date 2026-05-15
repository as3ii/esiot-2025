#ifndef PIR_H
#define PIR_H

#include "movement_detector.h"
#include <stdint.h>

class Pir : public MovementDetector {
private:
  uint8_t pin;
  unsigned long creation_time; // Time since object creation
public:
  explicit Pir(uint8_t pin);
  // Returns true after the calibration time is passed
  bool isCalibrationFinished() const;
  // Return true if motion is detected
  bool isMotionDetected() const final;
};

#endif
