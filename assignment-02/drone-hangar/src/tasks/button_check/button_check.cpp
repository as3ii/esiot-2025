#include "button_check.h"
#include "button_listener.h"
#include "task.h"
#include <button.h>
#include <stdint.h>

ButtonCheck::ButtonCheck(const uint32_t period,
                         Button* button,
                         ButtonListener* listener)
  : Task(period)
  , button(button)
  , listener(listener) {}

void ButtonCheck::tick() {
  if (button->isPressedDebounced()) {
    listener->buttonPressed();
  }
}
