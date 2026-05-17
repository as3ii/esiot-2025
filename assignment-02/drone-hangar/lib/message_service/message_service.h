#ifndef MESSAGE_SERVICE_H
#define MESSAGE_SERVICE_H

#include <WString.h>
#include <stdint.h>

constexpr uint32_t slow_speed = 9600;
constexpr uint32_t high_speed = 115200;

class MessageService {
private:
  MessageService(uint32_t speed);

public:
  static MessageService& getInstance();
  static MessageService& getInstance(uint32_t speed);
  // Send the given message with final `\n`.
  void sendMessage(const char* message);
  void sendMessage(const __FlashStringHelper* message);
  // Send the given message with final `\n`, then wait for the transmission
  // completion.
  void sendMessageBlock(const char* message);
  void sendMessageBlock(const __FlashStringHelper* message);
  // Returns true if a message is available
  bool isMessageAvailable() const;
  // Reads a line from serial in the given buffer and return the number of
  // characters read. If no message is available, returns 0.
  uint8_t readMessage(char* buffer, uint8_t len) const;

  MessageService(MessageService const&) = delete; // Unwanted method
  void operator=(MessageService const&) = delete; // Unwanted method
};

#endif
