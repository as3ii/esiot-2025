#ifndef PWM_LED_H
#define PWM_LED_H

#include "led.h"
#include "pwm_light.h"
#include <stdint.h>

class PwmLed
  : public Led
  , public PwmLight {
  // Input value in range 0..255
  PwmLed(uint8_t pin, uint8_t intensity);
  void setIntensity(uint8_t intensity) const final;
};

#endif
