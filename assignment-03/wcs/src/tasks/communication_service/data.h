#ifndef DATA_H
#define DATA_H

// Received commands bit set
#include "states/states.h"
#include <stdint.h>

constexpr uint8_t RX_COMMANDS_COUNT = 5;

enum class RX_COMMAND : uint8_t {
  GET_STATE = 1,   // Get system state
  GET_OPENING = 2, // Get the current valve position percentage
  SET_OPENING = 4, // Set the valve opening to the following percentage
  // Set the system to state Unconnected (allowed only if current state is
  // Automatic)
  SET_UNCONNECTED = 8,
  // Set the system to state Automatic (allowed only if current state is
  // Unconnected)
  SET_CONNECTED = 16,
};

constexpr uint8_t TX_COMMANDS_COUNT = 6;

// Send commands bit set
enum class TX_COMMAND : uint8_t {
  STATE = 1,       // Followed by the current state
  OPENING = 2,     // Followed by the current valve position percentage
  ACK_OPENING = 4, // No other parameters
  ACK_UNCONNECTED = 8,
  ACK_CONNECTED = 16,
  INVALID = 127, // No other parameters
};

using tx_data = struct tx_data {
  TX_COMMAND cmd;
  union {
    StateName state;
    uint8_t percentage;
  } val;
};

using rx_data = struct rx_data {
  RX_COMMAND cmd;
  uint8_t percentage;
};

#endif
