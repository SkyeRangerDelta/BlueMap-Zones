package net.pldyn.bluemapzones;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Shape;
import jdk.jfr.Description;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

public class ZoneGenerator {
    private static final Logger Log = Logger.getLogger("BM Zones");
    private static final ArrayList<ZonedShape> zonedShapes = new ArrayList<>();
    public ZoneGenerator(BlueMapAPI blueMapAPI) {
        Log.info("API loaded.");
        BlueMapMap workingMap = findConfMaps(blueMapAPI.getMaps());

        if (workingMap == null) {
            Log.warning("Couldn't find the map to load!");
            return;
        }

        MarkerSet objectiveSet = findMarkerSets(workingMap);
        if (objectiveSet == null) {
            Log.warning("Couldn't find the marker set to load!");
            return;
        }

        handleMarkerSet(objectiveSet);
    }

    public ArrayList<ZonedShape> getZonedShapes() {
        return zonedShapes;
    }

    private BlueMapMap findConfMaps(Collection<BlueMapMap> loadedWorlds) {
        String confWorld = (String) ConfigHandler.getPluginConfFile().get("Maps.name");
        for (BlueMapMap m : loadedWorlds) {
            if (m.getId().equals(confWorld)) {
                Log.info("Found an operating world. (" + m.getName() + ")");
                return m;
            }
        }

        return null;
    }

    private MarkerSet findMarkerSets(BlueMapMap world) {
        String markerID = (String) ConfigHandler.getPluginConfFile().get("Maps.marker-set");
        Map<String, MarkerSet> markerSets = world.getMarkerSets();
        Log.info("World has " + markerSets.size() + " marker sets.");

        if (markerSets.isEmpty()) {
            Log.info("Map has no marker sets!");
            return null;
        }

        if (!markerSets.containsKey(markerID)) {
            Log.info("Marker set requested is not available!");
            return null;
        }

        MarkerSet mSet = markerSets.get(markerID);

        if (mSet != null) Log.info("Found the configured marker set. (" + mSet.getLabel() + ")");
        return mSet;
    }

    private void handleMarkerSet(MarkerSet markerSet) {
        Map<String, Marker> setMarkers = markerSet.getMarkers();
        Log.info("Cataloging " + setMarkers.size() + " markers.");
        int shapeCount = 0;
        for (Map.Entry<String, Marker> entry : setMarkers.entrySet()) {
            Log.info("Thinking about shape " + ++shapeCount + " of " + setMarkers.size());
            String key = entry.getKey();
            Marker value = entry.getValue();
            assert false;
            zonedShapes.add(catalogMarker(key, value, zonedShapes));
        }
    }

    private ZonedShape catalogMarker(String k, Marker m, ArrayList<ZonedShape> chunkList) {
        if (!(m instanceof ShapeMarker shapeMarker)) return null;
        Shape markerShape = shapeMarker.getShape();
        Vector2d[] markerPoints = markerShape.getPoints();
        ZonedShape newZone = new ZonedShape(m.getLabel(), markerShape, ((ShapeMarker) m).getShapeY());

        int vCount = 0;
        Vector2d lastChunk = new Vector2d(0, 0);

        Log.info("Processing " + newZone.getLabel() + " with " + markerPoints.length + " vertex point(s).");
        for (Vector2d vertex : markerPoints) {
            int chID_X = vertex.getFloorX() / 16;
            int chID_Y = vertex.getFloorY() / 16;

            Vector2d chID = new Vector2d(chID_X, chID_Y);

            //Are we in a new chunk?
            if (chID.equals(lastChunk)) continue;

            //Skip if ID already listed
            if (newZone.getOwnedChunks().contains(chID)) continue;

            //Is conflicted?
            if (conflictedChunk(chID)) continue;

            Log.info("Adding chunk ID (" + chID_X + ", " + chID_Y + ").");
            newZone.addOwnedChunk(chID);
            lastChunk = chID;
        }
        Log.info(newZone.getLabel() + " has " + newZone.getOwnedChunks().size() + " chunks.");

        return newZone;
    }

    @Description("Returns a the ShapedChunk if it shares contains the tested Chunk ID")
    private boolean conflictedChunk(Vector2d chunkID) {
        for (ZonedShape zonedShape : zonedShapes) {
            ArrayList<Vector2d> ownedChunks = zonedShape.getOwnedChunks();
            if (ownedChunks.contains(chunkID)) {
                Log.info("Detected chunk conflict with " + zonedShape.getLabel());
                return true;
            };
        }

        return false;
    }
}
