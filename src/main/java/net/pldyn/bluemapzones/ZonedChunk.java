package net.pldyn.bluemapzones;

import com.flowpowered.math.vector.Vector2d;

import java.util.ArrayList;

public class ZonedChunk {
    private Vector2d chunkId;
    private boolean isConflicted = false;
    private ArrayList<ZonedShape> owners = new ArrayList<>();

    public ZonedChunk(Vector2d chunkId) {
        this.chunkId = chunkId;
    }

    public Vector2d getChunkId() {
        return chunkId;
    }

    public boolean isConflicted() {
        return isConflicted;
    }

    public void setChunkId(Vector2d chunkId) {
        this.chunkId = chunkId;
    }

    public ArrayList<ZonedShape> getOwners() {
        return owners;
    }

    public void setConflicted(boolean conflicted) {
        isConflicted = conflicted;
    }

    public void setOwners(ArrayList<ZonedShape> owners) {
        this.owners = owners;
    }

    public void removeOwner(ZonedShape owner) {
        this.owners.remove(owner);
    }

    public void addOwner(ZonedShape owner) {
        this.owners.add(owner);
    }

    public String getName() {
        if (owners.size() == 1) {
            return owners.get(0).getLabel();
        }

        String newName = "Boundary between ";
        for (int i = 0; i < owners.size(); i++) {
            if (i == owners.size() - 1) {
                newName = newName.concat("and " + owners.get(i).getLabel() + ".");
            }
            else {
                newName = newName.concat(owners.get(i).getLabel() + ", ");
            }
        }

        return newName;

    }
}
