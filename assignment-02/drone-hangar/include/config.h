#ifndef CONFIG_H
#define CONFIG_H

#include <pins_arduino.h>
#include <stdint.h>

constexpr uint32_t UART_SPEED = 115200;

// LCD
constexpr uint8_t LCD_ADDR = 0x27;
constexpr uint8_t LCD_COLS = 16;
constexpr uint8_t LCD_ROWS = 2;

// Leds
constexpr uint8_t LED_ON = 4;     // L1
constexpr uint8_t LED_ACTION = 3; // L2
constexpr uint8_t LED_ALARM = 2;  // L3

// Servo
constexpr uint8_t SERVO = 5;
constexpr uint8_t DOOR_CLOSE = 0;
constexpr uint8_t DOOR_OPEN = 180;

// Sonar
constexpr uint8_t SONAR_TRIG = 7;
constexpr uint8_t SONAR_ECHO = 8;

// Button
constexpr uint8_t BTN = 9;

// PIR
constexpr uint8_t PIR = 10;

// Temperature sensor
constexpr uint8_t TEMP = A0;

// Takeoff variables
constexpr uint32_t TIME1 = 10; // Time to wait in seconds
constexpr float DIST1 = 0.5;   // Drone distance threshold in meters

// Landing variables
constexpr uint32_t TIME2 = 10; // Time to wait in seconds
constexpr float DIST2 = 0.1;   // Drone distance threshold in meters

// Temperature monitoring thresholds
constexpr float TEMP_WARNING = 30.0;      // Temperature in celsius
constexpr uint32_t TIME_WARNING = 30000;  // Time in ms
constexpr float TEMP_CRITICAL = 45.0;     // Temperature in celsius
constexpr uint32_t TIME_CRITICAL = 10000; // Time in ms

// Time in ms, preferably same as or multiple of scheduler period
constexpr uint32_t distance_measurement_period = 200;
constexpr uint32_t blink_period = 500;
constexpr uint32_t temperature_measurement_period = 1000;
constexpr uint32_t temperature_measurement_err_period = 300;
constexpr uint32_t state_manager_task_period = 200;
constexpr uint32_t button_check_period = 100;

constexpr float nominal_room_temperature = 20.0;

#endif
