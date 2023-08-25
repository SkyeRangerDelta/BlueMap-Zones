package net.pldyn.bluemapzones;

import com.flowpowered.math.vector.Vector2d;
import it.unimi.dsi.fastutil.Hash;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.HashMap;

public class ToolHandler implements Listener {
    private ArrayList<ZonedShape> zonedShapes = new ArrayList<>();
    public ToolHandler(ArrayList<ZonedShape> zonedShapes) {
        this.zonedShapes = zonedShapes;
    }
    @EventHandler
    public void onPlayerUseTool(PlayerInteractEvent e) {
        Player pc = e.getPlayer();
        Material handItem = pc.getInventory().getItemInMainHand().getType();
        Action completedAction = e.getAction();

        if (completedAction == Action.RIGHT_CLICK_BLOCK) {
            if (handItem == Material.COMPASS) {
                Block interactedBlock = e.getClickedBlock();
                assert interactedBlock != null;
                Location blockLocation = interactedBlock.getLocation();
                Vector2d chunkId = new Vector2d(Math.floorDiv(blockLocation.getBlockX(), 16),
                        Math.floorDiv(blockLocation.getBlockZ(), 16));
                ZonedChunk targetChunk = null;

                for (ZonedShape shape : zonedShapes) {
                    HashMap<Vector2d, ZonedChunk> ownedChunks = shape.getOwnedChunks();
                    if (ownedChunks.containsKey(chunkId)) targetChunk = ownedChunks.get(chunkId);
                }

                String zoneName = targetChunk != null ? targetChunk.getName() : "Wilderness";

                pc.sendMessage("Currently inside chunk (" + chunkId.getX() + ", "
                        + chunkId.getY() + ") - " + zoneName);
            }
        }
    }
}
