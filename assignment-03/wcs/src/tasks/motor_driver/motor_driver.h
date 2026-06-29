#ifndef TASK_MOTOR_DRIVER_H
#define TASK_MOTOR_DRIVER_H

#include "config.h"
#include "servomotor_impl.h"
#include "tasks/motor_driver/motor_setter.h"
#include <stdint.h>
#include <task.h>

class MotorDriverTask
  : public Task
  , public MotorSetter {
private:
  uint8_t target_position = VALVE_CLOSED;
  uint8_t current_position = VALVE_CLOSED;
  ServomotorImpl& motor;

public:
  MotorDriverTask(uint32_t period);
  void tick() final;

  void setPosition(uint8_t position) final;

  MotorDriverTask(MotorDriverTask const&) = delete;
  void operator=(MotorDriverTask const&) = delete;
};

#endif
