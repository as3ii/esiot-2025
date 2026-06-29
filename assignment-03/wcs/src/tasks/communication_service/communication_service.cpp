#include "communication_service.h"
#include "command_callback.h"
#include "config.h"
#include "data.h"
#include "debug.h"
#include <message_service.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <task.h>

CommunicationService::CommunicationService(const uint32_t period)
  : Task(period)
  , msg_srv(MessageService::getInstance(UART_SPEED)) {}

CommunicationService& CommunicationService::getInstance() {
  return getInstance(state_manager_task_period);
}

CommunicationService& CommunicationService::getInstance(const uint32_t period) {
  static CommunicationService instance(period);
  return instance;
}

void CommunicationService::tick() {
  // TODO: parse and validate eventual checksum
  if (msg_srv.isMessageAvailable()) {
    const uint8_t len = msg_srv.readMessage(rx_buffer, BUFFER_LEN);
    // Expected messages: "Q:" followed by a valid RX_COMMAND, messages
    // Es. "Q:\0" "Q:\1|123" with \i integer referring to a valid RX_COMMAND
    // separated by newline
    if (len >= 3 && rx_buffer[0] == 'Q' && rx_buffer[1] == ':') {
      const int8_t i_cmd = bitmap_to_index(rx_buffer[2]);
      if (i_cmd >= 0 && callbacks[i_cmd] != nullptr) {
        DEBUG_PRINTF("D:Calling callback for %u", rx_buffer[2]);
        uint8_t percentage = 0;
        // Q : RX_COMMAND | [0-100] -> 5 to 7 chars
        constexpr uint8_t perc_msg_len = 5;
        if (len >= perc_msg_len && rx_buffer[3] == '|') {
          char buff[4]; // max 3 digits + null
          strncpy(buff, &(rx_buffer[4]), 4);
          buff[3] = '\0';
          percentage = atoi(buff);
        }
        composeMessage(callbacks[i_cmd]->callback(rx_data{
          .cmd = (RX_COMMAND)rx_buffer[2], .percentage = percentage }));
      } else {
        // Invalid received value or callback function not registered
        F_DEBUG_PRINT("D:Invalid or unregistered command");
        composeMessage(tx_data{ .cmd = TX_COMMAND::INVALID });
      }
    } else {
      // Invalid message
      DEBUG_PRINTF("D:Invalid message '%s'", rx_buffer);
    }
  }
}

// Format a response message: "R:" followed by a valid TX_COMMAND and optional
// data separated by '|'. The message must end with a newline (implicitly
// added by `MessageService::sendMessageBlock()` in this case)
using formatter = int (*)(char* buf, size_t len, const tx_data& arg);

static int format_state(char* buf, size_t len, const tx_data& arg) {
  return snprintf(buf,
                  len,
                  "R:%c|%c",
                  static_cast<uint8_t>(arg.cmd),
                  static_cast<uint8_t>(arg.val.state));
}
static int format_simple(char* buf, size_t len, const tx_data& arg) {
  return snprintf(buf, len, "R:%c", static_cast<uint8_t>(arg.cmd));
}
static int format_uint8(char* buf, size_t len, const tx_data& arg) {
  return snprintf(
    buf, len, "R:%c|%u", static_cast<uint8_t>(arg.cmd), arg.val.percentage);
}

// Formatter dispatch table.
// Maps TX_COMMAND (INVALID excluded) to the relevant formatter
static const formatter FORMATTERS[TX_COMMANDS_COUNT - 1] = {
  format_state,  // STATE
  format_uint8,  // OPENING
  format_simple, // ACK_OPENING
  format_simple, // ACK_UNCONNECTED
  format_simple, // ACK_CONNECTED
};

// TODO: add CRC
int16_t CommunicationService::composeMessage(const tx_data& argument) {
  int16_t len = 0;

  if (argument.cmd == TX_COMMAND::INVALID) {
    len = format_simple(tx_buffer, BUFFER_LEN, argument);
  } else {
    len = FORMATTERS[bitmap_to_index(static_cast<uint8_t>(argument.cmd))](
      tx_buffer, BUFFER_LEN, argument);
  }

  if (len < 0) { // Error composing the message, do not send anything
    F_DEBUG_PRINT("D:Error composing message");
    return -1;
  }
  if (len > BUFFER_LEN) { // Output truncated, ensure null termination
    F_DEBUG_PRINT("D:Message truncated");
    tx_buffer[BUFFER_LEN - 1] = '\0';
    len = BUFFER_LEN;
  }
  msg_srv.sendMessageBlock(tx_buffer);
  return len;
}

void CommunicationService::setCallback(const RX_COMMAND command,
                                       CommandCallback* callback) {
  DEBUG_PRINTF(callback != nullptr ? "D:Registering callback %u"
                                   : "D:De-registering callback %u",
               static_cast<uint8_t>(command));
  // Not checking for errors (-1) because `command` should be valid
  callbacks[bitmap_to_index(static_cast<uint8_t>(command))] = callback;
}

// Return the index of the first set bit in the input value.
// input uint8_t -> will always return integers in range [-1..7]
// -1 is return if no bit is set.
// If multiple bits are set, only the first one is considered
int8_t CommunicationService::bitmap_to_index(const uint8_t number) {
  // NOLINTNEXTLINE(cppcoreguidelines-avoid-magic-numbers,readability-magic-numbers)
  for (int8_t i = 0; i < 8; i++) {
    if ((number & (1U << i)) != 0U) {
      return i;
    }
  }
  // Should not happens
  return -1;
}
