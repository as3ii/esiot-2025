#ifndef CONFIG_H
#define CONFIG_H

#include <stdint.h>

constexpr uint32_t wait_connection = 500; // ms
constexpr uint32_t message_period = 1000; // ms

constexpr uint32_t serial_speed = 115200;

constexpr uint8_t green_led = 15;
constexpr uint8_t red_led = 2;

constexpr uint8_t trigger_pin = 17;
constexpr uint8_t echo_pin = 16;

constexpr uint16_t task_stack_size = 4096;
constexpr float moving_average_alpha = 0.2; // 0 < alpha < 1, lower = smoother
constexpr uint8_t measurements_count = 16;
// Wait at least this time between measurements
constexpr uint8_t measurements_delay_ms = 125; // 1000 / 8Sps = 125ms

#ifdef WIFI_SSID
constexpr const char* ssid = WIFI_SSID;
#else
#error "WIFI_SSID is not set"
#endif
#ifdef WIFI_PASSWORD
constexpr const char* password = WIFI_PASSWORD;
#else
#error "WIFI_PASSWORD is not set"
#endif

constexpr const char* server = "broker.mqtt-dashboard.com";
constexpr uint16_t port = 1883;
constexpr const char* topic = "esiot-2025-ld";
// client_name must contain a %u to tell where the random bits should be placed
constexpr const char* client_name_fmt = "esp32-esiot-2025-%u";
constexpr uint8_t client_name_length = 30;

constexpr uint8_t buffer_size = 40;

#endif
