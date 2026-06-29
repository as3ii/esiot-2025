#ifndef POTENTIOMETER_LISTENER_H
#define POTENTIOMETER_LISTENER_H

#include <stdint.h>

class PotentiometerListener {
public:
  // Called to update the percentage
  virtual void setPercentage(uint8_t percentage) = 0;
  // Destructor
  virtual ~PotentiometerListener() = default;
};

#endif
