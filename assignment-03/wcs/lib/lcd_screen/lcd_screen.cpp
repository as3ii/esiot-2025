#include "lcd_screen.h"
#include <LiquidCrystal_I2C.h>
#include <stdint.h>

LcdScreen::LcdScreen(const uint8_t addr, const uint8_t cols, const uint8_t rows)
  : lcd(LiquidCrystal_I2C(addr, cols, rows)) {
  lcd.init();
  lcd.backlight();
}

void LcdScreen::setCursor(const uint8_t col, const uint8_t row) {
  lcd.setCursor(col, row);
}

void LcdScreen::print(const char* const text) { lcd.print(text); }

void LcdScreen::clear() { lcd.clear(); }
