#ifndef SERVOMOTOR_IMPL_H
#define SERVOMOTOR_IMPL_H

#include "ServoTimer2.h"
#include "servomotor.h"
#include <stdint.h>

class ServomotorImpl : public Servomotor {
private:
  uint8_t pin;
  ServoTimer2 motor;

public:
  explicit ServomotorImpl(uint8_t pin);
  // Enable motor
  void on() final;
  // Set position/angle in range 0..180
  void setPosition(uint8_t angle) final;
  // Disable motor
  void off() final;
};

#endif
