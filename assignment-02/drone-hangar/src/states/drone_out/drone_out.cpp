#include "drone_out.h"
#include "components.h"
#include "config.h"
#include "states/states.h"
#include "task.h"
#include "tasks/communication_service/communication_service.h"
#include "tasks/communication_service/data.h"
#include "tasks/movement_detaction/movement_detection.h"
#include <lcd_screen.h>
#include <scheduler.h>

StateDroneOut::StateDroneOut(Scheduler& scheduler)
  : scheduler(scheduler)
  , components(Components::getInstance()) {
  DEBUG_PRINT("D:Entered state DroneOut");

  LcdScreen& lcd = components.getLcdScreen();
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("DRONE OUT");

  // Close door
  components.getDoorMotor().on();
  components.getDoorMotor().setPosition(DOOR_CLOSE);

  Task* drone_detector_task = new MovementDetection(
    movement_detector_period, &(components.getMovementDetector()), this);
  scheduler.addTask(drone_detector_task);

  CommunicationService::getInstance().setCallback(REQ_LANDING, this);
}

StateName StateDroneOut::getName() const { return StateName::DroneOut; }

void StateDroneOut::droneDetected() { drone_detected = true; }

bool StateDroneOut::goNext() { return drone_detected && req_received; }

data StateDroneOut::callback(const RX_COMMAND command) {
  switch (command) {
    case REQ_LANDING:
      req_received = true;
      return data{ .cmd = ACK_LANDING };
    default:
      return data{ .cmd = INVALID };
  }
}

StateDroneOut::~StateDroneOut() {
  DEBUG_PRINT("D:Destructing DroneOut");
  scheduler.removeLastTask(); // Remove movement detection
  CommunicationService::getInstance().setCallback(REQ_LANDING, nullptr);
}
