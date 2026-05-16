#include "pre_alarm.h"
#include "components.h"
#include "config.h"
#include "scheduler.h"
#include "states/states.h"
#include "task.h"
#include "tasks/temperature_measurement/temperature_measurement.h"

StatePreAlarm::StatePreAlarm(Scheduler& scheduler)
  : scheduler(scheduler) {
  DEBUG_PRINT("D:Entered state PreAlarm");

  // Check temperature 3x more frequently then state_manager
  Task* temperature_measurement =
    new TemperatureMeasurement(temperature_measurement_period / 3,
                               &(Components::getInstance().getThermometer()),
                               this);
  scheduler.addTask(temperature_measurement);
}

StateName StatePreAlarm::getName() const { return StateName::PreAlarm; }

bool StatePreAlarm::goNext() { return temperature < TEMP_WARNING; }

void StatePreAlarm::setTemperature(const float temperature) {
  this->temperature = temperature;
}

StatePreAlarm::~StatePreAlarm() {
  DEBUG_PRINT("D:Destructing PreAlarm");
  scheduler.removeLastTask(); // Remove temperature task
}
