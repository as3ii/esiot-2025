#ifndef MOVEMENT_LISTENER_H
#define MOVEMENT_LISTENER_H

class MovementListener {
public:
  // Called then the drone is detected
  virtual void droneDetected() = 0;
  // Destructor
  virtual ~MovementListener() = default;
};

#endif
