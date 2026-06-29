#ifndef STATE_AUTOMATIC_H
#define STATE_AUTOMATIC_H

#include "components.h"
#include "states/state.h"
#include "states/states.h"
#include "tasks/communication_service/command_callback.h"
#include "tasks/communication_service/data.h"
#include <stdint.h>

class StateAutomatic final
  : public State
  , public CommandCallback {
private:
  Components& components;
  uint8_t percentage;

public:
  StateAutomatic(uint8_t percentage);
  StateName getName() const final;
  bool goNext() final;
  ~StateAutomatic() final;

  uint8_t getPercentage() const final;
  tx_data callback(const rx_data& data) final;

  StateAutomatic(StateAutomatic const&) = delete; // Unwanted method
  void operator=(StateAutomatic const&) = delete; // Unwanted method
};

#endif
