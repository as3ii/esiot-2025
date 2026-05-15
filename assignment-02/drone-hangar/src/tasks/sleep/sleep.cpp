#include "sleep.h"
#include <stdint.h>
#include <task.h>

Sleep::Sleep(const uint32_t period)
  : Task(period) {}

void Sleep::tick() {}
