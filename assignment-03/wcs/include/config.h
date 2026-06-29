#ifndef CONFIG_H
#define CONFIG_H

#include <pins_arduino.h>
#include <stdint.h>

constexpr uint32_t UART_SPEED = 115200;

// LCD
constexpr uint8_t LCD_ADDR = 0x27;
constexpr uint8_t LCD_COLS = 16;
constexpr uint8_t LCD_ROWS = 2;

// Button
constexpr uint8_t BTN = 2; // Must support interrupt

// Potentiometer
constexpr uint8_t POT = A0;

// Valve positions
constexpr uint8_t VALVE_MOTOR = 11;
constexpr uint8_t VALVE_CLOSED = 0;
constexpr uint8_t VALVE_OPEN = 90;

constexpr uint8_t MOTOR_MAX_STEP_ANGLE = 5;

// Time in ms, preferably same as or multiple of scheduler period
constexpr uint32_t motor_driver_period = 100;
constexpr uint32_t potentiometer_measurement_period = 200;
constexpr uint32_t state_manager_task_period = 200;
constexpr uint32_t button_check_period = 100;

#endif
