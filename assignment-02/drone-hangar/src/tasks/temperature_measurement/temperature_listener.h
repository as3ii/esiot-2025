#ifndef TEMPERATURE_LISTENER_H
#define TEMPERATURE_LISTENER_H

class TemperatureListener {
public:
  // Called when the temperature is read
  virtual void setTemperature(float temperature) = 0;
  // Destructor
  virtual ~TemperatureListener() = default;
};

#endif
