package temportalist.esotericraft.common.tile

import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{AxisAlignedBB, BlockPos, ITickable}
import temportalist.esotericraft.api.ApiEsotericraft
import temportalist.origin.api.common.lib.V3O
import temportalist.origin.api.common.tile.ITileSaver
import temportalist.origin.api.common.utility.Stacks

/**
  * Created by TheTemportalist on 1/2/2016.
  */
class TENexusCrystal extends TileEntity with ITickable with ITileSaver {

	private var impartingPlayer: EntityPlayer = null
	private var impartingModuleID: Int = -1
	private var impartingTicks: Int = -1
	private val impartingTicksMax: Int = 20 * 5
	private var impartingBlock: IBlockState = null

	private var triggerBlockPos: BlockPos = null

	override def update(): Unit = {
		if (this.boundingBox == null) this.setBoundingBox(this.getPos)

		if (this.impartingModuleID >= 0) {
			def resetImparting(): Unit = {
				this.impartingModuleID = -1
				this.impartingTicks = -1
				this.impartingPlayer = null
				this.impartingBlock = null
			}
			// decrement ticks
			if (this.impartingTicks >= 0) this.impartingTicks -= 1
			// check player availability every second
			if (this.impartingTicks % 20 == 0 || this.impartingTicks <= 0) {
				val playerNullOrNotUnder = this.impartingPlayer == null ||
						!this.isPlayerUnder(this.impartingPlayer)
				val hasImpartingSacrifice = this.impartingBlock == null ||
						this.getWorld.getBlockState(this.triggerBlockPos) == this.impartingBlock
				if (playerNullOrNotUnder || !hasImpartingSacrifice) {
					ApiEsotericraft.getModule(this.impartingModuleID).
							onImpartingInterrupted(this.impartingPlayer, this,
								this.impartingTicks.toFloat / this.impartingTicksMax)
					resetImparting()
				}
			}
			// impart magic to player
			if (this.impartingTicks <= 0) {
				val module = ApiEsotericraft.getModule(this.impartingModuleID)
				if (module.onImpartingFinished(this.impartingPlayer, this))
					ApiEsotericraft.Player.impart(this.impartingPlayer, module)
				module.getImpartingReturn(this.impartingPlayer, this) match {
					case stack: ItemStack =>
						if (this.impartingPlayer.inventory.addItemStackToInventory(stack))
							Stacks.spawnItemStack(this.getWorld, new V3O(this),
								stack, this.getWorld.rand)
					case state: IBlockState =>
						this.getWorld.setBlockState(this.triggerBlockPos, state)
					case _ =>
				}
				resetImparting()
			}
		}

	}

	// ~~~~~~~~~~~ Create the area bounding box ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	override def setPos(posIn: BlockPos): Unit = {
		super.setPos(posIn)
		this.setBoundingBox(posIn)
		this.triggerBlockPos = posIn.down(4)
	}

	private var boundingBox: AxisAlignedBB = null

	def setBoundingBox(pos: BlockPos): Unit = {
		val posVec = new V3O(pos)
		val center = posVec + V3O.CENTER.suppressedYAxis()
		val radius = 2.5
		val height = 4.0
		val min = center - new V3O(radius, height, radius)
		val max = center + new V3O(radius, 0, radius)
		this.boundingBox = V3O.toAABB(min, max)
	}

	def hasBoundingBox: Boolean = this.boundingBox != null

	def getBoundingBox: AxisAlignedBB = this.boundingBox

	// ~~~~~~~~~~~ NBT Saving ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	override def writeToNBT(tagCom: NBTTagCompound): Unit = {
		super.writeToNBT(tagCom)
		// imparting things are not saved into NBT (should restart if crash or shutdown)

	}

	override def readFromNBT(tagCom: NBTTagCompound): Unit = {
		super.readFromNBT(tagCom)
		// imparting things are not saved into NBT (should restart if crash or shutdown)

	}

	// ~~~~~~~~~~~ Effect handling ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	def tryDoEffect(player: EntityPlayer): Boolean = {
		// make sure the player is around
		if (this.isPlayerUnder(player)) {
			// get the trigger present
			val trigger =
				if (player.getCurrentEquippedItem != null) player.getCurrentEquippedItem
				else this.getWorld.getBlockState(this.triggerBlockPos)
			// get the trigger module for said trigger
			val module = ApiEsotericraft.getModuleForTrigger(trigger)
			// start process if applicable
			if (module != null && module.onImpartingStarted(player, this)) {
				trigger match {
					case stack: ItemStack =>
						var oldStack = player.getCurrentEquippedItem
						oldStack.stackSize -= 1
						if (oldStack.stackSize <= 0) oldStack = null
						player.setCurrentItemOrArmor(0, oldStack)
					case state: IBlockState =>
						//this.getWorld.setBlockToAir(this.triggerBlockPos)
						this.impartingBlock = state
					case _ =>
				}
				this.impartingModuleID = module.getID
				this.impartingTicks = this.impartingTicksMax
				this.impartingPlayer = player
				return true
			}
		}
		false
	}

	private def isPlayerUnder(player: EntityPlayer): Boolean = {
		val bb = this.boundingBox
		val pPos = player.getPositionVector
		val inX = this.boundingBox.minX <= pPos.xCoord && pPos.xCoord <= bb.maxX
		val inY = this.boundingBox.minY <= pPos.yCoord && pPos.yCoord <= bb.maxY
		val inZ = this.boundingBox.minZ <= pPos.zCoord && pPos.zCoord <= bb.maxZ
		this.hasBoundingBox && inX && inY && inZ
	}

}
