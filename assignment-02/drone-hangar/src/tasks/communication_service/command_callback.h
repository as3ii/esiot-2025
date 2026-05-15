#ifndef COMMAND_CALLBACK_H
#define COMMAND_CALLBACK_H

#include "data.h"

class CommandCallback {
public:
  virtual data callback(RX_COMMAND command) = 0;
  // Destructor
  virtual ~CommandCallback() = default;
};

#endif
