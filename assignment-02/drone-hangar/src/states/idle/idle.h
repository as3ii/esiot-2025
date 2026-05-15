#ifndef STATE_IDLE_H
#define STATE_IDLE_H

#include "components.h"
#include "states/state.h"
#include "states/states.h"
#include "tasks/communication_service/command_callback.h"
#include "tasks/communication_service/data.h"

class StateIdle final
  : public State
  , public CommandCallback {
private:
  bool req_received = false;
  Components& components;

public:
  StateIdle();
  StateName getName() const final;
  bool goNext() final;
  // Supports only REQ_TAKE_OFF
  data callback(RX_COMMAND command) final;
  ~StateIdle() final;

  StateIdle(StateIdle const&) = delete;      // Unwanted method
  void operator=(StateIdle const&) = delete; // Unwanted method
};

#endif
