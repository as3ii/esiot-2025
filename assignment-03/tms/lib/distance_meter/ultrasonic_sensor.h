#ifndef ULTRASONIC_SENSOR_H
#define ULTRASONIC_SENSOR_H

#include "distance_meter.h"
#include <stdint.h>

class UltrasonicSensor : public DistanceMeter {
private:
  uint8_t trigger_pin;
  uint8_t echo_pin;
  static constexpr float nominal_room_temperature = 20.0;
  float temperature = nominal_room_temperature; // In celsius
public:
  UltrasonicSensor(uint8_t trigger_pin, uint8_t echo_pin);
  // Set current temperature in celsius
  void setTemperature(float temperature) final;
  // Return distance using temperature-compensated speed of sound, or -1 if no
  // echo is heard
  float getDistance() final;
};

#endif
