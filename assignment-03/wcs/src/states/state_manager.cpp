#include "state_manager.h"
#include "automatic/automatic.h"
#include "config.h"
#include "manual/manual.h"
#include "scheduler.h"
#include "states.h"
#include "tasks/communication_service/communication_service.h"
#include "tasks/communication_service/data.h"
#include <stdint.h>

StateManager::StateManager(Scheduler& scheduler)
  : scheduler(scheduler)
  , state(new StateAutomatic(VALVE_CLOSED)) {
  CommunicationService::getInstance().setCallback(RX_COMMAND::GET_STATE, this);
}

void StateManager::switchState() {
  if (state->goNext()) {
    const StateName name = state->getName();
    const uint8_t percentage = state->getPercentage();
    delete (state);
    asm volatile("" ::: "memory"); // Do not reorder instructions

    switch (name) {
      case StateName::Manual:
        state = new StateAutomatic(percentage);
        break;
      case StateName::Automatic:
        state = new StateManual(scheduler, percentage);
        break;
    }
  }
}

StateName StateManager::getCurrentState() const { return state->getName(); }

tx_data StateManager::callback(const rx_data& data) {
  switch (data.cmd) {
    case RX_COMMAND::GET_STATE:
      return tx_data{ .cmd = TX_COMMAND::STATE,
                      .val = { .state = state->getName() } };
    default:
      return tx_data{ .cmd = TX_COMMAND::INVALID };
  }
}
