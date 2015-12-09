package mcjty.gearswap.blocks;

import mcjty.gearswap.GearSwap;
import mcjty.gearswap.network.PacketHandler;
import mcjty.gearswap.network.PacketRememberSetup;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class GearSwapperBlock extends Block implements ITileEntityProvider {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    //    private IIcon iconSide;
    private String textureName;

    public GearSwapperBlock(Material material, String textureName, String blockName) {
        super(material);
        this.textureName = textureName;
        setUnlocalizedName(blockName);
        setHardness(2.0f);
        setHarvestLevel("pickaxe", 0);
//        setBlockTextureName(textureName);
        setCreativeTab(CreativeTabs.tabMisc);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    public TileEntity createNewTileEntity(World world, int i) {
        return new GearSwapperTE();
    }

    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack itemStack, EntityPlayer player, List list, boolean whatIsThis) {
        NBTTagCompound tagCompound = itemStack.getTagCompound();
        if (tagCompound != null) {

        }
        list.add("This block can remember four different sets of tools, weapons");
        list.add("and armor and allows you to quickly switch between them.");
        list.add("Sneak-left-click to store current hotbar+armor in slot.");
        list.add("Right-click on slot to restore hotbar+armor.");
        list.add("Right-click on bottom to open GUI.");
    }


    public static int getSlot(MovingObjectPosition mouseOver, World world) {
        BlockPos blockPos = mouseOver.getBlockPos();
        int x = blockPos.getX();
        int y = blockPos.getY();
        int z = blockPos.getZ();
        EnumFacing k = getOrientation(world, blockPos);
        if (mouseOver.sideHit == k) {
            float sx = (float) (mouseOver.hitVec.xCoord - x);
            float sy = (float) (mouseOver.hitVec.yCoord - y);
            float sz = (float) (mouseOver.hitVec.zCoord - z);
            return calculateHitIndex(sx, sy, sz, k);
        } else {
            return -1;
        }
    }

//    @SideOnly(Side.CLIENT)
//    public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
//        MovingObjectPosition mouseOver = accessor.getPosition();
//        int index = getSlot(mouseOver, accessor.getWorld());
//        if (index == -1) {
//            currenttip.add("Right-click to access GUI");
//        } else {
//            currenttip.add("Sneak-left-click to store current setup in this slot");
//            currenttip.add("Right-click to restore current setup from this slot");
//        }
//        return currenttip;
//    }

//    @SideOnly(Side.CLIENT)
//    @Override
//    public void registerBlockIcons(IIconRegister iconRegister) {
//        iconSide = iconRegister.registerIcon(textureName);
//    }


    @Override
    public void onBlockClicked(World world, BlockPos pos, EntityPlayer player) {
        if (world.isRemote && player.isSneaking()) {
            // On client. We find out what part of the block was hit and send that to the server.
            MovingObjectPosition mouseOver = Minecraft.getMinecraft().objectMouseOver;
            int index = getSlot(mouseOver, world);
            if (index >= 0) {
                PacketHandler.INSTANCE.sendToServer(new PacketRememberSetup(pos, index));
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float sx, float sy, float sz) {
        if (!world.isRemote) {
            EnumFacing k = getOrientation(world, pos);
            if (side == k) {
                TileEntity tileEntity = world.getTileEntity(pos);
                if (tileEntity instanceof GearSwapperTE) {
                    GearSwapperTE gearSwapperTE = (GearSwapperTE) tileEntity;
                    int index = calculateHitIndex(sx, sy, sz, k);

                    if (index == -1) {
                        player.openGui(GearSwap.instance, GearSwap.GUI_GEARSWAP, world, pos.getX(), pos.getY(), pos.getZ());
                        return true;
                    }

                    gearSwapperTE.restoreSetup(index, player);
                    player.addChatComponentMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Restored hotbar and armor"));
                }
            } else {
                player.openGui(GearSwap.instance, GearSwap.GUI_GEARSWAP, world, pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return true;
    }

    private static int calculateHitIndex(float sx, float sy, float sz, EnumFacing k) {
        int index = -1;
        switch (k) {
            case DOWN:
                if (sz < .13) {
                    return -1;
                }
                index = (sx > .5 ? 1 : 0) + (sz < .54 ? 2 : 0);
                break;
            case UP:
                if (sz > 1-.13) {
                    return -1;
                }
                index = (sx > .5 ? 1 : 0) + (sz > .54 ? 2 : 0);
                break;
            case NORTH:
                if (sy < .13) {
                    return -1;
                }
                index = (sx < .5 ? 1 : 0) + (sy < .54 ? 2 : 0);
                break;
            case SOUTH:
                if (sy < .13) {
                    return -1;
                }
                index = (sx > .5 ? 1 : 0) + (sy < .54 ? 2 : 0);
                break;
            case WEST:
                if (sy < .13) {
                    return -1;
                }
                index = (sz > .5 ? 1 : 0) + (sy < .54 ? 2 : 0);
                break;
            case EAST:
                if (sy < .13) {
                    return -1;
                }
                index = (sz < .5 ? 1 : 0) + (sy < .54 ? 2 : 0);
                break;
        }
        return index;
    }


    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase entityLivingBase, ItemStack itemStack) {
        world.setBlockState(pos, state.withProperty(FACING, getFacingFromEntity(pos, entityLivingBase)), 2);

        NBTTagCompound tagCompound = itemStack.getTagCompound();
        if (tagCompound != null) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof GearSwapperTE) {
                ((GearSwapperTE)te).readRestorableFromNBT(tagCompound);
            }
        }
    }

    @Override
    public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        TileEntity tileEntity = world.getTileEntity(pos);

        if (tileEntity instanceof GearSwapperTE) {
            ItemStack stack = new ItemStack(Item.getItemFromBlock(this));
            NBTTagCompound tagCompound = new NBTTagCompound();
            ((GearSwapperTE)tileEntity).writeRestorableToNBT(tagCompound);

            stack.setTagCompound(tagCompound);
            List<ItemStack> result = new ArrayList<ItemStack>();
            result.add(stack);
            return result;
        } else {
            return super.getDrops(world, pos, state, fortune);
        }
    }

    @Override
    public boolean removedByPlayer(World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        if (willHarvest) return true; // If it will harvest, delay deletion of the block until after getDrops
        return super.removedByPlayer(world, pos, player, willHarvest);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te) {
        super.harvestBlock(world, player, pos, state, te);
        world.setBlockToAir(pos);
    }


//    @Override
//    public IIcon getIcon(int side, int meta) {
//        return iconSide;
//    }
//
//    @Override
//    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
//        return iconSide;
//    }

    private static EnumFacing getOrientation(IBlockAccess world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getValue(FACING);
    }

    public static EnumFacing determineOrientation(int x, int y, int z, EntityLivingBase entityLivingBase) {
        if (MathHelper.abs((float) entityLivingBase.posX - x) < 2.0F && MathHelper.abs((float) entityLivingBase.posZ - z) < 2.0F) {
            double d0 = entityLivingBase.posY + 1.82D - entityLivingBase.getYOffset();

            if (d0 - y > 2.0D) {
                return EnumFacing.UP;
            }

            if (y - d0 > 0.0D) {
                return EnumFacing.DOWN;
            }
        }
        int l = MathHelper.floor_double((entityLivingBase.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        return l == 0 ? EnumFacing.NORTH : (l == 1 ? EnumFacing.EAST : (l == 2 ? EnumFacing.SOUTH : (l == 3 ? EnumFacing.WEST : EnumFacing.DOWN)));
    }

    public static EnumFacing getFacingFromEntity(BlockPos clickedBlock, EntityLivingBase entityIn) {
        if (MathHelper.abs((float) entityIn.posX - clickedBlock.getX()) < 2.0F && MathHelper.abs((float) entityIn.posZ - clickedBlock.getZ()) < 2.0F) {
            double d0 = entityIn.posY + (double) entityIn.getEyeHeight();

            if (d0 - (double) clickedBlock.getY() > 2.0D) {
                return EnumFacing.UP;
            }

            if ((double) clickedBlock.getY() - d0 > 0.0D) {
                return EnumFacing.DOWN;
            }
        }

        return entityIn.getHorizontalFacing().getOpposite();
    }



    @Override
    public boolean isFullCube() {
        return false;
    }


    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    /**
     * Convert the given metadata into a BlockState for this Block
     */
    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, getFacing(meta));
    }

    public static EnumFacing getFacing(int meta)
    {
        int i = meta & 7;
        return i > 5 ? null : EnumFacing.getFront(i);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, FACING);
    }


}