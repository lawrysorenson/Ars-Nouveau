package com.hollingsworth.arsnouveau.common.spell.effect;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.util.DamageUtil;
import com.hollingsworth.arsnouveau.client.particle.ParticleUtil;
import com.hollingsworth.arsnouveau.common.entity.Cinder;
import com.hollingsworth.arsnouveau.common.items.curios.ShapersFocus;
import com.hollingsworth.arsnouveau.common.lib.GlyphLib;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.hollingsworth.arsnouveau.setup.registry.DamageTypesRegistry;
import com.hollingsworth.arsnouveau.setup.registry.ModPotions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class EffectFlare extends AbstractEffect implements IDamageEffect {
    public static EffectFlare INSTANCE = new EffectFlare();

    private EffectFlare() {
        super(GlyphLib.EffectFlareID, "Flare");
    }

    @Override
    public void onResolveEntity(EntityHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (!(rayTraceResult.getEntity() instanceof LivingEntity livingEntity && world instanceof ServerLevel level))
            return;
        Vec3 vec = safelyGetHitPos(rayTraceResult);
        float damage = (float) (DAMAGE.get() + AMP_VALUE.get() * spellStats.getAmpMultiplier());
        int snareSec = 0;//(int) (POTION_TIME.get() + EXTEND_TIME.get() * spellStats.getDurationMultiplier());

//        if (!canDamage(livingEntity))
//            return;
//        this.damage(vec, level, shooter, livingEntity, spellStats, spellContext, resolver, snareSec, damage);
        spawnCinders(shooter, level,rayTraceResult.getLocation().add(0, (rayTraceResult.getEntity().onGround() ? 1 : 0),0), spellStats, spellContext, resolver);
    }

    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        super.onResolveBlock(rayTraceResult, world, shooter, spellStats, spellContext, resolver);
        if(!world.getBlockState(rayTraceResult.getBlockPos()).is(BlockTags.ICE))
            return;
        world.setBlock(rayTraceResult.getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
        spawnCinders(shooter, world, rayTraceResult.getLocation(), spellStats, spellContext, resolver);
    }

    public void spawnCinders(LivingEntity shooter, Level level, Vec3 hit, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver){
        double radiusMultiplier = 1;
        double rotateSpeed = 1d;
        int max = 11;
        for(int i = 0; i < max; i++) {
            float offset = i * 15;
            Vec3 vec3 = new Vec3(
                    hit.x() - radiusMultiplier * Math.sin(i / rotateSpeed + offset),
                    hit.y(), // Offset if the owner died
                    hit.z() - radiusMultiplier * Math.cos(i / rotateSpeed + offset));
            Vec3 scaleVec =  new Vec3(ParticleUtil.inRange(0.1, 0.5), 1, ParticleUtil.inRange(0.1, 0.5));

            Cinder fallingBlock = new Cinder(level, vec3.x(), vec3.y(), vec3.z(), BlockRegistry.MAGIC_FIRE.defaultBlockState());
            // Send the falling block the opposite direction of the target
            fallingBlock.setDeltaMovement(vec3.x() - hit.x(), ParticleUtil.inRange(0.1, 0.5), vec3.z() - hit.z());
            fallingBlock.setDeltaMovement(fallingBlock.getDeltaMovement().multiply(scaleVec));
            fallingBlock.cancelDrop = true;
            fallingBlock.hurtEntities = true;
            fallingBlock.baseDamage = ((float) (DAMAGE.get() + AMP_VALUE.get() * spellStats.getAmpMultiplier())) * 0.5f;
            fallingBlock.shooter = shooter;
            level.addFreshEntity(fallingBlock);
            ShapersFocus.tryPropagateEntitySpell(fallingBlock, level, shooter, spellContext, resolver);
        }
    }



    public void damage(Vec3 vec, ServerLevel world, LivingEntity shooter, LivingEntity livingEntity, SpellStats stats, SpellContext context, SpellResolver resolver, int snareTime, float damage) {
        if (attemptDamage(world, shooter, stats, context, resolver, livingEntity, buildDamageSource(world, shooter), damage)) {
            world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, vec.x, vec.y + 0.5, vec.z, 50,
                    ParticleUtil.inRange(-0.1, 0.1), ParticleUtil.inRange(-0.1, 0.1), ParticleUtil.inRange(-0.1, 0.1), 0.3);

            livingEntity.addEffect(new MobEffectInstance(ModPotions.SNARE_EFFECT.get(), 20 * snareTime));
        }
    }


    @Override
    public DamageSource buildDamageSource(Level world, LivingEntity shooter) {
        return DamageUtil.source(world, DamageTypesRegistry.FLARE, shooter);
    }

    @Override
    protected void addDefaultAugmentLimits(Map<ResourceLocation, Integer> defaults) {
        defaults.put(AugmentAmplify.INSTANCE.getRegistryName(), 2);
    }

    @Override
    public void buildConfig(ForgeConfigSpec.Builder builder) {
        super.buildConfig(builder);
        addDamageConfig(builder, 7.0);
        addAmpConfig(builder, 3.0);
        addExtendTimeConfig(builder, 1);
    }

    @Override
    public int getDefaultManaCost() {
        return 40;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(
                AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE,
                AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE,
                AugmentAOE.INSTANCE,
                AugmentFortune.INSTANCE, AugmentRandomize.INSTANCE
        );
    }

    @Override
    public String getBookDescription() {
        return "When used on entities that are on fire, Flare causes a burst of damage and will spread fire and deal damage to other nearby entities. Does significantly more damage than Harm. Can be augmented with Extend Time, Amplify, and AOE.";
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @NotNull
    @Override
    public Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.ELEMENTAL_FIRE);
    }
}
