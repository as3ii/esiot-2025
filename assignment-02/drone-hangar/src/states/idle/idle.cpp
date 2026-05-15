#include "idle.h"
#include "components.h"
#include "config.h"
#include "servomotor.h"
#include "states/states.h"
#include "tasks/communication_service/communication_service.h"
#include "tasks/communication_service/data.h"
#include <lcd_screen.h>

StateIdle::StateIdle()
  : components(Components::getInstance()) {
  DEBUG_PRINT("D:Entered state Idle");

  LcdScreen& lcd = components.getLcdScreen();
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("DRONE INSIDE");

  Servomotor& motor = components.getDoorMotor();
  motor.on();
  motor.setPosition(DOOR_CLOSE);

  CommunicationService::getInstance().setCallback(REQ_TAKE_OFF, this);
}

StateName StateIdle::getName() const { return StateName::Idle; }

bool StateIdle::goNext() { return req_received; }

data StateIdle::callback(const RX_COMMAND command) {
  switch (command) {
    case REQ_TAKE_OFF:
      req_received = true;
      return data{ .cmd = ACK_TAKE_OFF };
    default:
      return data{ .cmd = INVALID };
  }
}

StateIdle::~StateIdle() {
  DEBUG_PRINT("D:Destructing Idle");
  components.getDoorMotor().off();
  CommunicationService::getInstance().setCallback(REQ_TAKE_OFF, nullptr);
}
