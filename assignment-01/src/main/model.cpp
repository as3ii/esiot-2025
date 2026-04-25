#include "model.h"
#include "game_phase.h"
#include <stdint.h>
#include <stdio.h>

static inline const char* getGamePhaseName(const GAME_PHASE phase) {
  switch (phase) {
    case SHOW_SEQUENCE:
      return "show sequence";
    case REPLICATE_SEQUENCE:
      return "replicate sequence";
    case END_ROUND:
      return "end round";
    default:
      return "Invalid game phase";
  }
}

void genErrorMsg(char* str, uint8_t len, const SYS_PHASE phase) {
  switch (phase) {
    case WAITING:
      snprintf(str, len, "[ERR] WaitingPhase");
      break;
    case GAME:
      snprintf(
        str, len, "[ERR] GamePhase (%s)", getGamePhaseName(getGamePhase()));
      break;
    default:
      snprintf(str, len, "[ERR] Invalid phase");
      break;
  }
}
