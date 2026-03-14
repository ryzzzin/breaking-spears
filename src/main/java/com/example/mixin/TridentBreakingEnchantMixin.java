package com.example.mixin;

import com.example.BreakingSpearsMod;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ProjectileEntity.class)
public class TridentBreakingEnchantMixin {
	private static final Identifier BREAKING_ID = Identifier.of(BreakingSpearsMod.MOD_ID, "breaking");
	private static final TagKey<net.minecraft.block.Block> PICKAXE_MINEABLE = TagKey.of(RegistryKeys.BLOCK, Identifier.of("minecraft", "mineable/pickaxe"));
	private static final TagKey<net.minecraft.block.Block> NEEDS_IRON_TOOL = TagKey.of(RegistryKeys.BLOCK, Identifier.of("minecraft", "needs_iron_tool"));
	private static final TagKey<net.minecraft.block.Block> NEEDS_DIAMOND_TOOL = TagKey.of(RegistryKeys.BLOCK, Identifier.of("minecraft", "needs_diamond_tool"));

	@Unique
	private boolean breakingSpears$alreadyBrokeBlock;
	@Unique
	private boolean breakingSpears$hitEntity;

	@Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
	private void breakingSpears$onBlockHit(HitResult hit, CallbackInfo ci) {
		if (hit.getType() == HitResult.Type.ENTITY) {
			this.breakingSpears$hitEntity = true;
			return;
		}
		if (!(hit instanceof BlockHitResult hitResult)) {
			return;
		}
		ProjectileEntity projectile = (ProjectileEntity) (Object) this;
		if (!(projectile instanceof TridentEntity trident)) {
			return;
		}
		if (this.breakingSpears$alreadyBrokeBlock || this.breakingSpears$hitEntity) {
			return;
		}
		World world = projectile.getEntityWorld();
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}

		ItemStack tridentStack = trident.getWeaponStack();
		int level = getBreakingLevel(serverWorld, tridentStack);
		if (level <= 0) {
			return;
		}

		BlockState state = serverWorld.getBlockState(hitResult.getBlockPos());
		if (state.getHardness(serverWorld, hitResult.getBlockPos()) < 0.0f) {
			return;
		}
		if (!canBreakWithLevel(state, level)) {
			return;
		}
		if (!canModifyAt(serverWorld, projectile.getOwner(), hitResult.getBlockPos())) {
			return;
		}

		boolean drop = serverWorld.getRandom().nextFloat() < getDropChance(level);
		if (serverWorld.breakBlock(hitResult.getBlockPos(), drop, trident)) {
			this.breakingSpears$alreadyBrokeBlock = true;
			tridentStack.damage(1, serverWorld, null, item -> {});
			// One throw can break at most one block; then the trident should fall.
			projectile.setNoGravity(false);
			projectile.setVelocity(0.0, -0.2, 0.0);
			ci.cancel();
		}
	}

	private static int getBreakingLevel(ServerWorld world, ItemStack stack) {
		Optional<? extends RegistryEntry.Reference<Enchantment>> entry = world.getRegistryManager()
			.getOrThrow(RegistryKeys.ENCHANTMENT)
			.getEntry(BREAKING_ID);
		if (entry.isEmpty()) {
			return 0;
		}
		return EnchantmentHelper.getLevel(entry.get(), stack);
	}

	private static boolean canModifyAt(ServerWorld world, Entity owner, net.minecraft.util.math.BlockPos pos) {
		if (!(owner instanceof PlayerEntity player)) {
			return true;
		}
		return player.getAbilities().allowModifyWorld;
	}

	private static boolean canBreakWithLevel(BlockState state, int level) {
		if (!state.isToolRequired()) {
			return true;
		}
		if (!state.isIn(PICKAXE_MINEABLE)) {
			return false;
		}
		if (level == 1) {
			return !state.isIn(NEEDS_IRON_TOOL) && !state.isIn(NEEDS_DIAMOND_TOOL);
		}
		if (level == 2) {
			return !state.isIn(NEEDS_DIAMOND_TOOL);
		}
		return true;
	}

	private static float getDropChance(int level) {
		if (level == 1) {
			return 0.10f;
		}
		if (level == 2) {
			return 0.15f;
		}
		return 0.25f;
	}
}
