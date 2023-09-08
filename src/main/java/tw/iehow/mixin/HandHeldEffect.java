package tw.iehow.mixin;

import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tw.iehow.util.apply.PotionEffect;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static tw.iehow.util.apply.PlayerParticle.showParticle;
import static tw.iehow.util.apply.PlayerTitle.showTitle;
import static tw.iehow.util.check.SlotCheck.isValid;

@Mixin(PlayerEntity.class)
public abstract class HandHeldEffect {

    @Unique
    private boolean absorptionEffect = false;

    //Log for CD
    @Unique
    private final Map<UUID, Long> cooldown = new HashMap<>();

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo info) {
        //Get player info
        PlayerEntity player = ((PlayerEntity)(Object)this);
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        ItemStack offHand = ((PlayerEntity)(Object)this).getStackInHand(Hand.OFF_HAND);

        //Timestamp for CD
        UUID playerUuid = player.getUuid();
        long lastUsedTime = cooldown.getOrDefault(playerUuid, 0L);
        long currentTime = player.getWorld().getTime();
        long interval = currentTime - lastUsedTime;

        //Detect the usage of absorption
        float absorptionAmount = player.getAbsorptionAmount();

        //HowItem:blue_omamori
        if (isValid(offHand,"minecraft:flower_banner_pattern",1337001)) {
            PotionEffect.add(player, StatusEffects.CONDUIT_POWER, 10, 1);
        }

        //HowItem:purple_omamori
        if (isValid(offHand,"minecraft:flower_banner_pattern",1337002)) {
            player.removeStatusEffect(StatusEffects.HUNGER);
            player.removeStatusEffect(StatusEffects.DARKNESS);
            player.removeStatusEffect(StatusEffects.POISON);
            player.removeStatusEffect(StatusEffects.WITHER);
        }

        //HowItem:red_omamori
        if (isValid(offHand,"minecraft:totem_of_undying",1337001)) {
            if (interval >= 300 && !absorptionEffect) {
                PotionEffect.add(player, StatusEffects.ABSORPTION, -1, 3);
                absorptionAmount = 1.0F;
                absorptionEffect = true;
            }else {
                if (300 - interval >= 0){
                    showTitle(serverPlayer, 300 - interval);
                }
            }
        }
        if (absorptionEffect && (!isValid(offHand,"minecraft:totem_of_undying",1337001) || absorptionAmount == 0.0F)){
            PotionEffect.remove(player, StatusEffects.ABSORPTION);
            absorptionEffect = false;
            cooldown.put(playerUuid, currentTime);
        }

        //HowItem:blue_omamori
        if (isValid(offHand,"minecraft:skull_banner_pattern",1337001)) {
            PotionEffect.add(player, StatusEffects.SLOWNESS, 25, 1);
            PotionEffect.add(player, StatusEffects.WEAKNESS, 25, 1);
            PotionEffect.add(player, StatusEffects.BLINDNESS, 25, 1);
        }

        //HowItem:chinese_valentines_2023/red_rose
        if (isValid(offHand,"minecraft:flower_banner_pattern",1337028)) {
            if (interval >= 200) {
                PotionEffect.add(player, StatusEffects.REGENERATION, 80, 2);
                showParticle(serverPlayer, ParticleTypes.HEART, player.getX(), player.getY() + 1.0, player.getZ(), 0.5F, 0.5F, 0.5F, 1, 5);
                cooldown.put(playerUuid, currentTime);
            }else {
                showTitle(serverPlayer, 200 - interval);
            }
        }

        //HowItem:how_water_bucket
        if (isValid(offHand, "minecraft:water_bucket", 1337001)){
            if (player.getSteppingBlockState().getBlock() == Blocks.MAGMA_BLOCK){
                PotionEffect.add(player, StatusEffects.FIRE_RESISTANCE,5,1);
                showParticle(serverPlayer, ParticleTypes.SPLASH, player.getX(), player.getY(), player.getZ(), 1.6F, 0.8F, 1.6F, 0.4F, 240);
            }
            if (player.fallDistance >= 3.0F){
                BlockPos surfacePos = player.getWorld().getTopPosition(Heightmap.Type.WORLD_SURFACE, player.getBlockPos());
                double distanceToSurface = player.getY() - surfacePos.getY();

                if (distanceToSurface <= 3.0) {
                    PotionEffect.add(player, StatusEffects.SLOW_FALLING, 5, 1);
                    showParticle(serverPlayer, ParticleTypes.SPLASH, player.getX(), player.getY(), player.getZ(), 1.6F, 0.8F, 1.6F, 0.4F, 240);
                    serverPlayer.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(SoundEvents.ITEM_BUCKET_FILL), SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 1.0F, 2.0F - (player.fallDistance / 16.0F) * 0.2F, player.getRandom().nextLong()));
                }
            }
        }
    }
}
