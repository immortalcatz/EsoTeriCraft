package com.temportalist.esotericenhancing.common.enhancement.gear.passive

import com.temportalist.esotericenhancing.api.Enhancement
import net.minecraft.entity.player.EntityPlayer

// TODO unfinished (needs testing)
/**
  * Inspired by: iChun's Morph mod's AbilityFloat
  * https://github.com/iChun/Morph/blob/master/src/main/java/morph/common/ability/AbilityFloat.java#L43
  *
  * AbilityFloat for a chicken:
  *     terminal velocity = -0.114
  *     negate fall distance = true
  *
  * Created by TheTemportalist on 12/31/2015.
  */
object EnFloat extends Enhancement("float") {

	override def onPlayerTick(player: EntityPlayer, power: Float): Unit = {
		val isFlying = player.capabilities.isFlying
		val terminalVelocity = -this.getTerminalVelocity(power)
		// speed > terminal velocity
		if (!isFlying && player.motionY < terminalVelocity) {
			player.motionY = terminalVelocity
			if (this.shouldNegateFallDamage(power)) player.fallDistance = 0f
		}
	}

	def getTerminalVelocity(power: Float): Float = {
		0.114f
	}

	def shouldNegateFallDamage(power: Float): Boolean = true

}