#ifndef TIMER_H
#define TIMER_H

#include <stdint.h>

class Timer {
private:
  Timer();
  volatile bool timerFlag = false;

public:
  static Timer& getInstance();
  void setTimerFlag(bool flag) volatile;
  void setupFreq(uint32_t freq);
  /* period in ms */
  void setupPeriod(uint32_t period);
  void waitForNextTick() volatile;

  Timer(Timer const&) = delete;          // Unwanted method
  void operator=(Timer const&) = delete; // Unwanted method
};

#endif
