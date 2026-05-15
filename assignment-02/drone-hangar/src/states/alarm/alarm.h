#ifndef STATE_ALARM_H
#define STATE_ALARM_H

#include "config.h"
#include "scheduler.h"
#include "states/state.h"
#include "states/states.h"
#include "tasks/button_check/button_listener.h"
#include "tasks/temperature_measurement/temperature_listener.h"

class StateAlarm final
  : public State
  , public TemperatureListener
  , public ButtonListener {
private:
  Scheduler& scheduler;
  float temperature = nominal_room_temperature;
  bool button_pressed = false;

public:
  StateAlarm(Scheduler& scheduler);
  StateName getName() const final;
  bool goNext() final;
  void setTemperature(float temperature) final;
  void buttonPressed() final;
  ~StateAlarm() final;

  StateAlarm(StateAlarm const&) = delete;     // Unwanted method
  void operator=(StateAlarm const&) = delete; // Unwanted method
};

#endif
