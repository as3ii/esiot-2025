#ifndef STATE_PRE_ALARM_H
#define STATE_PRE_ALARM_H

#include "config.h"
#include "scheduler.h"
#include "states/state.h"
#include "states/states.h"
#include "tasks/temperature_measurement/temperature_listener.h"

class StatePreAlarm final
  : public State
  , public TemperatureListener {
private:
  Scheduler& scheduler;
  float temperature = nominal_room_temperature;

public:
  StatePreAlarm(Scheduler& scheduler);
  StateName getName() const final;
  bool goNext() final;
  void setTemperature(float temperature) final;
  ~StatePreAlarm() final;

  StatePreAlarm(StatePreAlarm const&) = delete;  // Unwanted method
  void operator=(StatePreAlarm const&) = delete; // Unwanted method
};

#endif
