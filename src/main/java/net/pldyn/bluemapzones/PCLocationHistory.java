package net.pldyn.bluemapzones;

import java.util.HashMap;
import java.util.Map;

/**
 * Where a player currently is, tracked separately for every zone level.
 *
 * <p>Two names are kept per level. The plain area name is whatever the player is
 * standing in, including a shared boundary. The non-conflicted name is the last real
 * zone they were in, which is what stops a spurious re-announcement when someone steps
 * onto a border chunk and back into the zone they came from.</p>
 */
public class PCLocationHistory {

  private final Map<Integer, String> areaByLevel = new HashMap<>();
  private final Map<Integer, String> nonConflictedByLevel = new HashMap<>();

  /**
   * @method getAreaName - The area the player is in at a level.
   * @param level The zone level.
   * @return The area name, or null if they are in no zone at that level.
   */
  public String getAreaName(int level) {
    return areaByLevel.get( level );
  }

  public void setAreaName(int level, String areaName) {
    if (areaName == null) {
      areaByLevel.remove( level );
      return;
    }

    areaByLevel.put( level, areaName );
  }

  /**
   * @method getNonConflictedAreaName - The last non-boundary area the player occupied
   *     at a level.
   * @param level The zone level.
   * @return The area name, or null if they have not been in a zone at that level.
   */
  public String getNonConflictedAreaName(int level) {
    return nonConflictedByLevel.get( level );
  }

  public void setNonConflictedAreaName(int level, String areaName) {
    if (areaName == null) {
      nonConflictedByLevel.remove( level );
      return;
    }

    nonConflictedByLevel.put( level, areaName );
  }
}
