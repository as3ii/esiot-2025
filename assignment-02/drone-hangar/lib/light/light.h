#ifndef LIGHT_H
#define LIGHT_H

class Light {
public:
  virtual void switchOn() const = 0;
  virtual void switchOff() const = 0;
  virtual void setState(bool state) const = 0;
  // Destructor
  virtual ~Light() = default;
};

#endif
