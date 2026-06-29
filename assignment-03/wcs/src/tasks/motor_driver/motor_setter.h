#ifndef MOTOR_SETTER_H
#define MOTOR_SETTER_H

#include <stdint.h>

class MotorSetter {
public:
  virtual void setPosition(uint8_t position) = 0;
  // Destructor
  virtual ~MotorSetter() = default;
};

#endif
