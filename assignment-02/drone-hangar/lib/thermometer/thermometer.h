#ifndef THERMOMETER_H
#define THERMOMETER_H

class Thermometer {
public:
  // Returns temperature in celsius
  virtual float getTemperature() const = 0;
  // Destructor
  virtual ~Thermometer() = default;
};

#endif
