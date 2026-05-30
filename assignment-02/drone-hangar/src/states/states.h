#ifndef STATES_H
#define STATES_H

#include <stdint.h>

// State name
enum class StateName : uint8_t {
  Init = 1,
  Idle = 2,
  Takeoff = 3,
  DroneOut = 4,
  Landing = 5,
  PreAlarm = 6,
  Alarm = 7
};

const char* getStateName(StateName state);

#endif
