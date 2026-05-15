#ifndef LED_H
#define LED_H

#include "light.h"
#include <stdint.h>

class Led : public Light {
private:
  uint8_t pin;

protected:
  uint8_t getPin() const;

public:
  explicit Led(uint8_t pin);
  void switchOn() const final;
  void switchOff() const final;
  void setState(bool state) const final;
};

#endif
