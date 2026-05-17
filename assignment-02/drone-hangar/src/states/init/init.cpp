#include "init.h"
#include "components.h"
#include "debug.h"
#include "states/states.h"

StateInit::StateInit() {
  F_DEBUG_PRINT("D:Entered state Init");

  Components& instance = Components::getInstance();
  instance.getLedOn().switchOn();
  instance.getLedAction().switchOff();
  instance.getLedAlarm().switchOff();
}

StateName StateInit::getName() const { return StateName::Init; }

bool StateInit::goNext() { return true; }

StateInit::~StateInit() { DEBUG_PRINT("D:Destructing Init"); }
