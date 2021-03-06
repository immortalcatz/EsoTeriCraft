package temportalist.esotericraft.emulation.common

import javax.annotation.Nullable

import net.minecraft.client.Minecraft
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.{EntityList, EntityLivingBase, IRangedAttackMob}
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.nbt.{NBTBase, NBTTagCompound}
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.{DamageSource, EnumHand}
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import temportalist.esotericraft.api.emulation.IAbility
import temportalist.esotericraft.emulation.client.{EntityModel, ModelHandler}
import temportalist.esotericraft.emulation.common.ability.AbilityLoader

import scala.collection.JavaConversions
import scala.collection.mutable.ListBuffer

/**
  *
  * Created by TheTemportalist on 5/17/2016.
  *
  * @author TheTemportalist
  */
trait IEntityEmulator {

	private var entityName: String = null
	private var entityState: EntityState = _
	private var abilities: Iterable[IAbility[_ <: NBTBase]] = Array[IAbility[_ <: NBTBase]]()
	@SideOnly(Side.CLIENT)
	private var entityModel: EntityModel[_ <: EntityLivingBase, _ <: EntityLivingBase] = _

	// ~~~~~~~~~~ Getters ~~~~~~~~~~

	final def getEntityName: String = this.entityName

	final def getEntityState: EntityState = this.entityState

	final def getEntityStateInstance(world: World): EntityLivingBase = {
		if (this.entityState != null)
			this.entityState.getInstance(world,
				(instance: EntityLivingBase) => {
					val self = this.getSelfEntityInstance
					// Abilities being removed
					for (ability <- this.abilities) ability.onRemovalFrom(self)
					// Fetch new abilities
					this.abilities = Emulation.getAbilitiesFor(instance)
					// Abilities being added
					for (ability <- this.abilities) ability.onApplicationTo(self)
				}
			)
		else {
			val self = this.getSelfEntityInstance
			// Abilities being removed
			if (this.abilities != null)
				for (ability <- this.abilities) ability.onRemovalFrom(self)
			this.abilities = Array[IAbility[_ <: NBTBase]]()
			null
		}
	}

	final def getEntityAbilities: java.lang.Iterable[IAbility[_ <: NBTBase]] = JavaConversions
			.asJavaIterable(this._getEntityAbilities)

	final def _getEntityAbilities: Iterable[IAbility[_ <: NBTBase]] = this.abilities

	@SideOnly(Side.CLIENT)
	@Nullable
	final def getEntityModelInstance(
			world: World): EntityModel[_ <: EntityLivingBase, _ <: EntityLivingBase] = {
		this.getEntityState match {
			case entityState: EntityState =>
				if (this.entityModel == null)
					this.entityModel = ModelHandler.getEntityModel(entityState.getInstance(world))
				this.entityModel
			case _ => null
		}
	}

	// ~~~~~~~~~~ Setters ~~~~~~~~~~

	final def setEntityName(entityName: String): Unit = {
		this.entityName = entityName
	}

	final def setEntityState(entityName: String, world: World): Unit = {
		if (entityName.isEmpty) {
			this.clearEntityState(world)
			return
		}
		else this.setEntityName(entityName)

		if (world != null) {
			if (!world.isRemote) this.syncEntityDataToClient({
				val tag = new NBTTagCompound
				tag.setString("id", this.entityName)
				tag
			})
			else if (this.entityModel != null) this.entityModel = null

			EntityList.createEntityByName(this.entityName, world) match {
				case living: EntityLivingBase =>
					this.setEntityStateEntity(living)
					this.onEntityConstructed(this.getEntityStateInstance(world))
				case _ =>
			}
		}
	}

	def onEntityConstructed(entity: EntityLivingBase): Unit = {}

	final def setEntityStateEntity(entity: EntityLivingBase): Unit = {
		if (entity == null) this.entityState = null
		else {
			this.entityState = EntityType.create(entity) match {
				case entType: EntityType => new EntityState(entType)
				case _ => null
			}
		}
	}

	final def setEntityState(state: EntityState): Unit = {
		this.entityName = state.getName
		this.entityState = state
		val world = this.getSelfEntityInstance.getEntityWorld
		if (!world.isRemote) {
			this.syncEntityDataToClient(this.entityState.serializeNBT())
			this.onEntityConstructed(this.getEntityStateInstance(world))
		}
		else this.entityModel = null

	}

