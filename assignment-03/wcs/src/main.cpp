#include "components.h"
#include "config.h"
#include "states/state_manager.h"
#include "tasks/communication_service/communication_service.h"
#include "tasks/motor_driver/motor_driver.h"
#include "tasks/state_manager/state_manager_task.h"
#include <scheduler.h>
#include <stdint.h>

constexpr uint16_t period_ms = 100;

static Scheduler* scheduler;
static StateManager* state_manager;

extern void setup() {
  scheduler = new Scheduler(period_ms);
  state_manager = new StateManager(*scheduler);

  // Turn on the valve's motor
  auto* motor_driver = new MotorDriverTask(motor_driver_period);
  scheduler->addTask(motor_driver);
  Components::getInstance().setMotorDriver(motor_driver);
  Components::getInstance().getMotor().on();

  auto* state_manager_task =
    new StateManagerTask(state_manager_task_period, state_manager);
  scheduler->addTask(state_manager_task);

  auto* communication_service =
    &CommunicationService::getInstance(state_manager_task_period);
  scheduler->addTask(communication_service);
}

extern void loop() { scheduler->schedule(); }
