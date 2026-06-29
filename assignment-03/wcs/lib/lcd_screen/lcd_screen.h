#ifndef LCD_SCREEN_H
#define LCD_SCREEN_H

#include <LiquidCrystal_I2C.h>
#include <stdint.h>

static constexpr uint8_t ADDR = 0x27;
static constexpr uint8_t COLS = 16;
static constexpr uint8_t ROWS = 2;

class LcdScreen {
private:
  LiquidCrystal_I2C lcd;

public:
  explicit LcdScreen(uint8_t addr = ADDR,
                     uint8_t cols = COLS,
                     uint8_t rows = ROWS);
  void setCursor(uint8_t col, uint8_t row);
  void print(const char* text);
  void clear();
};

#endif
