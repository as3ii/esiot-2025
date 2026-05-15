#ifndef DISTANCE_LISTENER_H
#define DISTANCE_LISTENER_H

class DistanceListener {
public:
  // Called to update the distance
  virtual void updateDistance(float distance) = 0;
  // Destructor
  virtual ~DistanceListener() = default;
};

#endif
