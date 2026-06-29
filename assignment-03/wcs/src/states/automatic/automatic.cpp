#include "automatic.h"
#include "components.h"
#include "config.h"
#include "states/states.h"
#include "tasks/communication_service/communication_service.h"
#include "tasks/communication_service/data.h"
#include <Arduino.h>
#include <button.h>
#include <lcd_screen.h>
#include <pins_arduino.h>
#include <stdint.h>
#include <stdio.h>

static volatile bool go_next;

static void button_pressed() { go_next = true; }

static void print_lcd(const char* first_line, const uint8_t percentage) {
  char str[LCD_COLS];
  LcdScreen& lcd = Components::getInstance().getLcdScreen();
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print(first_line);
  lcd.setCursor(0, 1);
  snprintf(str, LCD_COLS, "Valve: %3u", percentage);
  lcd.print(str);
}

StateAutomatic::StateAutomatic(const uint8_t percentage)
  : components(Components::getInstance())
  , percentage(percentage) {
  print_lcd("AUTOMATIC", percentage);

  go_next = false;

  // Button interrupt, instead of polling using a task
  attachInterrupt(digitalPinToInterrupt(components.getButton().getPin()),
                  button_pressed,
                  FALLING);

  CommunicationService& com = CommunicationService::getInstance();
  com.setCallback(RX_COMMAND::GET_OPENING, this);
  com.setCallback(RX_COMMAND::SET_OPENING, this);
  com.setCallback(RX_COMMAND::SET_CONNECTED, this);
  com.setCallback(RX_COMMAND::SET_UNCONNECTED, this);
};

StateName StateAutomatic::getName() const { return StateName::Automatic; }

bool StateAutomatic::goNext() { return go_next; }

uint8_t StateAutomatic::getPercentage() const { return percentage; }

tx_data StateAutomatic::callback(const rx_data& data) {
  LcdScreen& lcd = components.getLcdScreen();
  char str[LCD_COLS];

  switch (data.cmd) {
    case RX_COMMAND::GET_OPENING:
      return tx_data{ .cmd = TX_COMMAND::OPENING,
                      .val = { .percentage = percentage } };
    case RX_COMMAND::SET_OPENING:
      // NOLINTNEXTLINE(cppcoreguidelines-avoid-magic-numbers,readability-magic-numbers,-warnings-as-errors)
      if (data.percentage > 100) {
        return tx_data{ .cmd = TX_COMMAND::INVALID };
      }
      percentage = data.percentage;
      // NOLINTNEXTLINE(cppcoreguidelines-avoid-magic-numbers,readability-magic-numbers,-warnings-as-errors)
      components.getMotorDriver()->setPosition(percentage * 90 / 100);
      // Don't clear LCD nor rewrite first line
      lcd.setCursor(0, 1);
      snprintf(str, LCD_COLS, "Valve: %3u", percentage);
      lcd.print(str);
      return tx_data{ .cmd = TX_COMMAND::ACK_OPENING };
    case RX_COMMAND::SET_UNCONNECTED:
      print_lcd("UNCONNECTED", percentage);
      return tx_data{ .cmd = TX_COMMAND::ACK_UNCONNECTED };
    case RX_COMMAND::SET_CONNECTED:
      print_lcd("AUTOMATIC", percentage);
      return tx_data{ .cmd = TX_COMMAND::ACK_CONNECTED };
    default:
      return tx_data{ .cmd = TX_COMMAND::INVALID };
  }
}

StateAutomatic::~StateAutomatic() {
  // Detach button interrupt
  detachInterrupt(digitalPinToInterrupt(components.getButton().getPin()));

  CommunicationService& com = CommunicationService::getInstance();
  com.setCallback(RX_COMMAND::GET_OPENING, nullptr);
  com.setCallback(RX_COMMAND::SET_OPENING, nullptr);
  com.setCallback(RX_COMMAND::SET_CONNECTED, nullptr);
  com.setCallback(RX_COMMAND::SET_UNCONNECTED, nullptr);
}
