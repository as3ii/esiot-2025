#include "blink.h"
#include "light.h"
#include <stdint.h>
#include <task.h>

Blink::Blink(const uint32_t period, Light* light)
  : Task(period)
  , light(light)
  , state(state::OFF) {}

void Blink::tick() {
  switch (state) {
    case state::OFF:
      light->switchOn();
      state = state::ON;
      break;
    case state::ON:
      light->switchOff();
      state = state::OFF;
      break;
  }
}
