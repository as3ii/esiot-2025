#include "landing.h"
#include "components.h"
#include "config.h"
#include "debug.h"
#include "states/states.h"
#include "tasks/blink/blink.h"
#include "tasks/communication_service/communication_service.h"
#include "tasks/communication_service/data.h"
#include "tasks/distance_measurement/distance_measurement.h"
#include <Arduino.h>
#include <lcd_screen.h>
#include <scheduler.h>
#include <stdint.h>
#include <task.h>

constexpr uint32_t s_to_ms = 1000;

StateLanding::StateLanding(Scheduler& scheduler)
  : components(Components::getInstance())
  , scheduler(scheduler) {
  F_DEBUG_PRINT("D:Entered state Landing");

  LcdScreen& lcd = components.getLcdScreen();
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("LANDING");

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

  CommunicationService::getInstance().setCallback(RX_COMMAND::GET_DISTANCE,
                                                  this);
}

StateName StateLanding::getName() const { return StateName::Landing; }

bool StateLanding::goNext() {
  return distance < DIST2 && (millis() - timeDroneInside) > TIME2 * s_to_ms;
}

void StateLanding::updateDistance(const float distance) {
  this->distance = distance;
  if (timeDroneInside == 0) {
    timeDroneInside = millis();
  }
}

data StateLanding::callback(const RX_COMMAND command) {
  switch (command) {
    case RX_COMMAND::GET_DISTANCE:
      return data{ .cmd = TX_COMMAND::DISTANCE, .val = { .f = distance } };
    default:
      return data{ .cmd = TX_COMMAND::INVALID };
  }
}

StateLanding::~StateLanding() {
  F_DEBUG_PRINT("D:Destructing Landing");
  components.getDoorMotor().off(); // Shut down motor
  scheduler.removeLastTask();      // Remove distance meter task
  scheduler.removeLastTask();      // Remove blink task
  CommunicationService::getInstance().setCallback(RX_COMMAND::GET_DISTANCE,
                                                  nullptr);
}
