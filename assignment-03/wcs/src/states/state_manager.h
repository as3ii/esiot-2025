#ifndef STATE_MANAGER_H
#define STATE_MANAGER_H

#include "state.h"
#include "states.h"
#include "tasks/communication_service/command_callback.h"
#include "tasks/communication_service/data.h"
#include <scheduler.h>

class StateManager : public CommandCallback {
private:
  Scheduler& scheduler;
  State* state;

public:
  StateManager(Scheduler& scheduler);
  void switchState();
  StateName getCurrentState() const;

  tx_data callback(const rx_data& data) final;

  StateManager(StateManager const&) = delete;   // Unwanted method
  void operator=(StateManager const&) = delete; // Unwanted method
};

#endif
