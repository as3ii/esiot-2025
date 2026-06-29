#include "timer.h"
#include <avr/interrupt.h>
#include <avr/io.h>
#include <stdint.h>

constexpr uint16_t ocr1a_const = 16 * 1024; // assuming a prescaler of 1024
constexpr uint16_t s_to_ms = 1000;

ISR(TIMER1_COMPA_vect) { Timer::getInstance().setTimerFlag(true); }

Timer::Timer() = default;

Timer& Timer::getInstance() {
  static Timer instance;
  return instance;
}

void Timer::setTimerFlag(const bool flag) volatile { timerFlag = flag; }

// NOLINTNEXTLINE(readability-convert-member-functions-to-static)
void Timer::setupFreq(const uint32_t freq) {
  // disabling interrupt
  cli();

  // NOLINTNEXTLINE(clang-analyzer-core.FixedAddressDereference)
  TCCR1A = 0; // set entire TCCR1A register to 0
  TCCR1B = 0; // same for TCCR1B
  TCNT1 = 0;  // initialize counter value to 0

  /*
   * set compare match register
   *
   * OCR1A = (16*2^20) / (100*PRESCALER) - 1 (must be < 65536)
   *
   * assuming a prescaler = 1024 => OCR1A = (16*2^10)/freq
   */
  OCR1A = ocr1a_const / freq;
  // turn on CTC mode
  TCCR1B |= (1 << WGM12);
  // Set CS11 for 8 prescaler
  TCCR1B |= (1 << CS12) | (1 << CS10);
  // enable timer compare interrupt
  TIMSK1 |= (1 << OCIE1A);

  // enabling interrupt
  sei();
}

/* period in ms */
// NOLINTNEXTLINE(readability-convert-member-functions-to-static)
void Timer::setupPeriod(const uint32_t period) {
  // disabling interrupt
  cli();

  // NOLINTNEXTLINE(clang-analyzer-core.FixedAddressDereference)
  TCCR1A = 0; // set entire TCCR1A register to 0
  TCCR1B = 0; // same for TCCR1B
  TCNT1 = 0;  // initialize counter value to 0

  /*
   * set compare match register
   *
   * OCR1A = (16*2^20) / (100*PRESCALER) - 1 (must be < 65536)
   *
   * assuming a prescaler = 1024 => OCR1A = (16*2^10)* period/1000 (being in ms)
   */
  OCR1A = ocr1a_const * period / s_to_ms;
  // turn on CTC mode
  TCCR1B |= (1 << WGM12);
  // Set CS11 for 8 prescaler
  TCCR1B |= (1 << CS12) | (1 << CS10);
  // enable timer compare interrupt
  TIMSK1 |= (1 << OCIE1A);

  // enabling interrupt
  sei();
}

void Timer::waitForNextTick() volatile {
  /* wait for timer signal */
  while (!timerFlag) {
  }
  timerFlag = false;
}
