#ifndef STATE_MANUAL_H
#define STATE_MANUAL_H

#include "components.h"
#include "scheduler.h"
#include "states/state.h"
#include "states/states.h"
#include "tasks/communication_service/command_callback.h"
#include "tasks/communication_service/data.h"
#include "tasks/manual_handling/potentiometer_listener.h"
#include <stdint.h>

class StateManual final
  : public State
  , public CommandCallback
  , public PotentiometerListener {
private:
  Scheduler& scheduler;
  Components& components;
  uint8_t percentage;

public:
  StateManual(Scheduler& scheduler, uint8_t percentage);
  StateName getName() const final;
  bool goNext() final;
  ~StateManual() final;

  void setPercentage(uint8_t percentage) final;

  uint8_t getPercentage() const final;
  tx_data callback(const rx_data& data) final;

  StateManual(StateManual const&) = delete;    // Unwanted method
  void operator=(StateManual const&) = delete; // Unwanted method
};

#endif
