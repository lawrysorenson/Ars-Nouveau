package com.hollingsworth.arsnouveau.common.items;

import com.hollingsworth.arsnouveau.client.renderer.item.FixedGeoItemRenderer;
import com.hollingsworth.arsnouveau.client.renderer.item.ShieldModel;
import com.hollingsworth.arsnouveau.common.capability.CapabilityRegistry;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.function.Consumer;

import static com.hollingsworth.arsnouveau.setup.ItemsRegistry.defaultItemProperties;

public class EnchantersShield extends ShieldItem implements IAnimatable {

    public EnchantersShield() {
        super(defaultItemProperties().durability(500));
    }

    public EnchantersShield(Properties p_i48470_1_) {
        super(p_i48470_1_);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int p_77663_4_, boolean p_77663_5_) {
        if (world.isClientSide() || world.getGameTime() % 200 != 0 || stack.getDamageValue() == 0 || !(entity instanceof Player))
            return;

        CapabilityRegistry.getMana((LivingEntity) entity).ifPresent(mana -> {
            if (mana.getCurrentMana() > 20) {
                mana.removeMana(20);
                stack.setDamageValue(stack.getDamageValue() - 1);
            }
        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level p_77659_1_, Player p_77659_2_, InteractionHand p_77659_3_) {
        return super.use(p_77659_1_, p_77659_2_, p_77659_3_);
    }

    @Override
    public void registerControllers(AnimationData animationData) {
    }

    public AnimationFactory factory = new AnimationFactory(this);

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new FixedGeoItemRenderer<EnchantersShield>(new ShieldModel());

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }
}
