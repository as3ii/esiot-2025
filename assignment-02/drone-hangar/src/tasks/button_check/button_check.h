#ifndef TASK_BUTTON_CHECK_H
#define TASK_BUTTON_CHECK_H

#include "button_listener.h"
#include <button.h>
#include <stdint.h>
#include <task.h>

class ButtonCheck : public Task {
  Button* button;
  ButtonListener* listener;

public:
  ButtonCheck(uint32_t period, Button* button, ButtonListener* listener);
  void tick() final;
};

#endif