	final def clearEntityState(world: World): Unit = {
		if (!world.isRemote) this.syncEntityDataToClient()
		else this.entityModel = null
		this.entityName = null
		this.entityState = null
		this.getEntityStateInstance(world)
		this.onEntityStateCleared(world)
	}

	def onEntityStateCleared(world: World): Unit = {}

	// ~~~~~~~~~~ Syncing ~~~~~~~~~~

	protected def syncEntityDataToClient(tag: NBTTagCompound = new NBTTagCompound): Unit

	// ~~~~~~~~~~ Ticking ~~~~~~~~~~

	@SideOnly(Side.CLIENT)
	final def onTickClient(world: World): Unit = {
		if (this.entityState != null) {
			this.onTickEntityState(world)
			this.syncEntityWithSelf(this.getEntityStateInstance(world), this.getSelfEntityInstance)
		}
	}

	final def onTickServer(world: World): Unit = {

		if (this.entityState != null) {
			this.onTickEntityState(world)
		}

	}

	final def onTickEntityState(world: World): Unit = {
		this.entityState.onUpdate()
		val self = this.getSelfEntityInstance
		for (ability <- this._getEntityAbilities) {
			ability.onUpdate(self)
		}
		this.getEntityStateInstance(world) match {
			case living: EntityLivingBase =>
				this.setSizeAndEye(living.width, living.height, living.getEyeHeight,
					this.getSelfEntityInstance)
			case _ => // null
		}
	}

	def getSelfEntityInstance: EntityLivingBase

	// ~~~~~~~~~~ Entity data matching ~~~~~~~~~~

	@SideOnly(Side.CLIENT)
	final def syncEntityWithSelf(ent: EntityLivingBase, self: EntityLivingBase): Unit = {

		//prevs
		ent.prevRotationYawHead = self.prevRotationYawHead
		ent.prevRotationYaw = self.prevRotationYaw
		ent.prevRotationPitch = self.prevRotationPitch
		ent.prevRenderYawOffset = self.prevRenderYawOffset
		ent.prevLimbSwingAmount = self.prevLimbSwingAmount
		ent.prevSwingProgress = self.prevSwingProgress
		ent.prevPosX = self.prevPosX
		ent.prevPosY = self.prevPosY
		ent.prevPosZ = self.prevPosZ

		//currents
		ent.rotationYawHead = self.rotationYawHead
		ent.rotationYaw = self.rotationYaw
		ent.rotationPitch = self.rotationPitch
		ent.renderYawOffset = self.renderYawOffset
		ent.limbSwingAmount = self.limbSwingAmount
		ent.limbSwing = self.limbSwing
		ent.posX = self.posX
		ent.posY = self.posY
		ent.posZ = self.posZ
		ent.motionX = self.motionX
		ent.motionY = self.motionY
		ent.motionZ = self.motionZ
		ent.ticksExisted = self.ticksExisted
		ent.isAirBorne = self.isAirBorne
		ent.moveStrafing = self.moveStrafing
		ent.moveForward = self.moveForward
		ent.dimension = self.dimension
		ent.worldObj = self.worldObj
		// ent.ridingEntity = self.ridingEntity
		ent.hurtTime = self.hurtTime
		ent.deathTime = self.deathTime
		ent.isSwingInProgress = self.isSwingInProgress
		ent.swingProgress = self.swingProgress
		ent.swingProgressInt = self.swingProgressInt

		ent match {
			case bat: EntityBat => bat.setIsBatHanging(false)
			case _ =>
		}

		val prevOnGround: Boolean = ent.onGround
		ent.onGround = self.onGround

		val mc = Minecraft.getMinecraft

		if (self != mc.thePlayer) {
			ent.noClip = false
			ent.setEntityBoundingBox(self.getEntityBoundingBox)
			ent.moveEntity(0.0D, -0.01D, 0.0D)
			ent.posY = self.posY
		}
		ent.noClip = self.noClip

		ent.setSneaking(self.isSneaking)
		ent.setSprinting(self.isSprinting)
		ent.setInvisible(self.isInvisible)
		ent.setHealth(ent.getMaxHealth * (self.getHealth / self.getMaxHealth))

		if (prevOnGround && !ent.onGround) {
			ent match {
				case slime: EntitySlime => slime.squishAmount = 0.6F
				case _ =>
			}
		}

		ent match {
			case dragon: EntityDragon =>
				dragon.prevRotationYaw += 180F
				dragon.rotationYaw += 180F
				dragon.deathTicks = self.deathTime
			case _ =>
		}

		for (i <- EntityEquipmentSlot.values()) {
			if (ent.getItemStackFromSlot(i) == null && self.getItemStackFromSlot(i) != null ||
					ent.getItemStackFromSlot(i) != null && self.getItemStackFromSlot(i) == null ||
					ent.getItemStackFromSlot(i) != null && self.getItemStackFromSlot(i) != null &&
							!ent.getItemStackFromSlot(i)
									.isItemEqual(self.getItemStackFromSlot(i))) {
				ent.setItemStackToSlot(i,
					if (self.getItemStackFromSlot(i) != null) self.getItemStackFromSlot(i).copy
					else null)
			}
		}

		ent match {
			case entPlayer: EntityPlayer =>
				for (hand <- EnumHand.values()) {
					if (entPlayer.getHeldItem(hand) != self.getHeldItem(hand))
						entPlayer.setHeldItem(hand,
							if (self.getHeldItem(hand) == null) null
							else self.getHeldItem(hand).copy())
				}
			case _ =>
		}

	}

