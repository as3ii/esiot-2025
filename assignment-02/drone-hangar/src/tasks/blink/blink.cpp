#include "blink.h"
#include "light.h"
#include <stdint.h>
#include <task.h>

Blink::Blink(const uint32_t period, Light* light)
  : Task(period)
  , light(light)
  , state(OFF) {}

void Blink::tick() {
  switch (state) {
    case OFF:
      light->switchOn();
      state = ON;
      break;
    case ON:
      light->switchOff();
      state = OFF;
      break;
  }
}
