#include "manual.h"
#include "components.h"
#include "config.h"
#include "scheduler.h"
#include "states/states.h"
#include "tasks/communication_service/communication_service.h"
#include "tasks/communication_service/data.h"
#include "tasks/manual_handling/manual_handling.h"
#include <Arduino.h>
#include <lcd_screen.h>
#include <pins_arduino.h>
#include <stdint.h>
#include <stdio.h>
#include <task.h>

static volatile bool go_next;

static void button_pressed() { go_next = true; }

StateManual::StateManual(Scheduler& scheduler, const uint8_t percentage)
  : scheduler(scheduler)
  , components(Components::getInstance())
  , percentage(percentage) {
  LcdScreen& lcd = components.getLcdScreen();
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("MANUAL");
  char str[LCD_COLS];
  lcd.setCursor(0, 1);
  snprintf(str, LCD_COLS, "Valve: %3u", percentage);
  lcd.print(str);

  go_next = false;

  Task* manual = new ManualHandling(potentiometer_measurement_period, this);
  scheduler.addTask(manual);

  // Button interrupt, instead of polling using a task
  attachInterrupt(digitalPinToInterrupt(components.getButton().getPin()),
                  button_pressed,
                  FALLING);

  CommunicationService& com = CommunicationService::getInstance();
  com.setCallback(RX_COMMAND::GET_OPENING, this);
  com.setCallback(RX_COMMAND::SET_CONNECTED, this);
  com.setCallback(RX_COMMAND::SET_UNCONNECTED, this);
}

StateName StateManual::getName() const { return StateName::Manual; }

bool StateManual::goNext() { return go_next; }

uint8_t StateManual::getPercentage() const { return percentage; }

void StateManual::setPercentage(const uint8_t percentage) {
  this->percentage = percentage;
  LcdScreen& lcd = Components::getInstance().getLcdScreen();
  char str[LCD_COLS];
  lcd.setCursor(0, 1);
  snprintf(str, LCD_COLS, "Valve: %3u", percentage);
  lcd.print(str);
}

tx_data StateManual::callback(const rx_data& data) {
  switch (data.cmd) {
    case RX_COMMAND::GET_OPENING:
      return tx_data{ .cmd = TX_COMMAND::OPENING,
                      .val = { .percentage = percentage } };
    case RX_COMMAND::SET_UNCONNECTED:
      // Do nothing
      return tx_data{ .cmd = TX_COMMAND::ACK_UNCONNECTED };
    case RX_COMMAND::SET_CONNECTED:
      // Do nothing
      return tx_data{ .cmd = TX_COMMAND::ACK_CONNECTED };
    default:
      return tx_data{ .cmd = TX_COMMAND::INVALID };
  }
}

StateManual::~StateManual() {
  scheduler.removeLastTask();

  // Detach button interrupt
  detachInterrupt(digitalPinToInterrupt(components.getButton().getPin()));

  CommunicationService& com = CommunicationService::getInstance();
  com.setCallback(RX_COMMAND::GET_OPENING, nullptr);
  com.setCallback(RX_COMMAND::SET_CONNECTED, nullptr);
  com.setCallback(RX_COMMAND::SET_UNCONNECTED, nullptr);
}
