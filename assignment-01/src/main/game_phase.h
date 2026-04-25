#ifndef GAME_PHASE_H
#define GAME_PHASE_H

#include "model.h"
#include <LiquidCrystal_I2C.h>

// Handle game phase
// bool isPhaseChanged: set to true only the first time after phase change
STATUS game_phase(bool isPhaseChanged, LiquidCrystal_I2C* lcd);

// Return the current GAME_PHASE
// The returned value refers to the last GAME_PHASE if the current
// PHASE is not "game"
GAME_PHASE getGamePhase();

#endif
