#include "config.h"
#include <Arduino.h>
#include <PubSubClient.h>
#include <WiFi.h>
#include <WiFiClient.h>
#include <WiFiType.h>
#include <cstdint>
#include <freertos/portmacro.h>
#include <freertos/projdefs.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <ultrasonic_sensor.h>

static WiFiClient wifiClient;
static PubSubClient client(wifiClient);
static UltrasonicSensor distance_meter(trigger_pin, echo_pin);

static char client_name[client_name_length];
static char msg[buffer_size];
static uint32_t last_msg_time = 0;
static uint32_t serial_number = 0;

static QueueHandle_t queue;

enum class State : uint8_t { WORKING, RECONNECTING };

static State currentState = State::RECONNECTING;

static void setup_wifi() {
  WiFiClass::mode(WIFI_STA);
  WiFi.begin(ssid, password);
  while (WiFiClass::status() != WL_CONNECTED) {
    Serial.print("Waiting for WiFi: ");
    Serial.println(ssid);
    delay(wait_connection);
  }
  Serial.print("WiFi connected, IP: ");
  Serial.println(WiFi.localIP());
}

static void reconnect_mqtt() {
  Serial.print("Reconnecting to MQTT server: ");
  Serial.println(server);

  snprintf(
    client_name, client_name_length, client_name_fmt, (uint16_t)random());

  if (client.connect(client_name)) {
    Serial.print("Connected to MQTT with name: ");
    Serial.println(client_name);
  } else {
    Serial.print("Connection failed, rc=");
    Serial.println(client.state());
    delay(wait_connection);
  }
}

static int comp(const void* ptr_a, const void* ptr_b) {
  float A = *((float*)ptr_a);
  float B = *((float*)ptr_b);
  if (A > B) {
    return 1;
  }
  if (A < B) {
    return -1;
  }
  return 0;
}

static float median(const float* buff, uint8_t count) {
  float sorted[measurements_count];
  for (uint8_t i = 0; i < count; i++) {
    sorted[i] = buff[i];
  }
  qsort(sorted, count, sizeof(float), comp);
  if (count % 2 == 0) {
    return (sorted[(count / 2) - 1] + sorted[count / 2]) / 2.0F;
  }
  return sorted[count / 2];
}

static void sensorReadingTask(void* /*params*/) {
  float readings[measurements_count];
  uint8_t index = 0;
  uint8_t count = 0;
  float level = NAN;
  TickType_t xLastWakeTime = xTaskGetTickCount();

  while (true) {
    float reading = distance_meter.getDistance();
    if (reading >= 0) {
      readings[index] = reading;
      index = (index + 1) % measurements_count;
      if (count < measurements_count) {
        count++;
      }

      // float sum = 0;
      // for (float& reading : readings) {
      //   sum += reading;
      // }
      // level = sum / (float)count;

      if (isnan(level)) {
        level = median(readings, count);
      } else {
        level = (moving_average_alpha * median(readings, count)) +
                ((1.0F - moving_average_alpha) * level);
      }
      xQueueOverwrite(queue, &level);
    }
    // Use non-blocking precise delay
    vTaskDelayUntil(&xLastWakeTime, pdMS_TO_TICKS(measurements_delay_ms));
  }
}

static void monitoring() {
  client.loop();

  const uint32_t now = millis();
  if ((now - last_msg_time) > message_period) {
    last_msg_time = now;
    float level = NAN;
    if (xQueuePeek(queue, &level, 0) == pdTRUE) {
      snprintf(
        msg, buffer_size, R"({"seq":%u,"level":%.2f})", serial_number, level);
      if (client.publish(topic, msg)) {
        serial_number += 1;
        Serial.print("Sent message: ");
        Serial.println(msg);
      } else {
        Serial.println("Publish failed");
      }
    }
  }
}

inline static void run() {
  switch (currentState) {
    case State::WORKING:
      monitoring();
      break;
    case State::RECONNECTING:
      if (WiFiClass::status() != WL_CONNECTED) {
        setup_wifi();
      }
      reconnect_mqtt();
      break;
  }
}

inline static void check() {
  if (!client.connected()) {
    currentState = State::RECONNECTING;
    digitalWrite(green_led, LOW);
    digitalWrite(red_led, HIGH);
  } else {
    currentState = State::WORKING;
    digitalWrite(green_led, HIGH);
    digitalWrite(red_led, LOW);
  }
}

extern void setup() {
  pinMode(green_led, OUTPUT);
  pinMode(red_led, OUTPUT);

  digitalWrite(green_led, LOW);
  digitalWrite(red_led, HIGH);

  Serial.begin(serial_speed);
  setup_wifi();
  randomSeed(esp_random());
  client.setServer(server, port);

  queue = xQueueCreate(1, sizeof(float));
  if (queue == nullptr) {
    Serial.println("Failed queue creation. Rebooting");
    delay(message_period);
    esp_restart();
  }
  // Pin to core 0, loop() is pinned to core 1 by default
  xTaskCreatePinnedToCore(
    sensorReadingTask, "SensorReader", task_stack_size, nullptr, 1, nullptr, 0);
}

extern void loop() {
  run();
  check();
}
