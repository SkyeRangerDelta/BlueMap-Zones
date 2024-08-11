package net.pldyn.bluemapzones;

import java.util.ArrayList;

public interface ZoneGenerationCallback {
  void onZoneGenerationComplete(ArrayList<ZonedShape> zonedShapes);
}
