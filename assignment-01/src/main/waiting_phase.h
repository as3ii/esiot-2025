#ifndef WAITING_PHASE_H
#define WAITING_PHASE_H

#include "model.h"
#include <LiquidCrystal_I2C.h>

// Handle waiting phase
// bool isPhaseChanged: set to true only the first time after phase change
STATUS waiting_phase(bool isPhaseChanged, LiquidCrystal_I2C* lcd);

#endif
