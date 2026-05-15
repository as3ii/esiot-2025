#ifndef MOVEMENT_DETECTOR_H
#define MOVEMENT_DETECTOR_H

class MovementDetector {
public:
  // Return true if motion is detected
  virtual bool isMotionDetected() const = 0;
  // Destructor
  virtual ~MovementDetector() = default;
};

#endif
