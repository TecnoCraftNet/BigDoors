package nl.pim16aap2.bigDoors.NMS.v1_13_R2;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import net.minecraft.server.v1_13_R2.Block;
import net.minecraft.server.v1_13_R2.IBlockData;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock_Vall;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_Vall;
import nl.pim16aap2.bigDoors.NMS.NMSBlock_Vall;

public class FallingBlockFactory_V1_13_R2 implements FallingBlockFactory_Vall
{
    // Make a falling block.
    @Override
    public CustomCraftFallingBlock_Vall fallingBlockFactory(BigDoors plugin, Location loc, NMSBlock_Vall block, byte matData, Material mat)
    {
        IBlockData blockData = ((Block) block).getBlockData();
        CustomEntityFallingBlock_V1_13_R2 fBlockNMS = new CustomEntityFallingBlock_V1_13_R2(plugin, loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), blockData);
        CustomCraftFallingBlock_V1_13_R2 entity = new CustomCraftFallingBlock_V1_13_R2(Bukkit.getServer(), fBlockNMS);
        entity.setCustomName("BigDoorsEntity");
        entity.setCustomNameVisible(false);
        return entity;
    }

    @Override
    public NMSBlock_Vall nmsBlockFactory(World world, int x, int y, int z)
    {
        return new NMSBlock_V1_13_R2(world, x, y, z);
    }
}
