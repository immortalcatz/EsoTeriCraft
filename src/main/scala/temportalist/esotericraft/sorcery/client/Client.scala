package temportalist.esotericraft.sorcery.client

import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import org.lwjgl.input.Keyboard
import temportalist.esotericraft.api.init.IPluginClient
import temportalist.esotericraft.sorcery.common.Sorcery
import temportalist.esotericraft.sorcery.common.network.PacketCast
import temportalist.origin.api.client.{EnumKeyCategory, Rendering}
import temportalist.origin.foundation.client.IKeyBinder

/**
  *
  * Created by TheTemportalist on 5/6/2016.
  *
  * @author TheTemportalist
  */
@SideOnly(Side.CLIENT)
object Client extends IPluginClient {

	override def preInit(): Unit = {

		ModKeys.register()

	}

	// TODO this will no longer work, need to implement key pressed manually because no mod IModClient
	object ModKeys extends IKeyBinder {

		var cast: KeyBinding = _

		override def register(): Unit = {

			this.cast = new KeyBinding("desc", Keyboard.KEY_C, EnumKeyCategory.GAMEPLAY.getName)
			this.registerKeyBinding(this.cast)

		}

		override def onKeyPressed(keyBinding: KeyBinding): Unit = {
			if (keyBinding.getKeyDescription == this.cast.getKeyDescription) {
				val packet = new PacketCast()
				packet.sendToServer(Sorcery)
				val player = Rendering.mc.thePlayer
				packet.sendToAllAround(Sorcery, new TargetPoint(
					player.dimension, player.posX, player.posY, player.posZ, 128
				))
			}
		}

	}

}
