#ifndef TASK_H
#define TASK_H

#include <stdint.h>

class Task {
private:
  uint32_t period = 0;
  uint32_t time_elapsed = 0;

public:
  Task(const uint32_t period)
    : period(period) {}

  virtual void tick() = 0;

  bool updateAndCheckTime(const uint32_t base_period) {
    time_elapsed += base_period;
    if (time_elapsed >= period) {
      time_elapsed = 0;
      return true;
    }
    return false;
  }

  virtual ~Task() = default;

  Task(Task const&) = delete;           // Unwanted method
  void operator=(Task const&) = delete; // Unwanted method
};

#endif
