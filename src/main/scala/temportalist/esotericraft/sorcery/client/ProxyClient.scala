package temportalist.esotericraft.sorcery.client

import java.util

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.fml.client.IModGuiFactory
import net.minecraftforge.fml.client.IModGuiFactory.{RuntimeOptionCategoryElement, RuntimeOptionGuiHandler}
import temportalist.esotericraft.sorcery.common.ProxyCommon
import temportalist.origin.foundation.client.IModelLoader

/**
  * Created by TheTemportalist on 12/31/2015.
  */
class ProxyClient extends ProxyCommon with IModGuiFactory with IModelLoader {

	override def preInit(): Unit = {
		Client.preInit()

	}

	override def register(): Unit = {}

	override def postInit(): Unit = {}

	override def runtimeGuiCategories(): util.Set[RuntimeOptionCategoryElement] = null

	override def initialize(minecraftInstance: Minecraft): Unit = {}

	override def getHandlerFor(element: RuntimeOptionCategoryElement): RuntimeOptionGuiHandler = null

	override def mainConfigGuiClass(): Class[_ <: GuiScreen] = classOf[GuiConfig]

}
