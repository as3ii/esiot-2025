#include "components.h"
#include "button.h"
#include "config.h"
#include "lcd_screen.h"
#include "potentiometer.h"
#include "servomotor_impl.h"
#include "tasks/motor_driver/motor_setter.h"

Components::Components()
  : button(BTN)
  , lcd_screen(LCD_ADDR, LCD_COLS, LCD_ROWS)
  , potentiometer(POT)
  , motor(VALVE_MOTOR) {}

Components& Components::getInstance() {
  static Components instance;
  return instance;
}

Button& Components::getButton() { return button; }

LcdScreen& Components::getLcdScreen() { return lcd_screen; }

Potentiometer& Components::getPotentiometer() { return potentiometer; }

ServomotorImpl& Components::getMotor() { return motor; }

MotorSetter* Components::getMotorDriver() { return driver; }

void Components::setMotorDriver(MotorSetter* const task) { driver = task; }
