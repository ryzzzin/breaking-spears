package com.example.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(PlayerEntity.class)
public class ElytraGlassPierceMixin {
	private static final double MIN_SPEED = 1.2;
	private static final double RAYCAST_DISTANCE = 2.2;
	private static final float DAMAGE = 4.0f;
	private static final double SPEED_MULTIPLIER_AFTER_BREAK = 0.8;

	@Inject(method = "tick", at = @At("TAIL"))
	private void breakingSpears$elytraGlassPierce(CallbackInfo ci) {
		PlayerEntity player = (PlayerEntity) (Object) this;
		if (!(player.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}
		if (!player.isGliding()) {
			return;
		}
		if (!isUsingSpearOrTrident(player)) {
			return;
		}
		Vec3d velocity = player.getVelocity();
		if (velocity.lengthSquared() < MIN_SPEED * MIN_SPEED) {
			return;
		}
		if (velocity.lengthSquared() < 1.0E-6) {
			return;
		}

		Vec3d start = player.getEyePos();
		Vec3d end = start.add(velocity.normalize().multiply(RAYCAST_DISTANCE));
		HitResult hit = world.raycast(new RaycastContext(
			start,
			end,
			RaycastContext.ShapeType.COLLIDER,
			RaycastContext.FluidHandling.NONE,
			player
		));

		if (hit.getType() != HitResult.Type.BLOCK) {
			return;
		}

		if (!player.getAbilities().allowModifyWorld) {
			return;
		}

		BlockPos hitPos = ((BlockHitResult) hit).getBlockPos();
		BlockPos[] hole = getTwoByThreeHole(hitPos, velocity);
		boolean brokeAny = false;
		for (BlockPos targetPos : hole) {
			BlockState state = world.getBlockState(targetPos);
			if (!state.isIn(ConventionalBlockTags.GLASS_PANES)) {
				continue;
			}
			if (world.breakBlock(targetPos, true, player)) {
				brokeAny = true;
			}
		}

		if (brokeAny) {
			player.damage(world, world.getDamageSources().generic(), DAMAGE);
			player.setVelocity(player.getVelocity().multiply(SPEED_MULTIPLIER_AFTER_BREAK));
		}
	}

	private static BlockPos[] getTwoByThreeHole(BlockPos center, Vec3d velocity) {
		double ax = Math.abs(velocity.x);
		double ay = Math.abs(velocity.y);
		double az = Math.abs(velocity.z);

		if (ax >= ay && ax >= az) {
			Direction zDir = velocity.z >= 0.0 ? Direction.SOUTH : Direction.NORTH;
			return new BlockPos[] {
				center,
				center.up(),
				center.down(),
				center.offset(zDir),
				center.up().offset(zDir),
				center.down().offset(zDir)
			};
		}
		if (az >= ay) {
			Direction xDir = velocity.x >= 0.0 ? Direction.EAST : Direction.WEST;
			return new BlockPos[] {
				center,
				center.up(),
				center.down(),
				center.offset(xDir),
				center.up().offset(xDir),
				center.down().offset(xDir)
			};
		}
		Direction xDir = velocity.x >= 0.0 ? Direction.EAST : Direction.WEST;
		Direction zDir = velocity.z >= 0.0 ? Direction.SOUTH : Direction.NORTH;
		return new BlockPos[] {
			center,
			center.offset(xDir),
			center.offset(zDir),
			center.offset(xDir).offset(zDir),
			center.down().offset(zDir),
			center.down().offset(xDir).offset(zDir)
		};
	}

	private static boolean isUsingSpearOrTrident(PlayerEntity player) {
		if (!player.isUsingItem()) {
			return false;
		}
		return isSpearOrTrident(player.getActiveItem());
	}

	private static boolean isSpearOrTrident(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		if (stack.getItem() instanceof TridentItem) {
			return true;
		}
		String path = Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
		return path.contains("spear");
	}
}
