#ifndef COMMAND_CALLBACK_H
#define COMMAND_CALLBACK_H

#include "data.h"

class CommandCallback {
public:
  virtual tx_data callback(const rx_data& data) = 0;
  // Destructor
  virtual ~CommandCallback() = default;
};

#endif
