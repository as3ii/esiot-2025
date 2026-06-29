#ifndef MANUAL_HANDLING_H
#define MANUAL_HANDLING_H

#include "tasks/manual_handling/potentiometer_listener.h"
#include "tasks/motor_driver/motor_setter.h"
#include <potentiometer.h>
#include <stdint.h>
#include <task.h>

class ManualHandling : public Task {
private:
  Potentiometer& potentiometer;
  MotorSetter* driver;
  PotentiometerListener* callback;
  uint8_t percentage;

public:
  ManualHandling(uint32_t period, PotentiometerListener* callback);
  void tick() final;
};

#endif
