#ifndef SLEEP_H
#define SLEEP_H

#include <stdint.h>
#include <task.h>

class Sleep : public Task {
public:
  explicit Sleep(uint32_t period);
  void tick() final;
};

#endif
