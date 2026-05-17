#include "config.h"
#include "states/state_manager.h"
#include "tasks/communication_service/communication_service.h"
#include "tasks/communication_service/data.h"
#include "tasks/state_manager/state_manager_task.h"
#include <scheduler.h>
#include <stdint.h>
#include <task.h>

constexpr uint16_t period_ms = 100;

static Scheduler* scheduler;
static StateManager* state_manager;

extern void setup() {
  DEBUG_PRINT("D:----------------");
  DEBUG_PRINT("D:Starting tasks setup");

  scheduler = new Scheduler(period_ms);
  state_manager = new StateManager(*scheduler);

  auto* state_manager_task =
    new StateManagerTask(state_manager_task_period, state_manager);
  scheduler->addTask(state_manager_task);

  auto* communication_service =
    &CommunicationService::getInstance(state_manager_task_period);
  scheduler->addTask(static_cast<Task*>(communication_service));

  communication_service->setCallback(RX_COMMAND::GET_STATE, state_manager);
  communication_service->setCallback(RX_COMMAND::GET_TEMPERATURE,
                                     state_manager);

  DEBUG_PRINT("D:System initialized");
}

extern void loop() {
  // DEBUG_PRINTF("D:Loop %ld", millis());
  scheduler->schedule();
}
