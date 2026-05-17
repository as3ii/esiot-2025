#include "message_service.h"
#include <HardwareSerial.h>
#include <WString.h>
#include <stdint.h>

MessageService::MessageService(const uint32_t speed) { Serial.begin(speed); }

MessageService& MessageService::getInstance() {
  return getInstance(high_speed);
}

MessageService& MessageService::getInstance(const uint32_t speed) {
  static MessageService instance(speed);
  return instance;
}

// NOLINTNEXTLINE(readability-convert-member-functions-to-static)
bool MessageService::isMessageAvailable() const {
  return Serial.available() > 0;
}

// Reads a line from serial in the given buffer and return the number of
// characters read. If no message is available, returns 0.
// NOLINTNEXTLINE(readability-convert-member-functions-to-static)
uint8_t MessageService::readMessage(char* buffer, const uint8_t len) const {
  if (isMessageAvailable()) {
    const uint8_t bytes_read = Serial.readBytesUntil('\n', buffer, len - 1);
    // NOLINTNEXTLINE(cppcoreguidelines-pro-bounds-pointer-arithmetic)
    buffer[bytes_read] = '\0'; // Null-terminate the string
    return bytes_read;
  }
  return 0;
}

// Send the given message with final `\n`.
// NOLINTNEXTLINE(readability-convert-member-functions-to-static)
void MessageService::sendMessage(const char* const message) {
  Serial.println(message);
}

// NOLINTNEXTLINE(readability-convert-member-functions-to-static)
void MessageService::sendMessage(const __FlashStringHelper* message) {
  Serial.println(message);
}

// Send the given message with final `\n`, then wait for the transmission
// completion. NOLINTNEXTLINE(readability-convert-member-functions-to-static)
void MessageService::sendMessageBlock(const char* const message) {
  Serial.println(message);
  Serial.flush();
}

// NOLINTNEXTLINE(readability-convert-member-functions-to-static)
void MessageService::sendMessageBlock(const __FlashStringHelper* message) {
  Serial.println(message);
  Serial.flush();
}
