#ifndef SERVOMOTOR_H
#define SERVOMOTOR_H

#include <stdint.h>

class Servomotor {
public:
  // Enable motor
  virtual void on() = 0;
  // Set position/angle in range 0..180
  virtual void setPosition(uint8_t angle) = 0;
  // Disable motor
  virtual void off() = 0;
  // Destructor
  virtual ~Servomotor() = default;
};

#endif
