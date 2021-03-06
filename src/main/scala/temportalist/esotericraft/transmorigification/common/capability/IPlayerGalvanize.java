package temportalist.esotericraft.transmorigification.common.capability;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import temportalist.esotericraft.api.emulation.IAbility;
import temportalist.esotericraft.emulation.client.EntityModel;
import temportalist.esotericraft.emulation.common.EntityState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by TheTemportalist on 5/7/2016.
 *
 * @author TheTemportalist
 */
public interface IPlayerGalvanize extends INBTSerializable<NBTTagCompound> {

	void onTickClient();

	void onTickServer();

	void setEntityState(EntityState state);

	void setEntityState(String entityName, World world);

	EntityState getEntityState();

	void clearEntityState(World world);

	@SideOnly(Side.CLIENT)
	@Nullable
	EntityModel<? extends EntityLivingBase, ? extends EntityLivingBase> getEntityModelInstance(World world);

	Iterable<IAbility<? extends NBTBase>> getEntityAbilities();

	void addModelEntity(EntityLivingBase entity);

	void addModelEntity(EntityState state);

	List<EntityState> getModelEntities();

	void removeModelEntity(int index);

	boolean canTakeDamage(DamageSource source);

}
