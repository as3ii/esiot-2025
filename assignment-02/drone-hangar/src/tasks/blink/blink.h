#ifndef TASK_BLINK_H
#define TASK_BLINK_H

#include <light.h>
#include <stdint.h>
#include <task.h>

class Blink : public Task {
private:
  Light* light;
  enum class state : bool { ON, OFF } state;

public:
  Blink(uint32_t period, Light* light);
  void tick() final;
};

#endif
