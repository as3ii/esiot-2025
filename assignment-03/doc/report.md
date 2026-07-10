# Assignment 03

## Subsystems description

### Tank Monitoring Subsystem (TMS)

TMS has as its main and sole purpose to measure the water level of a tank using
a radar/sonar, and send the measured value to CUS via MQTT.
To satisfy this, the subsystem is composed by an ESP32, 2 LEDs and an ultrasonic
transducer. The sensor is meant to measure the water level from above the tank,
so lower measured value means higher water level.
The ESP32 is programmed in C++ using the Arduino framework and RTOS, at its core
there is a minimal final state machine with just 2 states, `WORKING` and `RECONNECTING`,
used by the main task (the `loop()` function and the functions it calls): the
first handles the sending of messages (JSON-formatted) via MQTT, the second handles
WiFi and MQTT (re)connection. The current state is signaled using 2 LEDs:
the red one represent that a network error was found and the switch to the `RECONNECTING`
state, the green LED represent that the subsystem is working correctly (`WORKING`
state).
Beside the main task, there is an independent task that is executed in the
other CPU core that reads the transducer faster than the required sampling period
to be able to apply a simple median filter followed by exponential smoothing to
reduce the measurements noise. By default, the MQTT messages are sent every second
and the transducer is sampled every 125ms.

### Control Unit Subsystem (CUS)

CUS is the central subsystem and it is the main system coordinator. It handles
TMS' MQTT messages and its eventual disconnection, it talks with WCS via serial
interface (UART), it exposes HTTP REST-like APIs and is able to serve the static files
that compose DBS.
It runs on a PC, it is written in Java (without frameworks) exploiting virtual threads
and asynchronous functions. It is composed by 4 core modules:

- MqttManager handles the reception and parsing of MQTT messages, calling a
  callback of CusController to notify it
- SerialManager handles the serial communication via async functions and a thread
  that wait for incoming messages and parses them
- ApiController handles the web server for the HTTP API and static file serving,
  it uses a thread pool and a queue to handle the incoming requests
- CusController is the main controller and it handles the intercommunication between
  the other modules:
  - polls WCS to gather its general state using a periodically scheduled thread
  - inform WCS of eventual TMS disconnections (detected tracking when the last
    message was received)
  - when WCS is in Automatic mode it controls WCS' valve opening based on TMS
    measurements (via 2 level thresholds) or commands received via API calls.

Thresholds, timeouts and other parameters can be configured via environment variables
or CLI arguments.

### Water Channel Subsystem (WCS)

WCS controls a servomotor that act on the tank valve, shows on a LCD the current
valve opening and a high level state of the system, and permit local manual control
over the valve.
It is based on Arduino and programmed in C++, at its core it has a minimal finite
state machine with two states, `AUTOMATIC` and `MANUAL`, and a task-based synchronous
scheduler.
When in `AUTOMATIC` mode, the subsystem is controlled only via serial interface,
the LCD shows "AUTOMATIC" if TMS is online and "UNCONNECTED" if CUS detect TMS is
offline, and it shows the current valve opening percentage. Pushing the physical button
the subsystem switches in `MANUAL` mode, where WCS only handles "read-only" commands
received via serial interface and the valve opening is directly controlled via the
potentiometer. Pushing the button makes the WCS switch back to `AUTOMATIC` mode.

Note that WCS' `AUTOMATIC` mode is different and separated to the `MANUAL`/`AUTOMATIC`
mode of CUS. Here "automatic" is intended as "remote controlled", opposed to
"manual" which represent the local (manual) valve control.

### DashBoard Subsystem (DBS)

DBS is a simple web interface that calls CUS APIs to show the current system state.
It can be served by any web server or by CUS itself.
This interface shows:

- if CUS is available
- if TMS is online
- the WCS state (operating mode and valve opening)
- a history graph of the water level (by default it shows the last 200 samples)
  DBS allows changing CUS operating mode from Automatic to Manual and controlling the
  valve opening (only if WCS is in Automatic mode)
