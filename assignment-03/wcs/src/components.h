#ifndef COMPONENTS_H
#define COMPONENTS_H

#include "tasks/motor_driver/motor_setter.h"
#include <button.h>
#include <lcd_screen.h>
#include <potentiometer.h>
#include <servomotor_impl.h>

class Components {
private:
  Button button;
  LcdScreen lcd_screen;
  Potentiometer potentiometer;
  ServomotorImpl motor;
  MotorSetter* driver = nullptr;
  Components();

public:
  static Components& getInstance();
  Button& getButton();
  LcdScreen& getLcdScreen();
  Potentiometer& getPotentiometer();
  ServomotorImpl& getMotor();
  MotorSetter* getMotorDriver(); // Can return null

  void setMotorDriver(MotorSetter* task);

  Components(Components const&) = delete;     // Unwanted method
  void operator=(Components const&) = delete; // Unwanted method
};

#endif
