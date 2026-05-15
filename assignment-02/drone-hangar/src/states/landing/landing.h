#ifndef STATE_LANDING_H
#define STATE_LANDING_H

#include "components.h"
#include "states/state.h"
#include "states/states.h"
#include "tasks/communication_service/command_callback.h"
#include "tasks/communication_service/data.h"
#include "tasks/distance_measurement/distance_listener.h"
#include <scheduler.h>

class StateLanding final
  : public State
  , public DistanceListener
  , public CommandCallback {
private:
  Components& components;
  Scheduler& scheduler;
  float distance = 0.0;
  unsigned long timeDroneInside = 0;

public:
  StateLanding(Scheduler& scheduler);
  StateName getName() const final;
  bool goNext() final;
  void updateDistance(float distance) final;
  // Support only GET_DISTANCE
  data callback(RX_COMMAND command) final;
  ~StateLanding() final;

  StateLanding(StateLanding const&) = delete;   // Unwanted method
  void operator=(StateLanding const&) = delete; // Unwanted method
};

#endif
