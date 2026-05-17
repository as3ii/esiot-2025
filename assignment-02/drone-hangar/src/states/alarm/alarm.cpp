#include "alarm.h"
#include "components.h"
#include "config.h"
#include "debug.h"
#include "scheduler.h"
#include "states/states.h"
#include "task.h"
#include "tasks/button_check/button_check.h"
#include "tasks/temperature_measurement/temperature_measurement.h"

StateAlarm::StateAlarm(Scheduler& scheduler)
  : scheduler(scheduler) {
  F_DEBUG_PRINT("D:Entered state Alarm");

  Components& components = Components::getInstance();

  // Close door
  components.getDoorMotor().on();
  components.getDoorMotor().setPosition(DOOR_CLOSE);

  // Switch on Alarm LED
  components.getLedAlarm().switchOn();

  components.getLcdScreen().clear();
  components.getLcdScreen().setCursor(0, 0);
  components.getLcdScreen().print("ALARM");

  // Check temperature more frequently then StateManager
  Task* temperature_measurement = new TemperatureMeasurement(
    temperature_measurement_err_period, &(components.getThermometer()), this);
  scheduler.addTask(temperature_measurement);

  // Check if reset button is pressed
  Task* button_check =
    new ButtonCheck(button_check_period, &(components.getResetButton()), this);
  scheduler.addTask(button_check);
}

StateName StateAlarm::getName() const { return StateName::Alarm; }

bool StateAlarm::goNext() { return button_pressed; }

void StateAlarm::setTemperature(const float temperature) {
  this->temperature = temperature;
}

void StateAlarm::buttonPressed() {
  F_DEBUG_PRINT("D:Reset button pressed");
  // Set to true only if the temperature is ok
  button_pressed = temperature < TEMP_CRITICAL;
}

StateAlarm::~StateAlarm() {
  F_DEBUG_PRINT("D:Destructing Alarm");
  Components::getInstance().getDoorMotor().off();
  scheduler.removeLastTask(); // Remove reset button task
  scheduler.removeLastTask(); // Remove temperature task
}
