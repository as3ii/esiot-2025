#ifndef SCHEDULER_H
#define SCHEDULER_H

#include "task.h"
#include "timer.h"
#include <stdint.h>

constexpr uint8_t MAX_TASKS = 10;

class Scheduler {
private:
  uint32_t basePeriod;
  uint8_t nTasks = 0;
  Task* taskList[MAX_TASKS] = {};
  Timer& timer;

public:
  explicit Scheduler(uint32_t basePeriod);
  bool addTask(Task* task);
  void schedule();
  bool removeLastTask();

  Scheduler(Scheduler const&) = delete;      // Unwanted method
  void operator=(Scheduler const&) = delete; // Unwanted method
};

#endif
