#include "manual_handling.h"
#include "components.h"
#include "potentiometer_listener.h"
#include <potentiometer.h>
#include <stdint.h>
#include <task.h>

ManualHandling::ManualHandling(const uint32_t period,
                               PotentiometerListener* callback)
  : Task(period)
  , potentiometer(Components::getInstance().getPotentiometer())
  , driver(Components::getInstance().getMotorDriver())
  , callback(callback)
  , percentage(potentiometer.read()) {}

void ManualHandling::tick() {
  percentage = potentiometer.read();
  callback->setPercentage(percentage);
  // Cast to uint32_t to avoid overflows during conversion
  // NOLINTNEXTLINE(cppcoreguidelines-avoid-magic-numbers,readability-magic-numbers,-warnings-as-errors)
  driver->setPosition(((uint32_t)percentage) * 90 / 100);
}
