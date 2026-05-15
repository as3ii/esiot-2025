#ifndef STATE_MANAGER_H
#define STATE_MANAGER_H

#include "components.h"
#include "config.h"
#include "state.h"
#include "states.h"
#include "tasks/communication_service/command_callback.h"
#include "tasks/communication_service/data.h"
#include "tasks/temperature_measurement/temperature_listener.h"
#include <scheduler.h>
#include <stdint.h>

class StateManager
  : public TemperatureListener
  , public CommandCallback {
private:
  Scheduler& scheduler;
  Components& components;
  State* state;
  StateName old_state_name = StateName::Init;
  uint32_t time_threshold_warn = 0;
  uint32_t time_threshold_crit = 0;
  float temperature = nominal_room_temperature;

  State* stateFactory(StateName state);
  void print_temperature() const;

public:
  StateManager(Scheduler& scheduler);
  void switchState();
  StateName getCurrentState() const;
  void setTemperature(float temperature) final;
  // Supports only GET_STATE and GET_TEMPERATURE
  data callback(RX_COMMAND command) final;
  ~StateManager() override;

  StateManager(StateManager const&) = delete;   // Unwanted method
  void operator=(StateManager const&) = delete; // Unwanted method
};

#endif
