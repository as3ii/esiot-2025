#ifndef TASK_COMMUNICATION_SERVICE_H
#define TASK_COMMUNICATION_SERVICE_H

#include "command_callback.h"
#include "data.h"
#include <message_service.h>
#include <stdint.h>
#include <task.h>

static constexpr uint8_t BUFFER_LEN = 16;

class CommunicationService final : public Task {
private:
  MessageService& msg_srv;
  CommandCallback* callbacks[RX_COMMANDS_COUNT]{};
  char tx_buffer[BUFFER_LEN]{};
  char rx_buffer[BUFFER_LEN]{};

  CommunicationService(uint32_t period);
  // Convert bitmap to array index.
  // Returns -1 if no bit is set
  static inline int8_t bitmap_to_index(uint8_t number);

public:
  static CommunicationService& getInstance();
  static CommunicationService& getInstance(uint32_t period);
  void tick() final;
  int16_t composeMessage(const tx_data& argument);
  void setCallback(RX_COMMAND command, CommandCallback* callback);

  CommunicationService(CommunicationService const&) = delete; // Unwanted method
  void operator=(CommunicationService const&) = delete;       // Unwanted method
};

#endif
