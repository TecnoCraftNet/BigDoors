package nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation;

import java.util.List;

import org.bukkit.Location;

import nl.pim16aap2.bigDoors.util.BlockData;

public interface GetNewLocation
{
	public Location getNewLocation(List<BlockData> savedBlocks, double xPos, double yPos, double zPos, int index);
}