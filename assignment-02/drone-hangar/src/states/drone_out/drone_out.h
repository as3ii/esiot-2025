#ifndef STATE_DRONE_OUT_H
#define STATE_DRONE_OUT_H

#include "components.h"
#include "states/state.h"
#include "states/states.h"
#include "tasks/communication_service/command_callback.h"
#include "tasks/communication_service/data.h"
#include "tasks/movement_detaction/movement_listener.h"
#include <scheduler.h>
#include <stdint.h>

// Time in ms
constexpr uint32_t movement_detector_period = 200;

class StateDroneOut final
  : public State
  , public MovementListener
  , public CommandCallback {
private:
  bool drone_detected = false;
  bool req_received = false;
  Scheduler& scheduler;
  Components& components;

public:
  StateDroneOut(Scheduler& scheduler);
  StateName getName() const final;
  void droneDetected() final;
  bool goNext() final;
  // Supports only REQ_LANDING
  data callback(RX_COMMAND command) final;
  ~StateDroneOut() final;

  StateDroneOut(StateDroneOut const&) = delete;  // Unwanted method
  void operator=(StateDroneOut const&) = delete; // Unwanted method
};

#endif
