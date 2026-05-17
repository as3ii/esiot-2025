#include "takeoff.h"
#include "components.h"
#include "config.h"
#include "debug.h"
#include "states/states.h"
#include "tasks/blink/blink.h"
#include "tasks/distance_measurement/distance_measurement.h"
#include <Arduino.h>
#include <lcd_screen.h>
#include <scheduler.h>
#include <stdint.h>
#include <task.h>

constexpr uint32_t s_to_ms = 1000;

StateTakeoff::StateTakeoff(Scheduler& scheduler)
  : components(Components::getInstance())
  , scheduler(scheduler) {
  F_DEBUG_PRINT("D:Entered state TakeOff");

  LcdScreen& lcd = components.getLcdScreen();
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("TAKE OFF");

  // Open door
  components.getDoorMotor().on();
  components.getDoorMotor().setPosition(DOOR_OPEN);

  // Blink Action LED
  Task* blink_action_led =
    new Blink(blink_period, &(components.getLedAction()));
  scheduler.addTask(blink_action_led);

  // Drone distance measurement
  Task* distance_measurement = new DistanceMeasurement(
    distance_measurement_period, &(components.getDistanceDetector()), this);
  scheduler.addTask(distance_measurement);
}

StateName StateTakeoff::getName() const { return StateName::Takeoff; }

bool StateTakeoff::goNext() {
  return distance > DIST1 && (millis() - timeDroneOutside) > TIME1 * s_to_ms;
}

void StateTakeoff::updateDistance(const float distance) {
  this->distance = distance;
  if (timeDroneOutside == 0) {
    timeDroneOutside = millis();
  }
}

StateTakeoff::~StateTakeoff() {
  F_DEBUG_PRINT("D:Destructing Takeoff");
  components.getDoorMotor().off(); // Shut down motor
  scheduler.removeLastTask();      // Remove distance meter task
  scheduler.removeLastTask();      // Remove blink task
}
