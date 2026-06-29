#ifndef POTENTIOMETER_H
#define POTENTIOMETER_H

#include <stdint.h>

class Potentiometer {
private:
  uint8_t pin;

protected:
  uint8_t getPin() const;

public:
  explicit Potentiometer(uint8_t pin);
  // Return a value in range [0-100]
  virtual uint8_t read() const final;
  // Destructor
  virtual ~Potentiometer() = default;
};

#endif
