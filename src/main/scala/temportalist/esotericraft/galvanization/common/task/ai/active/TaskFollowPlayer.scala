package temportalist.esotericraft.galvanization.common.task.ai.active

import net.minecraft.entity.EntityCreature
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumFacing.Axis
import net.minecraft.util.math.BlockPos
import temportalist.esotericraft.api.galvanize.ai.{EnumTaskType, GalvanizeTask}
import temportalist.esotericraft.api.init.Details
import temportalist.esotericraft.galvanization.common.task.ai.core.TaskBase
import temportalist.esotericraft.galvanization.common.task.ai.interfaces.{ITargetEntity, ITaskSized}

/**
  *
  * Created by TheTemportalist on 5/25/2016.
  *
  * @author TheTemportalist
  */
@GalvanizeTask(modid = Details.MOD_ID,
	name = "followPlayer",
	displayName = "Follow Player"
)
class TaskFollowPlayer(
		pos: BlockPos, face: EnumFacing
) extends TaskBase(pos, face) with ITaskSized with ITargetEntity {

	private val speed: Double = 1.2D
	private val radius: Double = 16D

	// ~~~~~ Task Info ~~~~~

	override def getTaskType: EnumTaskType = EnumTaskType.MOVEMENT_ACTIVE

	// ~~~~~ Bounding Box ~~~~~

	override def getRadius(axis: Axis): Double = axis match {
		case Axis.X => 8
		case Axis.Y => 5
		case Axis.Z => 8
		case _ => 0
	}

	// ~~~~~ AI ~~~~~

	override def shouldExecute(entity: EntityCreature): Boolean = {
		this.setTarget(entity.getEntityWorld.getClosestPlayerToEntity(entity, this.radius))
		this.getTarget != null &&
				this.getTarget.getEntityBoundingBox.intersectsWith(this.getBoundingBox)
	}

	override def updateTask(entity: EntityCreature): Unit = {

		entity.getLookHelper.setLookPositionWithEntity(
			this.getTarget, entity.getHorizontalFaceSpeed + 20,
			entity.getVerticalFaceSpeed
		)

		if (entity.getDistanceSqToEntity(this.getTarget) < 6.25D) {
			// proximity to player
			if (!this.getCanFly) {
				entity.getNavigator.clearPathEntity()
			}
		}
		else {
			this.moveEntityTowards(entity, this.getTarget, this.speed, this.getCanFly)
		}

	}

	override def resetTask(entity: EntityCreature): Unit = {
		this.setTarget(null)
		entity.getNavigator.clearPathEntity()
	}

	// ~~~~~ End ~~~~~

}
