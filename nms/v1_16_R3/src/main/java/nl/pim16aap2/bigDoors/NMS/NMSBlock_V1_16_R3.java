package nl.pim16aap2.bigDoors.NMS;

import com.cryptomorin.xseries.XMaterial;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.BlockRotatable;
import net.minecraft.server.v1_16_R3.EnumBlockRotation;
import net.minecraft.server.v1_16_R3.EnumDirection.EnumAxis;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.Item;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_16_R3.block.data.CraftBlockData;

import java.util.Set;

public class NMSBlock_V1_16_R3 extends net.minecraft.server.v1_16_R3.BlockBase implements NMSBlock
{
    private IBlockData blockData;
    private CraftBlockData craftBlockData;
    private XMaterial xmat;
    private Location loc;

    public NMSBlock_V1_16_R3(World world, int x, int y, int z, Info blockInfo)
    {
        super(blockInfo);

        loc = new Location(world, x, y, z);

        // If the block is waterlogged (i.e. it has water inside), unwaterlog it.
        craftBlockData = (CraftBlockData) ((CraftBlock) world.getBlockAt(x, y, z)).getBlockData();
        if (craftBlockData instanceof Waterlogged)
            ((Waterlogged) craftBlockData).setWaterlogged(false);

        constructBlockDataFromBukkit();

        xmat = XMaterial.matchXMaterial(world.getBlockAt(x, y, z).getType().toString()).orElse(XMaterial.BEDROCK);
    }

    @Override
    public boolean canRotate()
    {
        return craftBlockData instanceof MultipleFacing;
    }

    @Override
    public void rotateBlock(RotateDirection rotDir)
    {
        EnumBlockRotation rot;
        switch (rotDir)
        {
        case CLOCKWISE:
            rot = EnumBlockRotation.CLOCKWISE_90;
            break;
        case COUNTERCLOCKWISE:
            rot = EnumBlockRotation.COUNTERCLOCKWISE_90;
            break;
        default:
            rot = EnumBlockRotation.NONE;
        }
        blockData = blockData.a(rot);
    }

    private void constructBlockDataFromBukkit()
    {
        blockData = craftBlockData.getState();
    }

    @Override
    public void putBlock(Location loc)
    {
        this.loc = loc;

        if (craftBlockData instanceof MultipleFacing)
            updateCraftBlockDataMultipleFacing();

        ((CraftWorld) loc.getWorld()).getHandle()
            .setTypeAndData(new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), blockData, 1);
    }

    private void updateCraftBlockDataMultipleFacing()
    {
        Set<BlockFace> allowedFaces = ((MultipleFacing) craftBlockData).getAllowedFaces();
        allowedFaces.forEach((K) ->
        {
            org.bukkit.block.Block otherBlock = loc.clone().add(K.getModX(), K.getModY(), K.getModZ()).getBlock();
            CraftBlockData otherData = (CraftBlockData) ((CraftBlock) otherBlock).getBlockData();

            if (K.equals(BlockFace.UP))
                ((MultipleFacing) craftBlockData).setFace(K, true);
            else if (otherBlock.getType().isSolid())
            {
                ((MultipleFacing) craftBlockData).setFace(K, true);
                if (otherData instanceof MultipleFacing &&
                    (otherBlock.getType().equals(xmat.parseMaterial()) ||
                     (craftBlockData instanceof org.bukkit.block.data.type.Fence &&
                      otherData instanceof org.bukkit.block.data.type.Fence)))
                    if (((MultipleFacing) otherData).getAllowedFaces().contains(K.getOppositeFace()))
                    {
                        ((MultipleFacing) otherData).setFace(K.getOppositeFace(), true);
                        ((CraftBlock) otherBlock).setBlockData(otherData);
                    }
            }
            else
                ((MultipleFacing) craftBlockData).setFace(K, false);
        });
        constructBlockDataFromBukkit();
    }

    @Override
    public void rotateBlockUpDown(boolean NS)
    {
        EnumAxis axis = blockData.get(BlockRotatable.AXIS);
        EnumAxis newAxis = axis;
        switch (axis)
        {
        case X:
            newAxis = NS ? EnumAxis.X : EnumAxis.Y;
            break;
        case Y:
            newAxis = NS ? EnumAxis.Z : EnumAxis.X;
            break;
        case Z:
            newAxis = NS ? EnumAxis.Y : EnumAxis.Z;
            break;
        }
        blockData = blockData.set(BlockRotatable.AXIS, newAxis);
    }

    @Override
    public void rotateCylindrical(RotateDirection rotDir)
    {
        if (rotDir.equals(RotateDirection.CLOCKWISE))
            blockData = blockData.a(EnumBlockRotation.CLOCKWISE_90);
        else
            blockData = blockData.a(EnumBlockRotation.COUNTERCLOCKWISE_90);
    }

    /**
     * Gets the IBlockData (NMS) of this block.
     *
     * @return The IBlockData (NMS) of this block.
     */
    IBlockData getMyBlockData()
    {
        return blockData;
    }

    @Override
    public String toString()
    {
        return blockData.toString();
    }

    @Override
    public void deleteOriginalBlock(boolean applyPhysics)
    {
        if (!applyPhysics)
        {
            loc.getWorld().getBlockAt(loc).setType(Material.AIR, applyPhysics);
        }
        else
        {
            loc.getWorld().getBlockAt(loc).setType(Material.CAVE_AIR, false);
            loc.getWorld().getBlockAt(loc).setType(Material.AIR, true);
        }
    }

    @Override
    public Item getItem()
    {
        return null;
    }

    @Override
    protected Block p()
    {
        return null;
    }
}
