package openblocks.common.tileentity;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import openblocks.common.TrophyHandler.Trophy;
import openmods.api.*;
import openmods.sync.SyncableInt;
import openmods.tileentity.SyncedTileEntity;

import com.google.common.base.Preconditions;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityTrophy extends SyncedTileEntity implements IPlaceAwareTile, IActivateAwareTile, ICustomHarvestDrops, ICustomPickItem {

	public static Trophy debugTrophy = Trophy.Wolf;
	private int cooldown = 0;
	private SyncableInt trophyIndex;

	public TileEntityTrophy() {}

	@Override
	protected void createSyncedFields() {
		trophyIndex = new SyncableInt(-1);
	}

	public Trophy getTrophy() {
		int trophyId = trophyIndex.get();
		return trophyId >= 0? Trophy.VALUES[trophyId] : null;
	}

	@Override
	public void updateEntity() {
		super.updateEntity();
		if (!worldObj.isRemote) {
			Trophy trophy = getTrophy();
			if (trophy != null) trophy.executeTickBehavior(this);
			if (cooldown > 0) cooldown--;
		}
	}

	@Override
	public boolean onBlockActivated(EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if (!worldObj.isRemote) {
			Trophy trophyType = getTrophy();
			if (trophyType != null) {
				trophyType.playSound(worldObj, xCoord, yCoord, zCoord);
				if (cooldown <= 0) cooldown = trophyType.executeActivateBehavior(this, player);
			}
		}
		return true;
	}

	@Override
	public void onBlockPlacedBy(EntityPlayer player, ForgeDirection side, ItemStack stack, float hitX, float hitY, float hitZ) {
		boolean set = false;
		if (stack.hasTagCompound()) {
			NBTTagCompound tag = stack.getTagCompound();
			if (tag.hasKey("entity")) {
				String entityKey = tag.getString("entity");
				trophyIndex.set(Trophy.valueOf(entityKey).ordinal());
				set = true;
			}
		}

		if (!worldObj.isRemote) {
			if (!set) {
				int next = (debugTrophy.ordinal() + 1) % Trophy.VALUES.length;
				debugTrophy = Trophy.VALUES[next];
				trophyIndex.set(debugTrophy.ordinal());
			}

			sync();
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		cooldown = tag.getInteger("cooldown");
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setInteger("cooldown", cooldown);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void prepareForInventoryRender(Block block, int metadata) {
		Preconditions.checkElementIndex(metadata, Trophy.VALUES.length);
		super.prepareForInventoryRender(block, metadata);
		trophyIndex.set(metadata);
	}

	@Override
	public boolean suppressNormalHarvestDrops() {
		return true;
	}

	@Override
	public void addHarvestDrops(EntityPlayer player, List<ItemStack> drops) {
		final Trophy trophy = getTrophy();
		if (trophy != null) drops.add(trophy.getItemStack());
	}

	@Override
	public ItemStack getPickBlock() {
		final Trophy trophy = getTrophy();
		return trophy != null? trophy.getItemStack() : null;
	}

}
