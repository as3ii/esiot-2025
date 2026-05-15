#ifndef BUTTON_H
#define BUTTON_H

#include <stdint.h>

constexpr uint16_t DEBOUNCE_MS = 200;

class Button {
private:
  uint8_t pin;
  uint16_t debounce_ms;
  unsigned long last_pressed_time = 0;

public:
  explicit Button(uint8_t pin, uint16_t debounce_ms = DEBOUNCE_MS);
  bool isPressed() const;
  // Returns true if the button is pressed and more then `debounce_ms` is passed
  bool isPressedDebounced();
  // Destructor
  virtual ~Button() = default;
};

#endif
