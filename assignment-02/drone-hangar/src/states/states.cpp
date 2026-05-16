#include "states.h"

const char* getStateName(StateName state) {
  switch (state) {
    case StateName::Init:
      return "Init";
    case StateName::Idle:
      return "Idle";
    case StateName::Takeoff:
      return "Takeoff";
    case StateName::DroneOut:
      return "DroneOut";
    case StateName::Landing:
      return "Landing";
    case StateName::PreAlarm:
      return "PreAlarm";
    case StateName::Alarm:
      return "Alarm";
    default:
      return "UnknownState";
  }
};
