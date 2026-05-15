#ifndef STATE_H
#define STATE_H

#include "states.h"

class State {
public:
  // Return the StateName associated to the State
  virtual StateName getName() const = 0;
  // Return true if the system can switch to the next state
  virtual bool goNext() = 0;
  // Destructor
  virtual ~State() = default;
};

#endif
