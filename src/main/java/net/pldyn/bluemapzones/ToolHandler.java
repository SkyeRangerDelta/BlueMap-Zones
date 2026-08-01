package net.pldyn.bluemapzones;

import com.flowpowered.math.vector.Vector2d;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Debug tool: right-click while holding a compass to identify the zone you are in.
 * Works both against a block and in mid air.
 */
public class ToolHandler implements Listener {

  @EventHandler
  public void onPlayerUseTool(PlayerInteractEvent e) {
    // PlayerInteractEvent fires once per hand. Without this the player gets two
    // identical messages for a single right-click.
    if (e.getHand() != EquipmentSlot.HAND) return;

    Action action = e.getAction();
    if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return;

    Player pc = e.getPlayer();
    if (pc.getInventory().getItemInMainHand().getType() != Material.COMPASS) return;

    // Right-clicking air has no block, so fall back to where the player is standing.
    Block interactedBlock = e.getClickedBlock();
    Location target = interactedBlock != null ? interactedBlock.getLocation() : pc.getLocation();

    Vector2d chunkId = new Vector2d(Math.floorDiv(target.getBlockX(), 16),
        Math.floorDiv(target.getBlockZ(), 16));

    // Resolve through MovementHandler so the compass and the notices always agree.
    // Interior chunks are not stored, so this has to ray cast rather than look up.
    String zoneName = BlueMap_Zones.getInstance().movementHandler.resolveAreaName(chunkId);

    MessageHandler.send( pc, "Chunk (" + chunkId.getFloorX() + ", " + chunkId.getFloorY()
        + ") - " + zoneName, NamedTextColor.AQUA );
  }
}
