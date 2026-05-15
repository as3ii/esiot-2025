#include "temperature_sensor_LM36.h"
#include "Arduino.h"
#include <stdint.h>

constexpr float adc_ref_mv = 5000.0;  // ADC reference in mV
constexpr float adc_max_val = 1024.0; // ADC max returned value casted to float
constexpr float mv_to_c = 0.1;        // mV to °C conversion

TemperatureSensorLM36::TemperatureSensorLM36(const uint8_t pin)
  : pin(pin) {
  pinMode(pin, INPUT);
}

float TemperatureSensorLM36::getTemperature() const {
  /* Conversion between read value to measured mV
   *   value_read : adc_max_val = value_mv : adc_ref_mv
   *   value_mv = value_read * adc_ref_mv / adc_max_val
   * Conversion between value in mV and value in °C
   *   value_in_celsius = (value_mv - 500) * mv_to_c
   */
  return ((analogRead(pin) * (adc_ref_mv / adc_max_val)) - 500.0) * mv_to_c;
}
