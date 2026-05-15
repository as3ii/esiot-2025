#ifndef BUTTON_LISTENER_H
#define BUTTON_LISTENER_H

class ButtonListener {
public:
  // Called when the button is pressed
  virtual void buttonPressed() = 0;
  // Destructor
  virtual ~ButtonListener() = default;
};

#endif
