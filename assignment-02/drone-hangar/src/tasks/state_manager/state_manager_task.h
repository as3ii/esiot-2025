#ifndef TASK_STATE_MANAGER_H
#define TASK_STATE_MANAGER_H

#include "states/state_manager.h"
#include <stdint.h>
#include <task.h>

class StateManagerTask : public Task {
private:
  StateManager& state_manager;

public:
  StateManagerTask(uint32_t period, StateManager& state_manager);
  void tick() final;

  StateManagerTask(StateManagerTask const&) = delete; // Unwanted method
  void operator=(StateManagerTask const&) = delete;   // Unwanted method
};

#endif
