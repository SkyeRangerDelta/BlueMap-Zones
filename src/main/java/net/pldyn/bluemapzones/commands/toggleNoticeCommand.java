package net.pldyn.bluemapzones.commands;

import net.kyori.adventure.text.format.NamedTextColor;
import net.pldyn.bluemapzones.NoticeType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.pldyn.bluemapzones.ConfigHandler.getNoticeType;
import static net.pldyn.bluemapzones.ConfigHandler.setNoticeType;
import static net.pldyn.bluemapzones.MessageHandler.send;

public class toggleNoticeCommand implements CommandExecutor, TabExecutor {

  private final BlueMap_Zones plugin = BlueMap_Zones.getInstance();

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
    if (!(sender instanceof Player player)) {
      send( sender, "Only players can change zone notice settings.", NamedTextColor.RED );
      return true;
    }

    UUID pid = player.getUniqueId();
    NoticeType current = getNoticeType( pid );

    // No argument keeps the original toggle behaviour: off if currently on,
    // back to the default if currently off.
    if (args.length == 0) {
      NoticeType next = current == NoticeType.OFF ? NoticeType.DEFAULT : NoticeType.OFF;
      setNoticeType( pid, next );
      announce( player, next );
      return true;
    }

    NoticeType requested = NoticeType.fromId( args[0] );

    if (requested == null) {
      send( player, "Unknown notice type '" + args[0] + "'. Valid types: "
          + String.join( ", ", NoticeType.ids() ) + ".", NamedTextColor.RED );
      return true;
    }

    if (requested == current) {
      send( player, "Your zone notices are already set to " + requested.getId() + ".",
          NamedTextColor.YELLOW );
      return true;
    }

    setNoticeType( pid, requested );
    announce( player, requested );

    return true;
  }

  /**
   * @method announce - Confirm the new setting to the player.
   * @param player The player who ran the command.
   * @param type The type that was just stored.
   */
  private void announce(Player player, NoticeType type) {
    String message = switch (type) {
      case OFF -> "You will no longer receive zone notices.";
      case TITLE -> "Zone notices will now appear on screen.";
      case CHAT -> "Zone notices will now appear in chat.";
      case BOTH -> "Zone notices will now appear on screen and in chat.";
    };

    send( player, message, NamedTextColor.GREEN );
  }

  @Override
  public @Nullable List<String> onTabComplete( @NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
    // Only the first argument is a notice type; anything further takes no completions.
    if (args.length != 1) return new ArrayList<>();

    return StringUtil.copyPartialMatches( args[0], NoticeType.ids(), new ArrayList<>() );
  }

  @Override
  public @Nullable List<String> onTabComplete( @NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
    List<String> completions = new ArrayList<>();

    completions.add( "off" );
    completions.add( "on" );
    completions.add( "chat" );

    return completions;
  }
}
