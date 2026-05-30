#ifndef DATA_H
#define DATA_H

// Received commands bit set
#include "states/states.h"
#include <stdint.h>

constexpr uint8_t RX_COMMANDS_COUNT = 5;

enum class RX_COMMAND : uint8_t {
  GET_STATE = 1,       // Get system state
  REQ_TAKE_OFF = 2,    // Request take off
  REQ_LANDING = 4,     // Request landing
  GET_TEMPERATURE = 8, // Get current temperature
  GET_DISTANCE = 16    // Get distance, available only while landing
};

constexpr uint8_t TX_COMMANDS_COUNT = 6;

// Send commands bit set
enum class TX_COMMAND : uint8_t {
  STATE = 1,        // Followed by the current state
  ACK_TAKE_OFF = 2, // No other parameters
  ACK_LANDING = 4,  // No other parameters
  TEMPERATURE = 8, // Followed by the current temperature, with 2 decimal values
  DISTANCE = 16,   // Followed by the current drone distance in meters with 2
                   // decimal values
  INVALID = 127,   // No other parameters
};

using data = struct data {
  TX_COMMAND cmd;
  union {
    StateName state;
    float f;
  } val;
};

#endif
