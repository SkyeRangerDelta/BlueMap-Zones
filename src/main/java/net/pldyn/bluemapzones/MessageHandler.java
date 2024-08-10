package net.pldyn.bluemapzones;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.awt.*;

public class MessageHandler {
  public static void send( CommandSender sender, String message ) {
    sender.sendMessage( message );
  }

  public static void send( CommandSender sender, String message, NamedTextColor color) {
    final TextComponent tc = Component.text( message )
        .color( color );

    sender.sendMessage( tc );
  }
}
