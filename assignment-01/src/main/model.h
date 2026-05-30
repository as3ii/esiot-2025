#ifndef MODEL_H
#define MODEL_H

#include <HardwareSerial.h>
#include <pins_arduino.h>
#include <stdint.h>

#define DEBUG 1
#define SERIAL_DBG(str)                                                        \
  if (DEBUG) {                                                                 \
    Serial.println(str);                                                       \
    Serial.flush();                                                            \
  }

#ifndef UINT8_MAX
#define UINT8_MAX ((1 << 8) - 1) // 255
#endif

constexpr uint32_t UART_SPEED = 115200;
constexpr uint32_t LCD_ADDR = 0x27;
constexpr uint32_t LCD_COLS = 16;
constexpr uint32_t LCD_ROWS = 2;

constexpr uint16_t debounceDelay = 200; // Time in ms

// Leds
constexpr uint8_t LED_S = 6;
constexpr uint8_t LED_1 = 5;
constexpr uint8_t LED_2 = 4;
constexpr uint8_t LED_3 = 3;
constexpr uint8_t LED_4 = 2;

// Buttons
constexpr uint8_t BTN_1 = 11;
constexpr uint8_t BTN_2 = 10;
constexpr uint8_t BTN_3 = 9;
constexpr uint8_t BTN_4 = 8;

// Potentiometer
constexpr uint8_t POT = A0;

const uint8_t leds[4] = { LED_1, LED_2, LED_3, LED_4 };
const uint8_t buttons[4] = { BTN_1, BTN_2, BTN_3, BTN_4 };

enum SYS_PHASE : uint8_t {
  WAITING,
  GAME,
};

enum GAME_PHASE : uint8_t {
  SHOW_SEQUENCE,
  REPLICATE_SEQUENCE,
  END_ROUND,
};

enum STATUS : uint8_t {
  OK,
  GO_NEXT,
  ERR,
};

// Generate error messages
// char* str: string buffer
// uint8_t len: buffer length
// SYS_PHASE phase: current phase
void genErrorMsg(char* str, uint8_t len, SYS_PHASE phase);

#endif
