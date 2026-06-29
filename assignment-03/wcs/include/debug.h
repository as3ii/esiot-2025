#ifndef DEBUG_H
#define DEBUG_H

#include <stdint.h>

// DEBUG == 1 -> enable debug.
#ifndef DEBUG
#define DEBUG 1
#endif

// DEBUG_FLASH == 1 -> F_DEBUG_PRINT(str) stores str in flash memory.
#ifndef DEBUG_FLASH
#define DEBUG_FLASH 1
#endif

#if DEBUG == 1
#include "../lib/message_service/message_service.h"
#include <WString.h>
#include <stdarg.h>
#include <stdio.h>
// Buffer length used for formatting
static constexpr uint8_t FMT_BUFF_LEN = 64;
// Debug print without formatting.
inline void DEBUG_PRINT(const char* str) {
  MessageService::getInstance().sendMessageBlock(str);
}
// Debug print without formatting, uses a string stored in flash memory.
inline void DEBUG_PRINT(const __FlashStringHelper* str) {
  MessageService::getInstance().sendMessageBlock(str);
}
#if DEBUG_FLASH == 1
// NOLINTNEXTLINE(cppcoreguidelines-macro-usage,cppcoreguidelines-pro-type-reinterpret-cast)
#define F_DEBUG_PRINT(str) DEBUG_PRINT(F(str))
#else // DEBUG_FLASH
#define F_DEBUG_PRINT(str) DEBUG_PRINT(str)
#endif // DEBUG_FLASH
// Debug print with formatting. Max 127 chars + null terminator allowed
inline void DEBUG_PRINTF(const char* str, ...) {
  char buff[FMT_BUFF_LEN];
  va_list args{};
  va_start(args, str);
  vsnprintf(buff, FMT_BUFF_LEN, str, args);
  MessageService::getInstance().sendMessageBlock(buff);
  va_end(args);
}
#else // DEBUG
inline void DEBUG_PRINT(...) {}
inline void DEBUG_PRINTF(...) {}
#define F_DEBUG_PRINT(str)
#endif // DEBUG

#endif // DEBUG_H
