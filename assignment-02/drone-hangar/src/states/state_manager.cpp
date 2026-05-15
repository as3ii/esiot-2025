#include "state_manager.h"
#include "alarm/alarm.h"
#include "components.h"
#include "config.h"
#include "drone_out/drone_out.h"
#include "idle/idle.h"
#include "init/init.h"
#include "landing/landing.h"
#include "pre_alarm/pre_alarm.h"
#include "state.h"
#include "states.h"
#include "takeoff/takeoff.h"
#include "task.h"
#include "tasks/communication_service/data.h"
#include "tasks/temperature_measurement/temperature_measurement.h"
#include <Arduino.h>
#include <scheduler.h>

#if DEBUG == 1
#include <stdint.h>
#include <stdlib.h>
constexpr uint8_t temp_buff = 7;
void StateManager::print_temperature() const {
  // sign + 2 chars for integer + dot + 2 chars for decimal + null = 7
  char buff[temp_buff];
  DEBUG_PRINTF("D:Temp %s", dtostrf(temperature, 4, 2, buff));
}
#else
#include <HardwareSerial.h>
void StateManager::print_temperature() const {
  Serial.print("D:Temp ");
  Serial.println(temperature, 2);
  Serial.flush();
}
#endif

StateManager::StateManager(Scheduler& scheduler)
  : scheduler(scheduler)
  , components(Components::getInstance())
  , state(new StateInit()) {
  Task* temperature_measurement = new TemperatureMeasurement(
    temperature_measurement_period, &(components.getThermometer()), this);
  scheduler.addTask(temperature_measurement);
}

State* StateManager::stateFactory(StateName state) {
  switch (state) {
    case Init:
      return new StateInit();
    case Idle:
      return new StateIdle();
    case Takeoff:
      return new StateTakeoff(scheduler);
    case DroneOut:
      return new StateDroneOut(scheduler);
    case Landing:
      return new StateLanding(scheduler);
    case PreAlarm:
      return new StatePreAlarm(scheduler);
    case Alarm:
    default:
      return new StateAlarm(scheduler);
  }
}

void StateManager::switchState() {
  const StateName current = state->getName();

  // Critical temperature - alarm
  // Switch to StateAlarm instantly
  if (current != Alarm && temperature > TEMP_CRITICAL) {
    if (time_threshold_crit == 0) {
      time_threshold_crit = millis();
    } else {
      if ((millis() - time_threshold_crit) > TIME_CRITICAL) {
        old_state_name = current; // Save old state
        delete (state);
        asm volatile("" ::: "memory"); // Do not reorder instructions
        state = stateFactory(Alarm);
        return;
      }
    }
  }

  // Warning temperature - pre-alarm
  // Switch to StatePreAlarm only if current is Idle or DroneOut
  // to avoid future landings or takeoffs
  if ((current == Idle || current == DroneOut) && temperature > TEMP_WARNING) {
    if (time_threshold_warn == 0) {
      time_threshold_warn = millis();
    } else {
      if ((millis() - time_threshold_warn) > TIME_WARNING) {
        old_state_name = current; // Save old state
        delete (state);
        asm volatile("" ::: "memory"); // Do not reorder instructions
        state = stateFactory(PreAlarm);
        return;
      }
    }
  }

  // Normal operation
  if (state->goNext()) {
    const StateName name = state->getName();
    DEBUG_PRINTF("D:Switching away from %s", getStateName(name));
    delete (state);
    asm volatile("" ::: "memory"); // Do not reorder instructions

    switch (name) {
      case Init:
        state = stateFactory(Idle);
        break;
      case Idle:
        state = stateFactory(Takeoff);
        break;
      case Takeoff:
        state = stateFactory(DroneOut);
        break;
      case DroneOut:
        state = stateFactory(Landing);
        break;
      case Landing:
        state = stateFactory(Idle);
        break;
      case PreAlarm:
        state = stateFactory(old_state_name);
        time_threshold_warn = 0; // Reset
        old_state_name = Idle;   // Reset
        break;
      case Alarm:
        state = stateFactory(old_state_name);
        time_threshold_crit = 0; // Reset
        old_state_name = Idle;   // Reset
        break;
    }
  }
}

StateName StateManager::getCurrentState() const { return state->getName(); }

void StateManager::setTemperature(const float temperature) {
  this->temperature = temperature;
  print_temperature();
}

data StateManager::callback(const RX_COMMAND command) {
  switch (command) {
    case GET_STATE:
      return data{ .cmd = STATE, .val = { .state = getCurrentState() } };
    case GET_TEMPERATURE:
      return data{ .cmd = TEMPERATURE, .val = { .f = temperature } };
    default:
      return data{ .cmd = INVALID };
  }
}

StateManager::~StateManager() {
  scheduler.removeLastTask(); // Remove temperature task
}
