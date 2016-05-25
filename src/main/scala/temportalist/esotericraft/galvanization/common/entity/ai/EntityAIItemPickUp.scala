package temportalist.esotericraft.galvanization.common.entity.ai

import com.google.common.base.Predicate
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.{Entity, EntityCreature}
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import temportalist.esotericraft.api.galvanize.ai.{AIEmpty, EntityAIEmpty}
import temportalist.esotericraft.galvanization.common.Galvanize
import temportalist.origin.api.common.lib.Vect

import scala.collection.{JavaConversions, mutable}

/**
  *
  * Created by TheTemportalist on 5/19/2016.
  *
  * @author TheTemportalist
  */
@AIEmpty(displayName = "Pick Up Items", name = "itemPickUp", modid = Galvanize.MOD_ID)
class EntityAIItemPickUp(
		private val owner: EntityCreature
) extends EntityAIHelper
		with IEntityAIInventory
		with IEntityAIOrigin
		with EntityAIEmpty
		with IEntityAIFlyCheck {

	private val radiusXZ: Double = 4
	private val radiusY: Double = 1
	private val speed: Double = 1D

	this.setMutexBits(EnumAIMutex.EVERYTHING_OKAY)
	this.checkCanFly(this.owner)

	override def initWith(infoStack: ItemStack): Unit = {

		if (infoStack.hasTagCompound && infoStack.getTagCompound.hasKey("origin")) {
			val origin = Vect.readFrom(infoStack.getTagCompound, "origin")
			val face = EnumFacing.values()(infoStack.getTagCompound.getInteger("face_ordinal"))
			this.setOrigin(origin, face)
		}

	}

	def getCollectionArea: AxisAlignedBB = {
		if (this.getPosition == null) null
		else {
			new AxisAlignedBB(
				this.getPosition.x - this.radiusXZ,
				this.getPosition.y + 0.5 - this.radiusY,
				this.getPosition.z - this.radiusXZ,
				this.getPosition.x + this.radiusXZ,
				this.getPosition.y + 0.5 + this.radiusY,
				this.getPosition.z + this.radiusXZ
			)
		}
	}

	override def shouldExecute(): Boolean = {
		if (this.getPosition == null) return false

		val entities = this.findEntitiesInRange
		val exec = entities.nonEmpty
		exec
	}

	override def updateTask(): Unit = {
		val entityList = this.findEntitiesInRange

		var smallestDistanceToOrigin = -1D
		var closestEntityToOrigin: EntityItem = null
		var distanceToOrigin: Double = 0D
		for (entity <- entityList) {
			entity match {
				case entityItem: EntityItem =>
					distanceToOrigin = this.calculateDistanceToOriginFromEntity(entity)
					if (smallestDistanceToOrigin < 0 ||
							distanceToOrigin < smallestDistanceToOrigin) {
						smallestDistanceToOrigin = distanceToOrigin
						closestEntityToOrigin = entityItem
					}
				case _ =>
			}
		}

		//Galvanize.log("Going for an item")

		val ownerDistanceToEntity = (new Vect(this.owner) - new Vect(closestEntityToOrigin)).length
		if (ownerDistanceToEntity <= 1.25D) {
			this.pickUpItem(closestEntityToOrigin)
		}
		else {
			this.moveEntityTowards(this.owner, closestEntityToOrigin, this.speed, this.getCanFly)
		}
	}

	final def findEntitiesInRange: mutable.Buffer[Entity] = {
		val entList = this.owner.getEntityWorld.getEntitiesInAABBexcluding(
			this.owner, this.getCollectionArea,
			new Predicate[Entity] {
				override def apply(input: Entity): Boolean = {
					input.isInstanceOf[EntityItem]
				}
			}
		)
		JavaConversions.asScalaBuffer(entList)
	}

	final def calculateDistanceToOriginFromEntity(entity: Entity): Double = {
		(new Vect(entity) - this.getPosition).length
	}

	final def pickUpItem(entityItem: EntityItem): Unit = {
		val stackRemaining = this.addItemToHands(entityItem.getEntityItem.copy(), this.owner)
		if (stackRemaining == null) entityItem.setDead()
		else entityItem.setEntityItemStack(stackRemaining)
	}

}
