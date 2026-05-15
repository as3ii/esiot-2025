#include "states.h"

const char* getStateName(StateName state) {
  switch (state) {
    case Init:
      return "Init";
    case Idle:
      return "Idle";
    case Takeoff:
      return "Takeoff";
    case DroneOut:
      return "DroneOut";
    case Landing:
      return "Landing";
    case PreAlarm:
      return "PreAlarm";
    case Alarm:
      return "Alarm";
    default:
      return "UnknownState";
  }
};
