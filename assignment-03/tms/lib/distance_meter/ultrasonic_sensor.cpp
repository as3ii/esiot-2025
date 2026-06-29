#include "ultrasonic_sensor.h"
#include <Arduino.h>
#include <stdint.h>

constexpr uint8_t delay_us_1 = 3;  // First delay in microseconds
constexpr uint8_t delay_us_2 = 10; // Second delay in microseconds
// Used to convert microseconds passed from trigger to echo to seconds for half
// travel time
constexpr float us_to_s2 = 2000000.0;

// NOLINTNEXTLINE(bugprone-easily-swappable-parameters)
UltrasonicSensor::UltrasonicSensor(const uint8_t trigger_pin,
                                   const uint8_t echo_pin)
  : trigger_pin(trigger_pin)
  , echo_pin(echo_pin) {
  pinMode(trigger_pin, OUTPUT);
  pinMode(echo_pin, INPUT);
}

// Set current temperature in celsius
void UltrasonicSensor::setTemperature(float temperature) {
  this->temperature = temperature;
}

// Return distance using temperature-compensated speed of sound, or -1 if no
// echo is heard
float UltrasonicSensor::getDistance() {
  const float conv = 331.45F + (0.62F * temperature);

  digitalWrite(trigger_pin, LOW);
  delayMicroseconds(delay_us_1);
  digitalWrite(trigger_pin, HIGH);
  delayMicroseconds(delay_us_2);
  digitalWrite(trigger_pin, LOW);

  // Limit to ~5m (30ms), as datasheet tells that the max range is 4m
  const unsigned long time_us = pulseIn(echo_pin, HIGH, 30000);
  if (time_us > 0) {
    return ((float)time_us / us_to_s2) * conv;
  }
  return -1.0;
}
