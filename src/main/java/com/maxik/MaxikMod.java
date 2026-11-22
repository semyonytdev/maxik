package com.maxik;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.Vec3;

public class MaxikMod implements ModInitializer {
	public static final String MOD_ID = "maxik";

	// ---------- Материалы ----------
	private static final ToolMaterial MAX_TOOL = new ToolMaterial(
			BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 1_000_000, 100.0F, 0.0F, 25, ItemTags.NETHERITE_TOOL_MATERIALS);

	private static final int ARMOR_BASE_DURABILITY = 20_000;

	private static final ResourceKey<EquipmentAsset> ARMOR_KEY =
			ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MOD_ID, "max"));

	// Значения защиты, которые просил Семён: шлем 60, нагрудник 90, поножи 76, ботинки 28.
	private static final ArmorMaterial MAX_ARMOR = new ArmorMaterial(
			ARMOR_BASE_DURABILITY,
			Map.of(ArmorType.HELMET, 60, ArmorType.CHESTPLATE, 90, ArmorType.LEGGINGS, 76, ArmorType.BOOTS, 28),
			25,
			SoundEvents.ARMOR_EQUIP_NETHERITE,
			20.0F,
			1.0F,
			ItemTags.REPAIRS_NETHERITE_ARMOR,
			ARMOR_KEY);

	// ---------- Предметы ----------
	public static final Item MAX_HELMET = register("max_helmet", Item::new,
			new Item.Properties().humanoidArmor(MAX_ARMOR, ArmorType.HELMET)
					.durability(ArmorType.HELMET.getDurability(ARMOR_BASE_DURABILITY)).rarity(Rarity.EPIC));
	public static final Item MAX_CHESTPLATE = register("max_chestplate", Item::new,
			new Item.Properties().humanoidArmor(MAX_ARMOR, ArmorType.CHESTPLATE)
					.durability(ArmorType.CHESTPLATE.getDurability(ARMOR_BASE_DURABILITY)).rarity(Rarity.EPIC));
	public static final Item MAX_LEGGINGS = register("max_leggings", Item::new,
			new Item.Properties().humanoidArmor(MAX_ARMOR, ArmorType.LEGGINGS)
					.durability(ArmorType.LEGGINGS.getDurability(ARMOR_BASE_DURABILITY)).rarity(Rarity.EPIC));
	public static final Item MAX_BOOTS = register("max_boots", Item::new,
			new Item.Properties().humanoidArmor(MAX_ARMOR, ArmorType.BOOTS)
					.durability(ArmorType.BOOTS.getDurability(ARMOR_BASE_DURABILITY)).rarity(Rarity.EPIC));

	// Меч: 1 (рука) + 37 = 38 урона = 19 сердец.
	public static final Item MAX_SWORD = register("max_sword", Item::new,
			new Item.Properties().sword(MAX_TOOL, 37.0F, -2.0F).rarity(Rarity.EPIC));
	public static final Item MAX_PICKAXE = register("max_pickaxe", Item::new,
			new Item.Properties().pickaxe(MAX_TOOL, 9.0F, -2.4F).rarity(Rarity.EPIC));
	public static final Item MAX_AXE = register("max_axe",
			props -> new AxeItem(MAX_TOOL, 29.0F, -2.4F, props.rarity(Rarity.EPIC)), new Item.Properties());
	public static final Item MAX_SHOVEL = register("max_shovel",
			props -> new ShovelItem(MAX_TOOL, 9.0F, -2.4F, props.rarity(Rarity.EPIC)), new Item.Properties());
	public static final Item MAX_HOE = register("max_hoe",
			props -> new HoeItem(MAX_TOOL, 0.0F, 0.0F, props.rarity(Rarity.EPIC)), new Item.Properties());

	public static final Item MAX_TOTEM = register("max_totem", Item::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
					.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
	public static final Item MAX_END_CRYSTAL = register("max_end_crystal", MaxCrystalItem::new,
			new Item.Properties().rarity(Rarity.EPIC).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

	private static final Set<Item> MAX_TOOLS = Set.of(MAX_SWORD, MAX_PICKAXE, MAX_AXE, MAX_SHOVEL, MAX_HOE);

	private static final Map<Item, Integer> ARMOR_POINTS = Map.of(
			MAX_HELMET, 60, MAX_CHESTPLATE, 90, MAX_LEGGINGS, 76, MAX_BOOTS, 28);

	// ---------- Тотем ----------
	private static final Identifier HEALTH_ID = Identifier.fromNamespaceAndPath(MOD_ID, "max_totem_health");
	private static final Map<UUID, Long> PROTECTED_UNTIL = new HashMap<>();
	private static final long PROTECTION_TICKS = 10L * 60L * 20L; // 10 минут
	private static boolean reentry = false;

	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties settings) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, name));
		T item = factory.apply(settings.setId(key));
		Registry.register(BuiltInRegistries.ITEM, key, item);
		return item;
	}

	@Override
	public void onInitialize() {
		// Креатив-вкладки
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
			entries.accept(MAX_SWORD);
			entries.accept(MAX_HELMET);
			entries.accept(MAX_CHESTPLATE);
			entries.accept(MAX_LEGGINGS);
			entries.accept(MAX_BOOTS);
			entries.accept(MAX_TOTEM);
			entries.accept(MAX_END_CRYSTAL);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			entries.accept(MAX_PICKAXE);
			entries.accept(MAX_AXE);
			entries.accept(MAX_SHOVEL);
			entries.accept(MAX_HOE);
		});

		// Все макс-инструменты ломают любой блок мгновенно, даже бедрок.
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
			if (level instanceof ServerLevel serverLevel && !player.isSpectator() && !player.isCreative()
					&& MAX_TOOLS.contains(player.getMainHandItem().getItem())) {
				BlockState state = level.getBlockState(pos);
				if (!state.isAir()) {
					Block.dropResources(state, serverLevel, pos, serverLevel.getBlockEntity(pos), player,
							player.getMainHandItem());
					serverLevel.destroyBlock(pos, false, player);
					return InteractionResult.SUCCESS;
				}
			}
			return InteractionResult.PASS;
		});

		// Урон: защита тотема, макс-кристалл, макс-броня
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (reentry || !(entity.level() instanceof ServerLevel level)) {
				return true;
			}
			if (entity instanceof ServerPlayer player && isProtected(player, level)) {
				return false;
			}
			if (source.getDirectEntity() instanceof EndCrystal crystal && crystal.getTags().contains(MaxCrystalItem.TAG)) {
				float health = entity.getHealth();
				reentry = true;
				try {
					if (health <= 60.0F) {
						entity.kill(level);
					} else {
						entity.setHealth(health - 60.0F);
					}
				} finally {
					reentry = false;
				}
				return false;
			}
			int points = armorPoints(entity);
			if (points > 0 && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
				float reduced = (float) (amount * 100.0 / (100.0 + points));
				reentry = true;
				try {
					entity.hurtServer(level, source, reduced);
				} finally {
					reentry = false;
				}
				return false;
			}
			return true;
		});

		// Смерть: макс-тотем воскрешает
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer player && entity.level() instanceof ServerLevel level && holdsTotem(player)) {
				triggerTotem(player, level);
				return false;
			}
			return true;
		});

		// Каждый тик: +20 сердец с тотемом, спасение от бездны
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				updateTotemHealth(player);
				if (player.level() instanceof ServerLevel level && (holdsTotem(player) || isProtected(player, level))
						&& player.getY() < level.dimensionType().minY() - 2) {
					rescueFromVoid(player, level);
				}
			}
		});
	}

	private static boolean holdsTotem(ServerPlayer player) {
		if (player.getOffhandItem().is(MAX_TOTEM)) {
			return true;
		}
		for (int i = 0; i < 9; i++) {
			if (player.getInventory().getItem(i).is(MAX_TOTEM)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isProtected(ServerPlayer player, ServerLevel level) {
		Long until = PROTECTED_UNTIL.get(player.getUUID());
		return until != null && until > level.getServer().overworld().getGameTime();
	}

	private static void updateTotemHealth(ServerPlayer player) {
		AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
		if (health == null) {
			return;
		}
		boolean has = health.getModifier(HEALTH_ID) != null;
		if (holdsTotem(player)) {
			if (!has) {
				health.addTransientModifier(new AttributeModifier(HEALTH_ID, 40.0, AttributeModifier.Operation.ADD_VALUE));
				player.setHealth(player.getHealth() + 40.0F);
			}
		} else if (has) {
			health.removeModifier(HEALTH_ID);
			if (player.getHealth() > player.getMaxHealth()) {
				player.setHealth(player.getMaxHealth());
			}
		}
	}

	private static void triggerTotem(ServerPlayer player, ServerLevel level) {
		player.setHealth(player.getMaxHealth());
		player.clearFire();
		PROTECTED_UNTIL.put(player.getUUID(), level.getServer().overworld().getGameTime() + PROTECTION_TICKS);
		if (player.getY() < level.dimensionType().minY() - 2) {
			rescueFromVoid(player, level);
		}
		// Чёрные частицы вместо зелёных
		level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0, player.getZ(), 80, 0.5, 1.0, 0.5, 0.15);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.5, 1.0, 0.5, 0.05);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 0.6F);
		player.displayClientMessage(Component.literal("Макс-тотем: полная защита на 10 минут!"), true);
	}

	private static void rescueFromVoid(ServerPlayer player, ServerLevel level) {
		int x = player.getBlockX();
		int z = player.getBlockZ();
		int minY = level.dimensionType().minY();
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
		int y;
		if (top <= minY + 1) {
			// Под ногами пустота: строим маленькую обсидиановую площадку
			y = Math.max(minY + 32, 64);
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					level.setBlockAndUpdate(new BlockPos(x + dx, y - 1, z + dz), Blocks.OBSIDIAN.defaultBlockState());
					level.setBlockAndUpdate(new BlockPos(x + dx, y, z + dz), Blocks.AIR.defaultBlockState());
					level.setBlockAndUpdate(new BlockPos(x + dx, y + 1, z + dz), Blocks.AIR.defaultBlockState());
				}
			}
		} else {
			y = top;
		}
		player.teleportTo(level, x + 0.5, y, z + 0.5, Set.<net.minecraft.world.entity.Relative>of(),
				player.getYRot(), player.getXRot(), true);
		player.fallDistance = 0;
		player.setDeltaMovement(Vec3.ZERO);
	}

	private static int armorPoints(LivingEntity entity) {
		int total = 0;
		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			ItemStack stack = entity.getItemBySlot(slot);
			Integer points = ARMOR_POINTS.get(stack.getItem());
			if (points != null) {
				total += points;
			}
		}
		return total;
	}
}
