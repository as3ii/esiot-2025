#include "components.h"
#include "config.h"
#include <button.h>
#include <lcd_screen.h>
#include <led.h>
#include <pir.h>
#include <servomotor_impl.h>
#include <temperature_sensor_LM36.h>
#include <ultrasonic_sensor.h>

Components::Components()
  : led_on(Led(LED_ON))
  , led_action(Led(LED_ACTION))
  , led_alarm(Led(LED_ALARM))
  , reset_button(Button(BTN))
  , lcd_screen(LcdScreen(LCD_ADDR, LCD_COLS, LCD_ROWS))
  , distance_detector(UltrasonicSensor(SONAR_TRIG, SONAR_ECHO))
  , thermometer(TemperatureSensorLM36(TEMP))
  , door_motor(ServomotorImpl(SERVO))
  , movement_detector(Pir(PIR)) {}

Components& Components::getInstance() {
  static Components instance;
  return instance;
}

Led& Components::getLedOn() { return led_on; }

Led& Components::getLedAction() { return led_action; }

Led& Components::getLedAlarm() { return led_alarm; }

Button& Components::getResetButton() { return reset_button; }

LcdScreen& Components::getLcdScreen() { return lcd_screen; }

UltrasonicSensor& Components::getDistanceDetector() {
  return distance_detector;
}

TemperatureSensorLM36& Components::getThermometer() { return thermometer; }

ServomotorImpl& Components::getDoorMotor() { return door_motor; }

Pir& Components::getMovementDetector() { return movement_detector; }
