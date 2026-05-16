#ifndef STATES_H
#define STATES_H

#include <stdint.h>

// State name
enum class StateName : uint8_t {
  Init,
  Idle,
  Takeoff,
  DroneOut,
  Landing,
  PreAlarm,
  Alarm
};

const char* getStateName(StateName state);

#endif
