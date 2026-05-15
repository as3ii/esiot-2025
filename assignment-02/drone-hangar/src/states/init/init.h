#ifndef STATE_INIT_H
#define STATE_INIT_H

#include "states/state.h"
#include "states/states.h"

class StateInit final : public State {
public:
  StateInit();
  StateName getName() const final;
  bool goNext() final;
  ~StateInit() final;

  StateInit(StateInit const&) = delete;      // Unwanted method
  void operator=(StateInit const&) = delete; // Unwanted method
};

#endif
