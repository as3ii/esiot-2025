#ifndef PWM_LIGHT_H
#define PWM_LIGHT_H

#include "light.h"
#include <stdint.h>

class PwmLight : public Light {
public:
  // Input value in range 0..255
  virtual void setIntensity(uint8_t intensity) const = 0;
};

#endif
