#ifndef DISTANCE_METER_H
#define DISTANCE_METER_H

class DistanceMeter {
public:
  // Get distance in meters
  virtual float getDistance() = 0;
  // Set current temperature in celsius, if the sensor needs it
  virtual void setTemperature(float temperature) {};
  // Destructor
  virtual ~DistanceMeter() = default;
};

#endif