	private final def setSizeAndEye(width: Float, height: Float, eyeHeight: Float,
			self: EntityLivingBase): Unit = {
		if (self != null) {

			self match {
				case player: EntityPlayer =>
					player.eyeHeight = eyeHeight
				case _ =>
			}

			// Start; setSize because it protected
			if (width != self.width || height != self.height) {
				self.width = width
				self.height = height
				self.setEntityBoundingBox(new AxisAlignedBB(
					self.posX - width / 2F,
					self.posY,
					self.posZ - width / 2F,
					self.posX + width / 2F,
					self.posY + height,
					self.posZ + width / 2F
				))
			}
			// End

		}
	}

	// ~~~~~~~~~~ NBT ~~~~~~~~~~~~~~~~~~~~~~~~~~~

	final def serializeNBTEmulator: NBTTagCompound = this.serializeNBT()

	private final def serializeNBT(): NBTTagCompound = {
		val nbt = new NBTTagCompound

		for (ability <- this.abilities) {
			val id = AbilityLoader.getAbilityID(ability)
			if (id != null)
				nbt.setTag(id, {
					val abilityTag = new NBTTagCompound
					val mappingArgArray = ability.encodeMappingArguments()
					var mappingArgs = ""
					for (i <- mappingArgArray.indices) {
						mappingArgs += mappingArgArray(i)
						if (i < mappingArgArray.length - 1)
							mappingArgs += ","
					}
					abilityTag.setString("mappingArguments", mappingArgs)
					if (ability.hasNBT) abilityTag.setTag("nbt", ability.serializeNBT())
					abilityTag
				})
		}

		nbt
	}

	final def deserializeNBTEmulator(nbt: NBTTagCompound): Unit = this.deserializeNBT(nbt)

	private def deserializeNBT(nbt: NBTTagCompound): Unit = {

		val abilityKeys = JavaConversions.asScalaSet(nbt.getKeySet)
		var abilities = ListBuffer[IAbility[_ <: NBTBase]]()
		for (abilityName <- abilityKeys) {
			val abilityTag = nbt.getCompoundTag(abilityName)
			val mappingArgs = abilityTag.getString("mappingArguments")
			Emulation.createAbility(abilityName, mappingArgs,
				abilityName + "|" + mappingArgs) match {
				case ability: IAbility[_] =>
					if (ability.hasNBT)
						ability.asInstanceOf[IAbility[_ <: NBTBase]]
								.deserialize(abilityTag.getTag("nbt"))
					abilities += ability.asInstanceOf[IAbility[_ <: NBTBase]]
				case _ =>
			}
		}
		this.abilities = abilities.toArray.asInstanceOf[Array[IAbility[_ <: NBTBase]]]

	}

	// ~~~~~~~~~~ Other ~~~~~~~~~~

	def attackEntity(target: EntityLivingBase): Unit = {
		val world = this.getSelfEntityInstance.getEntityWorld
		this.getEntityStateInstance(world) match {
			case ranged: IRangedAttackMob => ranged.attackEntityWithRangedAttack(target, 1F)
			case _ =>
				target.attackEntityFrom(DamageSource.causeMobDamage(this.getSelfEntityInstance), 1F)
		}
	}

	// ~~~~~~~~~~ Ability ~~~~~~~~~~

	def canTakeDamage(source: DamageSource): Boolean = {
		// assumes true
		this.abilities.foreach(ability => {
			if (!ability.canTakeDamage(source, this.getSelfEntityInstance)) return false
		})
		true
	}

}
