#include "state_manager_task.h"
#include "states/state_manager.h"
#include <stdint.h>
#include <task.h>

StateManagerTask::StateManagerTask(const uint32_t period,
                                   StateManager* state_manager)
  : Task(period)
  , state_manager(state_manager) {}

void StateManagerTask::tick() { state_manager->switchState(); }
