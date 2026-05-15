#ifndef STATE_TAKEOFF_H
#define STATE_TAKEOFF_H

#include "components.h"
#include "scheduler.h"
#include "states/state.h"
#include "states/states.h"
#include "tasks/distance_measurement/distance_listener.h"

class StateTakeoff final
  : public State
  , public DistanceListener {
private:
  Components& components;
  Scheduler& scheduler;
  float distance = 0.0;
  unsigned long timeDroneOutside = 0;

public:
  StateTakeoff(Scheduler& scheduler);
  StateName getName() const final;
  bool goNext() final;
  void updateDistance(float distance) final;
  ~StateTakeoff() final;

  StateTakeoff(StateTakeoff const&) = delete;   // Unwanted method
  void operator=(StateTakeoff const&) = delete; // Unwanted method
};

#endif
