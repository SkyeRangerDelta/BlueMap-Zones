package net.pldyn.bluemapzones;

import java.util.ArrayList;
import java.util.List;

/**
 * How a player is told they have entered a new zone.
 *
 * <p>The id is what gets persisted to BMZ-NoticeExclusions.yml and what players
 * type as the argument to /bmz-toggle-notices, so it must stay stable.</p>
 */
public enum NoticeType {
  OFF("off", false, false),
  TITLE("title", true, false),
  CHAT("chat", false, true),
  BOTH("both", true, true);

  /** Applied to any player who has never set a preference. Matches the original behaviour. */
  public static final NoticeType DEFAULT = TITLE;

  private final String id;
  private final boolean showsTitle;
  private final boolean showsChat;

  NoticeType(String id, boolean showsTitle, boolean showsChat) {
    this.id = id;
    this.showsTitle = showsTitle;
    this.showsChat = showsChat;
  }

  public String getId() {
    return id;
  }

  public boolean showsTitle() {
    return showsTitle;
  }

  public boolean showsChat() {
    return showsChat;
  }

  /**
   * @method fromId - Resolve a stored or player-supplied name to a notice type.
   * @param id The name to resolve, case-insensitive.
   * @return The matching type, or null if there is no match.
   */
  public static NoticeType fromId(String id) {
    if (id == null) return null;

    for (NoticeType type : values()) {
      if (type.id.equalsIgnoreCase(id)) return type;
    }

    return null;
  }

  /**
   * @method ids - Every valid notice type name, for tab completion and error messages.
   * @return {List<String>}
   */
  public static List<String> ids() {
    List<String> ids = new ArrayList<>();

    for (NoticeType type : values()) {
      ids.add(type.id);
    }

    return ids;
  }
}
