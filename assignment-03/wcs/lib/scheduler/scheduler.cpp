#include "scheduler.h"
#include "task.h"
#include "timer.h"
#include <stdint.h>

Scheduler::Scheduler(const uint32_t basePeriod)
  : basePeriod(basePeriod)
  , timer(Timer::getInstance()) {
  timer.setupPeriod(basePeriod);
}

bool Scheduler::addTask(Task* task) {
  if (nTasks < MAX_TASKS - 1) {
    taskList[nTasks] = task;
    nTasks++;
    return true;
  }
  return false;
}

void Scheduler::schedule() {
  timer.waitForNextTick();
  for (int i = 0; i < nTasks; i++) {
    if (taskList[i]->updateAndCheckTime(basePeriod)) {
      taskList[i]->tick();
    }
  }
}

bool Scheduler::removeLastTask() {
  if (nTasks > 0) {
    nTasks--;
    delete taskList[nTasks];
    return true;
  }
  return false;
}
