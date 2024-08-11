package net.pldyn.bluemapzones;

public class PCLocationHistory {
  private String lastAreaName = null;
  private String lastNonConflictedAreaName = null;

  public PCLocationHistory( String lastChunkName, String lastNonConflictedChunkName ) {
    this.lastAreaName = lastChunkName;
    this.lastNonConflictedAreaName = lastNonConflictedChunkName;
  }

  public String getLastAreaName() {
    return lastAreaName;
  }

  public void setLastAreaName( String lastAreaName ) {
    this.lastAreaName = lastAreaName;
  }

  public String getLastNonConflictedAreaName() {
    return lastNonConflictedAreaName;
  }

  public void setLastNonConflictedAreaName( String lastNonConflictedAreaName ) {
    this.lastNonConflictedAreaName = lastNonConflictedAreaName;
  }
}
