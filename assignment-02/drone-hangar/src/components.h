#ifndef COMPONENTS_H
#define COMPONENTS_H

#include <button.h>
#include <lcd_screen.h>
#include <led.h>
#include <pir.h>
#include <servomotor_impl.h>
#include <temperature_sensor_LM36.h>
#include <ultrasonic_sensor.h>

class Components {
private:
  Led led_on;
  Led led_action;
  Led led_alarm;
  Button reset_button;
  LcdScreen lcd_screen;
  UltrasonicSensor distance_detector;
  TemperatureSensorLM36 thermometer;
  ServomotorImpl door_motor;
  Pir movement_detector;
  Components();

public:
  static Components& getInstance();
  Led& getLedOn();
  Led& getLedAction();
  Led& getLedAlarm();
  Button& getResetButton();
  LcdScreen& getLcdScreen();
  UltrasonicSensor& getDistanceDetector();
  TemperatureSensorLM36& getThermometer();
  ServomotorImpl& getDoorMotor();
  Pir& getMovementDetector();

  Components(Components const&) = delete;     // Unwanted method
  void operator=(Components const&) = delete; // Unwanted method
};

#endif
