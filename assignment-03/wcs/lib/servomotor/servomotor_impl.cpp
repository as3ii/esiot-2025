#include "servomotor_impl.h"
#include <stdint.h>

ServomotorImpl::ServomotorImpl(const uint8_t pin)
  : pin(pin) {}

void ServomotorImpl::on() {
  if (!motor.attached()) {
    motor.attach(pin);
  }
}

void ServomotorImpl::setPosition(const uint8_t angle) {
  // NOLINTBEGIN(cppcoreguidelines-avoid-magic-numbers,readability-magic-numbers)
  constexpr float coeff = (2250.0 - 750.0) / 180;
  motor.write((int)(750 + (angle * coeff)));
  // NOLINTEND(cppcoreguidelines-avoid-magic-numbers,readability-magic-numbers)
}

void ServomotorImpl::off() {
  if (motor.attached()) {
    motor.detach();
  }
}
