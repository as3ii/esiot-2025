#include "motor_driver.h"
#include "components.h"
#include "config.h"
#include <stdint.h>
#include <task.h>

MotorDriverTask::MotorDriverTask(const uint32_t period)
  : Task(period)
  , motor(Components::getInstance().getMotor()) {}

void MotorDriverTask::tick() {
  if (target_position > (current_position + MOTOR_MAX_STEP_ANGLE)) {
    current_position += MOTOR_MAX_STEP_ANGLE;
  } else if ((target_position + MOTOR_MAX_STEP_ANGLE) < current_position) {
    current_position -= MOTOR_MAX_STEP_ANGLE;
  } else {
    current_position = target_position;
  }
  motor.setPosition(current_position);
}

void MotorDriverTask::setPosition(const uint8_t position) {
  target_position = position;
}
